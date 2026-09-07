import Foundation

/// Единственная точка доступа к API FixByte CRM.
/// Аутентификация — по cookie сессии (HTTPCookieStorage.shared), cookie
/// сохраняется в UserDefaults, чтобы вход не слетал между запусками.
final class Api {

    static let shared = Api()

    private let defaults = UserDefaults.standard
    private let session: URLSession

    private init() {
        let cfg = URLSessionConfiguration.default
        cfg.httpCookieStorage = HTTPCookieStorage.shared
        cfg.httpCookieAcceptPolicy = .always
        cfg.httpShouldSetCookies = true
        cfg.timeoutIntervalForRequest = 30
        cfg.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = URLSession(configuration: cfg)
    }

    // MARK: — настройки

    var baseURL: String {
        get { defaults.string(forKey: "baseURL") ?? "http://localhost:9087" }
        set { defaults.set(newValue.trimmedTrailingSlash, forKey: "baseURL") }
    }

    var lastUsername: String {
        get { defaults.string(forKey: "username") ?? "" }
        set { defaults.set(newValue, forKey: "username") }
    }

    private func makeURL(_ path: String) -> URL {
        URL(string: baseURL + path)!
    }

    func absoluteURL(_ path: String) -> URL {
        path.hasPrefix("http") ? URL(string: path)! : URL(string: baseURL + path)!
    }

    // MARK: — cookie (session-cookie переживает перезапуск)

    func saveCookies() {
        guard let u = URL(string: baseURL),
              let cookies = HTTPCookieStorage.shared.cookies(for: u) else { return }
        let arr: [[String: String]] = cookies.map {
            ["name": $0.name, "value": $0.value, "domain": $0.domain, "path": $0.path]
        }
        defaults.set(arr, forKey: "cookies")
    }

    func restoreCookies() {
        guard let arr = defaults.array(forKey: "cookies") as? [[String: String]] else { return }
        for d in arr {
            var p: [HTTPCookiePropertyKey: Any] = [:]
            p[.name] = d["name"]
            p[.value] = d["value"]
            p[.domain] = d["domain"]
            p[.path] = d["path"] ?? "/"
            if let c = HTTPCookie(properties: p) {
                HTTPCookieStorage.shared.setCookie(c)
            }
        }
    }

    func clearCookies() {
        defaults.removeObject(forKey: "cookies")
        if let u = URL(string: baseURL),
           let cs = HTTPCookieStorage.shared.cookies(for: u) {
            cs.forEach { HTTPCookieStorage.shared.deleteCookie($0) }
        }
    }

    // MARK: — аутентификация

