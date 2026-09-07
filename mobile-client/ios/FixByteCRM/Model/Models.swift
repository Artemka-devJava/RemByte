import Foundation

struct ClientBrief: Identifiable, Decodable, Hashable {
    let id: Int
    let name: String
    let phone: String
    let type: String?
    let tags: String?
    let archivedAt: String?
    var archived: Bool { archivedAt != nil }
}

struct ClientDetail: Decodable {
    let id: Int
    let name: String
    let phone: String
    let email: String?
    let address: String?
    let type: String?
    let tags: String?
    let notes: String?
    let preferredChannel: String?
    let archivedAt: String?
    var archived: Bool { archivedAt != nil }
}

struct ClientSummary: Decodable {
    let ordersCount: Int
    let revenue: Double
    let debt: Double
}

struct ClientPhoto: Identifiable, Decodable, Hashable {
    let id: Int
    let url: String
    let caption: String?
    let createdAt: String?
}

struct AuthInfo: Decodable {
    let username: String
    let roles: [String]
}

enum CreateClientResult {
    /// Клиент создан. `orderFailed` = первичную заявку завести не удалось.
    case created(id: Int, orderFailed: Bool)
    case duplicate(id: Int, name: String)
}

enum ApiError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self { case .message(let m): return m }
    }
}

extension String {
    var trimmedTrailingSlash: String {
        var s = trimmingCharacters(in: .whitespacesAndNewlines)
        while s.hasSuffix("/") { s.removeLast() }
        return s
    }
    var digitsAndPlus: String { filter { $0.isNumber || $0 == "+" } }
    var digitsOnly: String { filter { $0.isNumber } }

    /// Приводит российский номер к виду "+7XXXXXXXXXX" (8XXXXXXXXXX → тот же вид).
    /// nil — если после нормализации получилось не ровно 11 цифр, начинающихся с 7.
    var normalizedRuPhone: String? {
        var digits = digitsOnly
        if digits.count == 11, digits.hasPrefix("8") {
            digits = "7" + digits.dropFirst()
        }
        guard digits.count == 11, digits.hasPrefix("7") else { return nil }
        return "+" + digits
    }
}
