import SwiftUI

/// Картинка из CRM, загружается тем же клиентом (с cookie сессии), с простым кешем в памяти.
struct RemoteImage<Placeholder: View>: View {
    let url: URL
    let contentMode: ContentMode
    @ViewBuilder let placeholder: () -> Placeholder

    @State private var image: UIImage?
    @State private var failed = false

    init(url: URL,
         contentMode: ContentMode = .fill,
         @ViewBuilder placeholder: @escaping () -> Placeholder = { Color.gray.opacity(0.2) }) {
        self.url = url
        self.contentMode = contentMode
        self.placeholder = placeholder
    }

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
            } else if failed {
                placeholder().overlay(Image(systemName: "photo").foregroundStyle(.secondary))
            } else {
                placeholder().overlay(ProgressView())
            }
        }
        .task(id: url) { await load() }
    }

    private func load() async {
        if let cached = ImageMemoryCache.shared.image(for: url) {
            image = cached
            return
        }
        do {
            let data = try await Api.shared.downloadData(url)
            if let ui = UIImage(data: data) {
                ImageMemoryCache.shared.set(ui, for: url)
                image = ui
            } else {
                failed = true
            }
        } catch {
            failed = true
        }
    }
}

final class ImageMemoryCache {
    static let shared = ImageMemoryCache()
    private let cache = NSCache<NSURL, UIImage>()
    func image(for url: URL) -> UIImage? { cache.object(forKey: url as NSURL) }
    func set(_ image: UIImage, for url: URL) { cache.setObject(image, forKey: url as NSURL) }
}
