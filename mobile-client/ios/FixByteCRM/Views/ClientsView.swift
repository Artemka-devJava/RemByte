import SwiftUI

enum Route: Hashable {
    case client(Int)
    case order(orderId: Int, clientId: Int)
}

struct ClientsView: View {
    @EnvironmentObject private var session: Session

    @State private var path: [Route] = []
    @State private var clients: [ClientBrief] = []
    @State private var query = ""
    @State private var loading = false
    @State private var error: String?
    @State private var showCreate = false
    @State private var showSettings = false

    var body: some View {
        NavigationStack(path: $path) {
            List(clients) { c in
                NavigationLink(value: Route.client(c.id)) {
                    ClientRowView(client: c)
                }
            }
            .listStyle(.plain)
            .overlay {
                if clients.isEmpty && !loading {
                    ContentUnavailableCompat(text: query.isEmpty ? "Клиентов нет" : "Ничего не найдено")
                }
            }
            .refreshable { await reload() }
            .searchable(text: $query, prompt: "Имя, телефон или email")
            .navigationTitle("Клиенты")
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .client(let id): ClientDetailView(clientId: id)
                case .order(let oid, let cid): OrderDetailView(orderId: oid, clientId: cid)
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Menu {
                        Button("Настройки сервера") {
                            showSettings = true
                        }
                        Button("Выйти", role: .destructive) {
                            Task { await session.logout() }
                        }
                    } label: {
                        Image(systemName: "person.crop.circle")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            // Кнопка добавления клиента: закреплена внизу, всегда под рукой
            .safeAreaInset(edge: .bottom) {
                Button {
                    showCreate = true
                } label: {
                    Label("Новый клиент", systemImage: "plus.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal)
                .padding(.bottom, 8)
                .background(.bar)
            }
            .sheet(isPresented: $showCreate) {
                CreateClientSheet { newId in
                    Task {
                        await reload()
                        path.append(.client(newId))
                    }
                }
            }
            .alert("Ошибка", isPresented: Binding(
                get: { error != nil },
                set: { if !$0 { error = nil } }
            ), presenting: error) { _ in
                Button("OK", role: .cancel) {}
            } message: { Text($0) }
        }
        .task { await reload() }
        .task(id: query) {
            try? await Task.sleep(nanoseconds: 350_000_000)
            if !Task.isCancelled { await reload() }
        }
    }

    private func reload() async {
        loading = true
        defer { loading = false }
        do {
            let q = query.trimmingCharacters(in: .whitespaces)
            if q.count >= 2 {
                clients = try await Api.shared.searchClients(q)
            } else {
                clients = try await Api.shared.clients().sorted {
                    $0.name.lowercased() < $1.name.lowercased()
                }
            }
        } catch {
            if error.localizedDescription.contains("Сессия истекла") {
                await session.logout()
            } else {
                self.error = error.localizedDescription
            }
        }
    }
}

struct ClientRowView: View {
    let client: ClientBrief

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(client.name).font(.headline)
                if client.archived {
                    Text("архив").font(.caption2)
                        .padding(.horizontal, 6).padding(.vertical, 1)
                        .background(Color.secondary.opacity(0.2)).clipShape(Capsule())
                }
            }
            Text(client.phone).font(.subheadline).foregroundStyle(.secondary)
            if let tags = client.tags, !tags.isEmpty {
                Text(tags).font(.caption).foregroundStyle(Color.accentColor)
            }
        }
        .padding(.vertical, 2)
    }
}

/// Замена ContentUnavailableView для iOS 16.
struct ContentUnavailableCompat: View {
    let text: String
    var body: some View {
        Text(text).foregroundStyle(.secondary)
    }
}
