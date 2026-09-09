import SwiftUI
import PhotosUI
import UIKit

// MARK: — Экран заказа

struct OrderDetailView: View {
    let orderId: Int
    let clientId: Int

    @State private var order: OrderBrief?
    @State private var busy = false
    @State private var error: String?

    @State private var showEdit = false
    @State private var showCamera = false
    @State private var pickerItem: PhotosPickerItem?
    @State private var fullscreen: PhotoURL?
    @State private var actShare: ShareItem?

    private let columns = [GridItem(.adaptive(minimum: 100), spacing: 8)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                headerBlock
                Divider()
                photoSection
                Divider()
                actSection
            }
            .padding()
        }
        .navigationTitle(order?.number ?? "Заказ")
        .navigationBarTitleDisplayMode(.inline)
        .overlay { if busy { ProgressView().scaleEffect(1.2) } }
        .task { await load() }
        .refreshable { await load() }
        .sheet(isPresented: $showEdit) {
            EditOrderSheet(
                orderId: orderId,
                device: order?.deviceDescription ?? "",
                estimate: order?.total ?? 0
            ) { Task { await load() } }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker(
                onImage: { image in showCamera = false; Task { await upload(image) } },
                onCancel: { showCamera = false }
            )
            .ignoresSafeArea()
        }
        .onChange(of: pickerItem) { item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    await upload(image)
                }
                pickerItem = nil
            }
        }
        .sheet(item: $fullscreen) { p in
            FullScreenImageView(url: Api.shared.absoluteURL(p.url))
        }
        .sheet(item: $actShare) { item in
            ActivityView(items: [item.url])
        }
        .alert("Ошибка", isPresented: Binding(
            get: { error != nil }, set: { if !$0 { error = nil } }
        ), presenting: error) { _ in
            Button("OK", role: .cancel) {}
        } message: { Text($0) }
    }

    // MARK: — секции

    private var headerBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Статус: \(order?.statusRu ?? "…")").foregroundStyle(.secondary)

            Text("Принято в ремонт").font(.headline)
            Text(order?.deviceDescription?.isEmpty == false ? order!.deviceDescription! : "—")

            Text(order.map { $0.total > 0 ? "Примерная стоимость ремонта: \(Int($0.total)) ₽" : "Примерная стоимость: не задана" } ?? "")
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.accentColor.opacity(0.10))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            Button {
                showEdit = true
            } label: {
                Label("Изменить неисправность / оценку", systemImage: "pencil")
            }
            .buttonStyle(.bordered)
        }
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Фото по заказу").font(.headline)

            HStack {
                Button { showCamera = true } label: {
                    Label("Сфотографировать", systemImage: "camera").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!UIImagePickerController.isSourceTypeAvailable(.camera))

                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Label("Галерея", systemImage: "photo").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            let photos = order?.photos ?? []
            if photos.isEmpty {
                Text("Фото по заказу пока нет").foregroundStyle(.secondary).padding(.vertical, 8)
            } else {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(photos, id: \.self) { url in
                        RemoteImage(url: Api.shared.absoluteURL(url))
                            .frame(height: 100)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .contentShape(Rectangle())
                            .onTapGesture { fullscreen = PhotoURL(url: url) }
                            .contextMenu {
                                Button(role: .destructive) {
                                    Task { await deletePhoto(url) }
                                } label: { Label("Удалить", systemImage: "trash") }
                            }
                    }
                }
            }
        }
    }

    private var actSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Акт приёмки").font(.headline)
            Text("Формируется на сервере по данным заказа. Печать — на принтер по AirPrint.")
                .font(.caption).foregroundStyle(.secondary)

            Button {
                Task { await printAct() }
            } label: {
                Label("Акт приёмки — печать", systemImage: "printer").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)

            Button {
                Task { await shareActPdf() }
            } label: {
                Label("Отправить PDF", systemImage: "square.and.arrow.up").frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
    }

    // MARK: — действия

    private func load() async {
        busy = true; defer { busy = false }
        do { order = try await Api.shared.order(orderId) }
        catch { self.error = error.localizedDescription }
    }

    private func upload(_ image: UIImage) async {
        guard let jpeg = image.downscaled().jpeg() else { return }
        busy = true; defer { busy = false }
        do {
            try await Api.shared.uploadOrderPhoto(orderId: orderId, jpeg: jpeg)
            await load()
        } catch { self.error = error.localizedDescription }
    }

    private func deletePhoto(_ url: String) async {
        do {
            try await Api.shared.deleteOrderAttachment(orderId: orderId, url: url)
            await load()
        } catch { self.error = error.localizedDescription }
    }

    private func actFile() async -> URL? {
        do {
            let data = try await Api.shared.acceptanceActPdf(orderId: orderId)
            guard !data.isEmpty else { throw ApiError.message("Пустой ответ сервера") }
            let safe = (order?.number ?? "\(orderId)").replacingOccurrences(of: "/", with: "_")
            let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("act_\(safe).pdf")
            try data.write(to: tmp, options: .atomic)
            return tmp
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    private func printAct() async {
        busy = true; defer { busy = false }
        guard let file = await actFile() else { return }
        let pic = UIPrintInteractionController.shared
        let info = UIPrintInfo.printInfo()
        info.jobName = "Акт приёмки \(order?.number ?? "\(orderId)")"
        info.outputType = .general
        pic.printInfo = info
        pic.printingItem = file
        pic.present(animated: true, completionHandler: nil)
    }

    private func shareActPdf() async {
        busy = true; defer { busy = false }
        if let file = await actFile() { actShare = ShareItem(url: file) }
    }
}

/// Обёртка для .sheet(item:) по строке-URL фото.
struct PhotoURL: Identifiable {
    let url: String
    var id: String { url }
}

// MARK: — Новый заказ

struct NewOrderSheet: View {
    let clientId: Int
    let onCreated: (Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var problem = ""
    @State private var priceText = ""
    @State private var busy = false
    @State private var error: String?

    private var estimate: Double? {
        Double(priceText.replacingOccurrences(of: ",", with: ".").replacingOccurrences(of: " ", with: ""))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Что принято / неисправность") {
                    TextEditor(text: $problem).frame(minHeight: 80)
                }
                Section("Примерная стоимость") {
                    HStack {
                        TextField("Сумма", text: $priceText).keyboardType(.decimalPad)
                        Text("₽").foregroundStyle(.secondary)
                    }
                }
                if let error { Text(error).foregroundStyle(.red).font(.footnote) }
            }
            .navigationTitle("Новый заказ")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Создать") { Task { await create() } }.disabled(busy)
                }
            }
            .overlay { if busy { ProgressView() } }
        }
        .presentationDetents([.medium, .large])
    }

    private func create() async {
        let p = problem.trimmingCharacters(in: .whitespacesAndNewlines)
        if p.isEmpty && estimate == nil {
            error = "Заполните хотя бы одно поле"; return
        }
        busy = true; error = nil
        do {
            let id = try await Api.shared.createOrder(clientId: clientId, deviceDescription: p, estimate: estimate)
            dismiss()
            onCreated(id)
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

// MARK: — Изменить заказ

struct EditOrderSheet: View {
    let orderId: Int
    let device: String
    let estimate: Double
    let onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var problem: String
    @State private var priceText: String
    @State private var busy = false
    @State private var error: String?

    init(orderId: Int, device: String, estimate: Double, onSaved: @escaping () -> Void) {
        self.orderId = orderId
        self.device = device
        self.estimate = estimate
        self.onSaved = onSaved
        _problem = State(initialValue: device)
        _priceText = State(initialValue: estimate > 0 ? String(Int(estimate)) : "")
    }

    private var estimateValue: Double? {
        Double(priceText.replacingOccurrences(of: ",", with: ".").replacingOccurrences(of: " ", with: ""))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Неисправность / что принято") {
                    TextEditor(text: $problem).frame(minHeight: 80)
                }
                Section("Примерная стоимость") {
                    HStack {
                        TextField("Сумма", text: $priceText).keyboardType(.decimalPad)
                        Text("₽").foregroundStyle(.secondary)
                    }
                }
                if let error { Text(error).foregroundStyle(.red).font(.footnote) }
            }
            .navigationTitle("Изменить заказ")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Сохранить") { Task { await save() } }.disabled(busy)
                }
            }
            .overlay { if busy { ProgressView() } }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() async {
        busy = true; error = nil
        do {
            try await Api.shared.updateOrder(
                orderId: orderId,
                deviceDescription: problem.trimmingCharacters(in: .whitespacesAndNewlines),
                estimate: estimateValue
            )
            dismiss()
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}
