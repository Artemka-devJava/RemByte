import SwiftUI
import PhotosUI

struct ShareItem: Identifiable {
    let id = UUID()
    let url: URL
}

struct ClientDetailView: View {
    let clientId: Int

    @State private var detail: ClientDetail?
    @State private var summary: ClientSummary?
    @State private var photos: [ClientPhoto] = []

    @State private var caption = ""
    @State private var busy = false
    @State private var error: String?

    @State private var showCamera = false
    @State private var pickerItem: PhotosPickerItem?
    @State private var fullscreen: ClientPhoto?
    @State private var share: ShareItem?
    @State private var pendingDelete: ClientPhoto?

    private let columns = [GridItem(.adaptive(minimum: 100), spacing: 8)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                if let s = summary { summaryStrip(s) }
                if let d = detail { details(d) }
                Divider()
                photoSection
            }
            .padding()
        }
        .navigationTitle(detail?.name ?? "Клиент")
        .navigationBarTitleDisplayMode(.inline)
        .overlay { if busy { ProgressView().scaleEffect(1.2) } }
        .task { await loadAll() }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker(
                onImage: { image in
                    showCamera = false
                    Task { await upload(image) }
                },
                onCancel: { showCamera = false }
            )
            .ignoresSafeArea()
        }
        .onChange(of: pickerItem) { item in
            guard let item else { return }
            Task {
                let loaded = (try? await item.loadTransferable(type: Data.self)) ?? nil
                if let data = loaded, let image = UIImage(data: data) {
                    await upload(image)
                }
                pickerItem = nil
            }
        }
        .sheet(item: $fullscreen) { p in
            FullScreenImageView(url: Api.shared.absoluteURL(p.url))
        }
        .sheet(item: $share) { item in
            ActivityView(items: [item.url])
        }
        .confirmationDialog("Удалить фото?", isPresented: Binding(
            get: { pendingDelete != nil },
            set: { if !$0 { pendingDelete = nil } }
        ), presenting: pendingDelete) { p in
            Button("Удалить", role: .destructive) { Task { await deletePhoto(p) } }
            Button("Отмена", role: .cancel) {}
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

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Фото проблемы / устройства").font(.headline)

            TextField("Подпись (необязательно)", text: $caption)
                .textFieldStyle(.roundedBorder)

            HStack {
                Button {
                    showCamera = true
                } label: {
                    Label("Сфотографировать", systemImage: "camera").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!UIImagePickerController.isSourceTypeAvailable(.camera))

                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Label("Галерея", systemImage: "photo").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            if photos.isEmpty {
                Text("Фото пока нет").foregroundStyle(.secondary).padding(.vertical, 8)
            } else {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(photos) { p in
                        RemoteImage(url: Api.shared.absoluteURL(p.url))
                            .frame(height: 100)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .contentShape(Rectangle())
                            .onTapGesture { fullscreen = p }
                            .contextMenu {
                                Button {
                                    Task { await sharePhoto(p) }
                                } label: { Label("Поделиться", systemImage: "square.and.arrow.up") }
                                Button(role: .destructive) {
                                    pendingDelete = p
                                } label: { Label("Удалить", systemImage: "trash") }
                            }
                    }
                }
            }
        }
    }

    // MARK: — действия

    private func loadAll() async {
        do {
            detail = try await Api.shared.client(clientId)
        } catch {
            self.error = error.localizedDescription
        }
        summary = try? await Api.shared.summary(clientId)
        photos = (try? await Api.shared.photos(clientId)) ?? []
    }

    private func reloadPhotos() async {
        photos = (try? await Api.shared.photos(clientId)) ?? []
    }

    private func upload(_ image: UIImage) async {
        guard let jpeg = image.downscaled().jpeg() else { return }
        busy = true; defer { busy = false }
        do {
            try await Api.shared.uploadPhoto(
                clientId: clientId, jpeg: jpeg,
                caption: caption.trimmingCharacters(in: .whitespaces)
            )
            caption = ""
            await reloadPhotos()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func deletePhoto(_ p: ClientPhoto) async {
        do {
            try await Api.shared.deletePhoto(clientId: clientId, photoId: p.id)
            await reloadPhotos()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// Скачать фото из CRM и отдать в системный «Поделиться».
    private func sharePhoto(_ p: ClientPhoto) async {
        busy = true; defer { busy = false }
        do {
            let data = try await Api.shared.downloadData(Api.shared.absoluteURL(p.url))
            let tmp = FileManager.default.temporaryDirectory
                .appendingPathComponent("photo_\(p.id).jpg")
            try data.write(to: tmp, options: .atomic)
            share = ShareItem(url: tmp)
        } catch {
            self.error = error.localizedDescription
        }
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
