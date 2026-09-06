import SwiftUI

struct CreateClientSheet: View {
    /// Вызывается с id созданного клиента (или id существующего, если открыли дубль).
    let onOpen: (Int) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var phone = ""
    @State private var type = "INDIVIDUAL"
    @State private var busy = false
    @State private var error: String?
    @State private var duplicate: (id: Int, name: String)?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("ФИО / организация", text: $name)
                    TextField("Телефон", text: $phone)
                        .keyboardType(.phonePad)
                    Picker("Тип", selection: $type) {
                        Text("Физлицо").tag("INDIVIDUAL")
                        Text("Организация").tag("COMPANY")
                    }
                    .pickerStyle(.segmented)
                }
                if let error {
                    Text(error).foregroundStyle(.red).font(.footnote)
                }
            }
            .navigationTitle("Новый клиент")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Создать") { Task { await create() } }
                        .disabled(busy)
                }
            }
            .overlay { if busy { ProgressView() } }
            .alert("Клиент уже есть", isPresented: Binding(
                get: { duplicate != nil },
                set: { if !$0 { duplicate = nil } }
            ), presenting: duplicate) { dup in
                Button("Открыть карточку") {
                    dismiss()
                    onOpen(dup.id)
                }
                Button("Отмена", role: .cancel) {}
            } message: { dup in
                Text("Клиент с этим телефоном уже есть: \(dup.name)")
            }
        }
        .presentationDetents([.medium])
    }

    private func create() async {
        let n = name.trimmingCharacters(in: .whitespaces)
        let p = phone.trimmingCharacters(in: .whitespaces)
        guard !n.isEmpty, !p.isEmpty else {
            error = "Имя и телефон обязательны"; return
        }
        busy = true; error = nil
        do {
            switch try await Api.shared.createClient(name: n, phone: p, type: type) {
            case .created(let id):
                dismiss()
                onOpen(id)
            case .duplicate(let id, let dupName):
                duplicate = (id, dupName)
            }
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}
