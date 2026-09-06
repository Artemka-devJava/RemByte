import UIKit

extension UIImage {
    /// Ужать до maxDimension по большей стороне (для загрузки — быстрее и не упрётся в лимит 15 МБ).
    func downscaled(maxDimension: CGFloat = 1600) -> UIImage {
        let maxSide = max(size.width, size.height)
        guard maxSide > maxDimension else { return self }
        let scale = maxDimension / maxSide
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: newSize, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: newSize))
        }
    }

    func jpeg(quality: CGFloat = 0.85) -> Data? {
        jpegData(compressionQuality: quality)
    }
}
