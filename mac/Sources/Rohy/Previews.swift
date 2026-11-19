import AppKit
import SwiftUI

/// Off-screen PNG renders of the pairing window, light and dark, to compare with the design board
/// "Rohy macOS — natif". The menu is made of standard `NSMenuItem`s and cannot be rendered off screen.
@MainActor
enum Previews {
    static func render(to directory: URL) {
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let qr = QRCode.image(for: "rohy://pair?v=1&pk=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&t=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&n=MacBook-Pro&h=192.168.88.3&p=47823")
        let screens: [(String, AnyView)] = [
            ("pairing", AnyView(PairingContent(qr: qr, copied: false, onCopy: {}, onDone: {}))),
            ("pairing-copied", AnyView(PairingContent(qr: qr, copied: true, onCopy: {}, onDone: {}))),
        ]
        for (scheme, suffix) in [(ColorScheme.light, "light"), (.dark, "dark")] {
            for (name, view) in screens {
                let content = view
                    .background(Color(nsColor: .windowBackgroundColor))
                    .environment(\.colorScheme, scheme)
                let renderer = ImageRenderer(content: content)
                renderer.scale = 2
                guard let image = renderer.nsImage, let tiff = image.tiffRepresentation,
                      let png = NSBitmapImageRep(data: tiff)?.representation(using: .png, properties: [:]) else { continue }
                try? png.write(to: directory.appendingPathComponent("\(name)-\(suffix).png"))
            }
        }
    }
}
