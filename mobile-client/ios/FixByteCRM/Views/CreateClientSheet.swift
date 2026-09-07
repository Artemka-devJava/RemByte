import SwiftUI

struct CreateClientSheet: View {
    /// Вызывается с id созданного клиента (или id существующего, если открыли дубль).
    let onOpen: (Int) -> Void

    @Environment(\.dismiss) private var dismiss

    private let phonePrefix = "+7 "

    @State private var name = ""
    @State private var phone = "+7 "
    @State private var type = "INDIVIDUAL"
    @State private var problem = ""
    @State private var priceText = ""
    @State private var busy = false
    @State private var error: String?
    @State private var duplicate: (id: Int, name: String)?

    private var estimate: Double? {
        Double(priceText.replacingOccurrences(of: ",", with: ".")
            .replacingOccurrences(of: " ", with: ""))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Клиент") {
                    TextField("ФИО / организация", text: $name)
                        .textInputAutocapitalization(.words)
                    TextField("Телефон", text: $phone)
                        .keyboardType(.phonePad)
                        .onChange(of: phone) { newValue in reformatPhone(newValue) }
                    Picker("Тип", selection: $type) {
                        Text("Физлицо").tag("INDIVIDUAL")
                        Text("Организация").tag("COMPANY")
                    }
                    .pickerStyle(.segmented)
                }

                Section("Первичная заявка (необязательно)") {
                    ZStack(alignment: .topLeading) {
                        if problem.isEmpty {
                            Text("Проблема с ПК / устройством")
                                .foregroundStyle(.secondary)
                                .padding(.top, 8)
                                .padding(.leading, 4)
                        }
                        TextEditor(text: $problem)
                            .frame(minHeight: 80)
                    }
                    HStack {
                        TextField("Примерная цена (озвучена клиенту)", text: $priceText)
                            .keyboardType(.decimalPad)
                        Text("₽").foregroundStyle(.secondary)
                    }
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
        .presentationDetents([.large])
    }

    /// Поле всегда начинается с "+7 " — префикс нельзя стереть/испортить.
    private func reformatPhone(_ raw: String) {
        let local: String
        if raw.hasPrefix(phonePrefix) {
            local = String(raw.dropFirst(phonePrefix.count))
        } else {
            var digits = raw.digitsOnly
            if digits.hasPrefix("7") || digits.hasPrefix("8") { digits.removeFirst() }
            local = digits
        }
        var digits = local.digitsOnly
        if digits.count > 10 { digits = String(digits.prefix(10)) }
        let formatted = phonePrefix + digits
        if formatted != raw { phone = formatted }
    }

    private func create() async {
        let n = name.trimmingCharacters(in: .whitespaces)
        guard !n.isEmpty else {
            error = "Введите имя клиента"; return
        }
        guard let normalizedPhone = phone.normalizedRuPhone else {
            error = "Введите корректный номер телефона: +7 и 10 цифр"; return
        }
        busy = true; error = nil
        do {
            let res = try await Api.shared.createClient(
                name: n, phone: normalizedPhone, type: type,
                problem: problem.trimmingCharacters(in: .whitespacesAndNewlines),
                estimate: estimate
            )
            switch res {
            case .created(let id, _):
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
