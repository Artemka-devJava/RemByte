import SwiftUI
import UIKit

/// Системный лист «Поделиться» (UIActivityViewController) — WhatsApp, Telegram, VK, почта и т.д.
struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
