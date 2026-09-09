import SwiftUI

struct ShareItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct ClientDetailView: View {
    let clientId: Int

    @State private var detail: ClientDetail?
    @State private var summary: ClientSummary?
    @State private var orders: [OrderBrief] = []

    @State private var busy = false
    @State private var error: String?
    @State private var showNewOrder = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                if let s = summary { summaryStrip(s) }
                if let d = detail { details(d) }
                Divider()
                ordersSection
            }
            .padding()
        }
        .navigationTitle(detail?.name ?? "Клиент")
        .navigationBarTitleDisplayMode(.inline)
        .overlay { if busy { ProgressView().scaleEffect(1.2) } }
        .task { await loadAll() }
        .refreshable { await loadOrders() }
        .sheet(isPresented: $showNewOrder) {
            NewOrderSheet(clientId: clientId) { _ in
                Task { await loadOrders() }
            }
        }
        .alert("Ошибка", isPresented: Binding(
            get: { error != nil }, set: { if !$0 { error = nil } }
        ), presenting: error) { _ in
            Button("OK", role: .cancel) {}
        } message: { Text($0) }
    }

    // MARK: — секции

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(detail?.name ?? "…").font(.title2).bold()
                if detail?.archived == true {
                    Text("архив").font(.caption)
                        .padding(.horizontal, 6).padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.2)).clipShape(Capsule())
                }
            }
            Text(subline).foregroundStyle(.secondary).font(.subheadline)

            if let d = detail {
                HStack {
                    if let call = URL(string: "tel:\(d.phone.digitsAndPlus)") {
                        Link(destination: call) {
                            Label("Позвонить", systemImage: "phone").frame(maxWidth: .infinity)
                        }.buttonStyle(.bordered)
                    }
                    if let write = writeURL(d) {
                        Link(destination: write) {
                            Label("Написать", systemImage: "paperplane").frame(maxWidth: .infinity)
                        }.buttonStyle(.bordered)
                    }
                }
                .padding(.top, 4)
            }
        }
    }

    private var subline: String {
        guard let d = detail else { return "" }
        let t = d.type == "COMPANY" ? "Организация" : "Физлицо"
        return [d.phone.isEmpty ? nil : d.phone, t].compactMap { $0 }.joined(separator: "  ·  ")
    }

    private func summaryStrip(_ s: ClientSummary) -> some View {
        HStack {
            stat("\(s.ordersCount)", "заказов")
            Spacer()
            stat(money(s.revenue), "оборот")
            Spacer()
            stat(money(s.debt), "долг", danger: s.debt > 0)
        }
        .padding(12)
        .background(Color.accentColor.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func stat(_ value: String, _ label: String, danger: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.headline).foregroundStyle(danger ? Color.red : Color.primary)
            Text(label.uppercased()).font(.caption2).foregroundStyle(.secondary)
        }
    }

    private func details(_ d: ClientDetail) -> some View {
        let lines: [String] = [
            d.email.map { "Email: \($0)" },
            d.address.map { "Адрес: \($0)" },
            d.tags.map { "Метки: \($0)" },
            d.notes.map { "Заметка: \($0)" }
        ].compactMap { $0 }
        return Group {
            if !lines.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(lines, id: \.self) { Text($0) }
                }.font(.callout)
            }
        }
    }

    private var ordersSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Заказы").font(.headline)
                Spacer()
                Button {
                    showNewOrder = true
                } label: {
                    Label("Заказ", systemImage: "plus")
                }
            }

            if orders.isEmpty {
                Text("Заказов пока нет").foregroundStyle(.secondary).padding(.vertical, 6)
            } else {
                VStack(spacing: 0) {
                    ForEach(orders) { o in
                        NavigationLink(value: Route.order(orderId: o.id, clientId: clientId)) {
                            orderRow(o)
                        }
                        .buttonStyle(.plain)
                        Divider()
                    }
                }
            }

            Text("Фото делаются внутри заказа — откройте заказ и добавьте снимки там.")
                .font(.caption).foregroundStyle(.secondary).padding(.top, 4)
        }
    }

    private func orderRow(_ o: OrderBrief) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(o.number).font(.subheadline).bold()
                Spacer()
                Text(o.statusRu).font(.caption).foregroundStyle(Color.accentColor)
            }
            if let dev = o.deviceDescription, !dev.isEmpty {
                Text(dev).font(.caption).foregroundStyle(.secondary).lineLimit(2)
            }
            Text([
                o.total > 0 ? money(o.total) : "оценка не задана",
                o.createdAt?.prefix(10).split(separator: "-").reversed().joined(separator: ".") ?? ""
            ].filter { !$0.isEmpty }.joined(separator: "  ·  "))
                .font(.caption2).foregroundStyle(.secondary)
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
    }

    // MARK: — действия

    private func loadAll() async {
        do {
            detail = try await Api.shared.client(clientId)
        } catch {
            self.error = error.localizedDescription
        }
        summary = try? await Api.shared.summary(clientId)
        await loadOrders()
    }

    private func loadOrders() async {
        orders = (try? await Api.shared.clientOrders(clientId)) ?? []
    }

    private func writeURL(_ d: ClientDetail) -> URL? {
        let digits = d.phone.digitsOnly
        switch d.preferredChannel {
        case "TELEGRAM": return URL(string: "https://t.me/+\(digits)")
        case "WHATSAPP": return URL(string: "https://wa.me/\(digits)")
        default:
            if let e = d.email, !e.isEmpty { return URL(string: "mailto:\(e)") }
            return digits.isEmpty ? nil : URL(string: "https://wa.me/\(digits)")
        }
    }

    private func money(_ v: Double) -> String { "\(Int(v))₽" }
}

struct FullScreenImageView: View {
    let url: URL
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            RemoteImage(url: url, contentMode: .fit) { Color.black }
                .ignoresSafeArea()
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(.white.opacity(0.9))
                    .padding()
            }
        }
    }
}
