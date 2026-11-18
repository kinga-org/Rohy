import AppKit
import CoreText
import RohyCore
import SwiftUI

/// Off-screen PNG renders of each screen in both appearances, to compare with the design board and with
/// the Windows/Linux app (`ROHY_SNAPSHOT_DIR=… cargo test -p rohy-desktop -- --ignored renders`).
@MainActor
enum Previews {
    static func render(to directory: URL) {
        registerFonts()
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let paired = Date(timeIntervalSince1970: 1_795_009_797)
        let phones = [
            PhoneRow(id: "a", name: "SM-S928B", connected: true,
                     detail: DesktopPresentation.deviceDetail(connected: true, pairedAt: paired)),
            PhoneRow(id: "b", name: "Pixel 9", connected: false,
                     detail: DesktopPresentation.deviceDetail(connected: false, pairedAt: paired.addingTimeInterval(-16 * 86_400))),
        ]
        let footer = DesktopPresentation.footer(computerName: "MacBook-Pro", port: 47823)
        let connected = DesktopPresentation.Header.connected(names: "SM-S928B", activity: "Texte reçu de SM-S928B · il y a 12 s")
        let qr = QRCode.image(for: "rohy://pair?v=1&pk=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&t=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&n=MacBook-Pro&h=192.168.88.3&p=47823")
        let screens: [(String, AnyView)] = [
            ("pairing", AnyView(PairingContent(qr: qr, copied: false) {})),
            ("panel", AnyView(panel(connected, phones, footer))),
            ("panel-empty", AnyView(panel(.waiting, [], footer))),
        ]
        for (scheme, suffix) in [(ColorScheme.light, "light"), (.dark, "dark")] {
            for (name, view) in screens {
                let renderer = ImageRenderer(content: view.environment(\.colorScheme, scheme))
                renderer.scale = 2
                guard let image = renderer.nsImage, let tiff = image.tiffRepresentation,
                      let png = NSBitmapImageRep(data: tiff)?.representation(using: .png, properties: [:]) else { continue }
                try? png.write(to: directory.appendingPathComponent("\(name)-\(suffix).png"))
            }
        }
    }

    private static func panel(_ header: DesktopPresentation.Header, _ phones: [PhoneRow], _ footer: String) -> some View {
        PanelContent(
            header: header, phones: phones, footer: footer, error: nil,
            appearance: .constant(.system), onPair: {}, onForget: { _ in }, onQuit: {}
        )
    }

    /// Inter from the app bundle, or from `mac/Resources/Fonts` when run with `swift run` in `mac/`.
    private static func registerFonts() {
        let candidates = [Bundle.main.resourceURL?.appendingPathComponent("Fonts"), URL(fileURLWithPath: "Resources/Fonts")]
        guard let folder = candidates.compactMap({ $0 }).first(where: { FileManager.default.fileExists(atPath: $0.path) }),
              let files = try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil) else { return }
        for file in files where file.pathExtension == "otf" {
            CTFontManagerRegisterFontsForURL(file as CFURL, .process, nil)
        }
    }
}
