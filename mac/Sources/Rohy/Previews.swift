import AppKit
import RohyCore
import SwiftUI

/// Off-screen PNG renders of each screen in both appearances, to compare with the design board
/// "Rohy macOS — natif". The menu bar panel's system material is not drawn off screen: the renders put the
/// panel on the window background instead.
@MainActor
enum Previews {
    static func render(to directory: URL) {
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let paired = Date(timeIntervalSince1970: 1_795_009_797)
        let earlier = paired.addingTimeInterval(-16 * 86_400)
        let phones = [
            PhoneRow(id: "a", name: "SM-S928B", connected: true,
                     detail: DesktopPresentation.deviceDetail(connected: true, pairedAt: paired),
                     pairedOn: DesktopPresentation.shortDate(paired)),
            PhoneRow(id: "b", name: "Pixel 9", connected: false,
                     detail: DesktopPresentation.deviceDetail(connected: false, pairedAt: earlier),
                     pairedOn: DesktopPresentation.shortDate(earlier)),
        ]
        let footer = DesktopPresentation.footer(computerName: "MacBook-Pro", port: 47823)
        let connected = DesktopPresentation.Header.connected(names: "SM-S928B", activity: "Texte reçu de SM-S928B · il y a 12 s")
        let qr = QRCode.image(for: "rohy://pair?v=1&pk=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&t=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&n=MacBook-Pro&h=192.168.88.3&p=47823")
        let screens: [(String, AnyView)] = [
            ("pairing", AnyView(PairingContent(qr: qr, copied: false, onCopy: {}, onDone: {}))),
            ("pairing-copied", AnyView(PairingContent(qr: qr, copied: true, onCopy: {}, onDone: {}))),
            ("panel", AnyView(panel(connected, phones, footer))),
            ("panel-empty", AnyView(panel(.waiting, [], footer))),
            ("panel-error", AnyView(panel(.failed("Le port 47823 est déjà utilisé."), phones.map(offline), footer))),
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

    private static func panel(_ header: DesktopPresentation.Header, _ phones: [PhoneRow], _ footer: String) -> some View {
        PanelContent(header: header, phones: phones, footer: footer, error: nil, onPair: {}, onForget: { _ in }, onQuit: {})
    }

    private static func offline(_ phone: PhoneRow) -> PhoneRow {
        PhoneRow(id: phone.id, name: phone.name, connected: false,
                 detail: phone.detail.replacingOccurrences(of: "Connecté", with: "Hors ligne"), pairedOn: phone.pairedOn)
    }
}