    @discardableResult
    func login(username: String, password: String) async throws -> AuthInfo {
        var req = URLRequest(url: makeURL("/api/auth/login"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: ["username": username, "password": password])

        let (data, resp) = try await session.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        guard code == 200 else {
            throw ApiError.message(errorText(data) ?? "Не удалось войти (\(code))")
        }
        saveCookies()
        return (try? JSONDecoder().decode(AuthInfo.self, from: data)) ?? AuthInfo(username: username, roles: [])
    }

    func me() async -> AuthInfo? {
        guard let (data, resp) = try? await session.data(from: makeURL("/api/auth/me")),
              (resp as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        return try? JSONDecoder().decode(AuthInfo.self, from: data)
    }

    func logout() async {
        var req = URLRequest(url: makeURL("/api/auth/logout"))
        req.httpMethod = "POST"
        _ = try? await session.data(for: req)
        clearCookies()
    }

    // MARK: — клиенты

    func clients() async throws -> [ClientBrief] { try await getJSON("/api/clients") }

    func searchClients(_ query: String) async throws -> [ClientBrief] {
        let q = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        return try await getJSON("/api/clients/search?name=\(q)")
    }

    func client(_ id: Int) async throws -> ClientDetail { try await getJSON("/api/clients/\(id)") }

    func summary(_ id: Int) async throws -> ClientSummary { try await getJSON("/api/clients/\(id)/summary") }

    func photos(_ id: Int) async throws -> [ClientPhoto] { try await getJSON("/api/clients/\(id)/photos") }

    /// Создать клиента. При заданных `problem` / `estimate` следом заводится
    /// первичная заявка (заказ NEW): описание = проблема, строка
    /// «Предварительная оценка» = озвученная примерная цена.
    func createClient(
        name: String,
        phone: String,
        type: String,
        problem: String? = nil,
        estimate: Double? = nil
    ) async throws -> CreateClientResult {
        var req = URLRequest(url: makeURL("/api/clients"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: [
            "name": name, "phone": phone, "type": type, "isActive": true
        ])

        let (data, resp) = try await session.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]

        if (200..<300).contains(code) {
            let id = intValue(obj?["id"])
            let hasProblem = !(problem ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            let hasPrice = (estimate ?? 0) > 0
            var orderFailed = false
            if hasProblem || hasPrice {
                do {
                    try await createInitialOrder(clientId: id, problem: problem, estimate: estimate)
                } catch {
                    orderFailed = true
                }
            }
            return .created(id: id, orderFailed: orderFailed)
        }
        if code == 409 {
            return .duplicate(id: intValue(obj?["id"]), name: (obj?["name"] as? String) ?? "")
        }
        throw ApiError.message(errorText(data) ?? "Не удалось создать клиента (\(code))")
    }

    /// Первичная заявка для только что созданного клиента.
    private func createInitialOrder(clientId: Int, problem: String?, estimate: Double?) async throws {
        var order: [String: Any] = [
            "client": ["id": clientId],
            "status": "NEW",
            "paidAmount": 0,
            "notes": "Первичная заявка (моб. приложение)"
        ]
        let desc = (problem ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if !desc.isEmpty { order["deviceDescription"] = desc }

        var lines: [[String: Any]] = []
        if (estimate ?? 0) > 0 {
            lines.append([
                "name": "Предварительная оценка (со слов клиента)",
                "unitPrice": estimate!,
                "quantity": 1
            ])
        }
        order["lines"] = lines

        var req = URLRequest(url: makeURL("/api/orders"))
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: order)

        let (_, resp) = try await session.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else {
            throw ApiError.message("Заявка не создана (\(code))")
        }
    }

    // MARK: — фото

    func uploadPhoto(clientId: Int, jpeg: Data, caption: String?) async throws {
        let boundary = "Boundary-\(UUID().uuidString)"
        var req = URLRequest(url: makeURL("/api/clients/\(clientId)/photos"))
        req.httpMethod = "POST"
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        func append(_ s: String) { body.append(s.data(using: .utf8)!) }

        append("--\(boundary)\r\n")
        append("Content-Disposition: form-data; name=\"files\"; filename=\"photo.jpg\"\r\n")
        append("Content-Type: image/jpeg\r\n\r\n")
        body.append(jpeg)
        append("\r\n")

        if let caption, !caption.isEmpty {
            append("--\(boundary)\r\n")
            append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
            append(caption)
            append("\r\n")
        }
        append("--\(boundary)--\r\n")
        req.httpBody = body

        let (data, resp) = try await session.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else {
            throw ApiError.message(errorText(data) ?? "Не удалось загрузить фото (\(code))")
        }
    }

    func deletePhoto(clientId: Int, photoId: Int) async throws {
        var req = URLRequest(url: makeURL("/api/clients/\(clientId)/photos/\(photoId)"))
        req.httpMethod = "DELETE"
        let (_, resp) = try await session.data(for: req)
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) || code == 204 else {
            throw ApiError.message("Не удалось удалить (\(code))")
        }
    }

    /// Скачать данные (фото) тем же клиентом — с cookie сессии.
    func downloadData(_ url: URL) async throws -> Data {
        let (data, resp) = try await session.data(from: url)
        guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw ApiError.message("Не удалось получить файл")
        }
        return data
    }

    // MARK: — helpers

    private func getJSON<T: Decodable>(_ path: String) async throws -> T {
        let (data, resp) = try await session.data(from: makeURL(path))
        let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
        if code == 401 || code == 403 || code == 302 {
            throw ApiError.message("Сессия истекла, войдите заново")
        }
        guard (200..<300).contains(code) else {
            throw ApiError.message("Ошибка запроса (\(code))")
        }
        return try JSONDecoder().decode(T.self, from: data)
    }

    private func errorText(_ data: Data) -> String? {
        guard !data.isEmpty else { return nil }
        if let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
           let e = o["error"] as? String { return e }
        return String(data: data, encoding: .utf8)
    }

    private func intValue(_ any: Any?) -> Int {
        if let i = any as? Int { return i }
        if let n = any as? NSNumber { return n.intValue }
        if let s = any as? String, let i = Int(s) { return i }
        return 0
    }
}
