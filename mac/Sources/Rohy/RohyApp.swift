import AppKit
import CoreImage.CIFilterBuiltins
import RohyCore
import SwiftUI

@main
struct RohyApp: App {
    @StateObject private var model = AppModel()

    init() {
        // `Rohy --render-previews <dir>`: PNG renders of each screen, light and dark, then quit.
        if let index = CommandLine.arguments.firstIndex(of: "--render-previews") {
            let dir = CommandLine.arguments.dropFirst(index + 1).first ?? "previews"
            Previews.render(to: URL(fileURLWithPath: dir))
            exit(0)
        }
    }

    var body: some Scene {
        MenuBarExtra("Rohy", systemImage: model.connected.isEmpty ? "link" : "link.circle.fill") {
            MenuPanel(model: model)
        }
        .menuBarExtraStyle(.window)

        Window("Associer un téléphone", id: "pairing") {
            PairingView(model: model)
        }
        .windowResizability(.contentSize)
    }
}

/// Menu bar panel, drawn like the system's own menus (canvas "Rohy macOS — natif").
struct MenuPanel: View {
    @ObservedObject var model: AppModel
    @Environment(\.openWindow) private var openWindow

    var body: some View {
        TimelineView(.periodic(from: .now, by: 5)) { timeline in
            PanelContent(
                header: header(now: timeline.date),
                phones: model.trusted.map { device in
                    let connected = model.connected.contains { $0.id == device.id }
                    return PhoneRow(
                        id: device.id, name: device.name, connected: connected,
                        detail: DesktopPresentation.deviceDetail(connected: connected, pairedAt: device.pairedAt),
                        pairedOn: DesktopPresentation.shortDate(device.pairedAt)
                    )
                },
                footer: DesktopPresentation.footer(computerName: model.macName, port: model.port),
                error: model.startupError,
                onPair: {
                    openWindow(id: "pairing")
                    NSApp.activate(ignoringOtherApps: true)
                },
                onForget: forget,
                onQuit: { NSApp.terminate(nil) }
            )
        }
    }

    private func header(now: Date) -> DesktopPresentation.Header {
        var failure: String?
        if case .failed(let message) = model.serverState { failure = message }
        return DesktopPresentation.header(
            listening: model.port != nil, failure: failure,
            connected: model.connected.map(\.name), activity: model.lastActivity, now: now
        )
    }

    /// Same confirmation as the Android app before forgetting.
    private func forget(_ id: String) {
        guard let device = model.trusted.first(where: { $0.id == id }) else { return }
        let alert = NSAlert()
        alert.messageText = "Oublier \(device.name) ?"
        alert.informativeText = "Il faudra scanner à nouveau le QR code de ce Mac pour le réassocier."
        alert.addButton(withTitle: "Oublier").hasDestructiveAction = true
        alert.addButton(withTitle: "Annuler")
        NSApp.activate(ignoringOtherApps: true)
        if alert.runModal() == .alertFirstButtonReturn { model.remove(device) }
    }
}

struct PhoneRow: Identifiable {
    let id: String
    let name: String
    let connected: Bool
    let detail: String
    let pairedOn: String
}

struct PanelContent: View {
    let header: DesktopPresentation.Header
    let phones: [PhoneRow]
    let footer: String
    let error: String?
    let onPair: () -> Void
    let onForget: (String) -> Void
    let onQuit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StatusHeader(header: header, error: error)
            MenuSeparator()
            SectionTitle("Téléphones associés")
            if phones.isEmpty {
                Text("Aucun téléphone pour l'instant. Associe ton Android pour copier d'un côté et coller de l'autre.")
                    .font(.system(size: 12)).foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 9).padding(.top, 2).padding(.bottom, 8)
            } else {
                ForEach(phones) { phone in
                    PhoneItem(phone: phone) { onForget(phone.id) }
                }
            }
            MenuSeparator()
            MenuItem("Associer un téléphone…", systemImage: "qrcode", shortcut: "⌘N", action: onPair)
                .keyboardShortcut("n")
            // Non-breaking hyphens: "MacBook-Pro" wraps as a whole.
            Text(footer.replacingOccurrences(of: "-", with: "\u{2011}"))
                .font(.system(size: 11)).foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.leading, 31).padding(.trailing, 9).padding(.top, 1).padding(.bottom, 5)
            MenuSeparator()
            MenuItem("Quitter Rohy", systemImage: "power", shortcut: "⌘Q", action: onQuit)
                .keyboardShortcut("q")
        }
        .padding(5)
        .frame(width: 300)
    }
}

/// "Rohy", then a status dot with the header title, then the last activity or the explanation.
private struct StatusHeader: View {
    let header: DesktopPresentation.Header
    let error: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("Rohy").font(.system(size: 13, weight: .bold))
            HStack(spacing: 7) {
                statusDot
                Text(header.title).font(.system(size: 13))
            }
            if let detail {
                Text(detail)
                    .font(.system(size: 11)).foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.leading, 15)
            }
            if let error {
                Text(error)
                    .font(.system(size: 11)).foregroundStyle(.red)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.leading, 15)
            }
        }
        .padding(.horizontal, 9).padding(.top, 7).padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private var detail: String? {
        if case .connected(_, let activity) = header { return activity }
        return header.subtitle
    }

    @ViewBuilder private var statusDot: some View {
        switch header {
        case .connected: Circle().fill(.green).frame(width: 8, height: 8)
        case .failed: Circle().fill(.red).frame(width: 8, height: 8)
        case .starting, .waiting: Circle().strokeBorder(.secondary, lineWidth: 1.5).frame(width: 8, height: 8)
        }
    }
}

private struct SectionTitle: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold)).foregroundStyle(.secondary)
            .padding(.horizontal, 9).padding(.top, 5).padding(.bottom, 3)
    }
}

private struct MenuSeparator: View {
    var body: some View {
        Rectangle().fill(Color(nsColor: .separatorColor)).frame(height: 1)
            .padding(.horizontal, 9).padding(.vertical, 4)
    }
}

/// A line of the panel that behaves like a menu item: system accent highlight under the pointer.
private struct MenuItem: View {
    let title: String
    let systemImage: String
    let shortcut: String
    let action: () -> Void

    init(_ title: String, systemImage: String, shortcut: String, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.shortcut = shortcut
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage).frame(width: 14)
                Text(title)
                Spacer(minLength: 8)
                Text(shortcut).opacity(0.75)
            }
        }
        .buttonStyle(MenuItemStyle())
    }
}

private struct MenuItemStyle: ButtonStyle {
    @State private var hovered = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 13))
            .foregroundStyle(hovered ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
            .padding(.horizontal, 9).padding(.vertical, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 6).fill(hovered ? Color.accentColor : .clear))
            .contentShape(Rectangle())
            .onHover { hovered = $0 }
    }
}

/// A paired phone. Clicking it (or a right click) opens a native menu with its details and "Oublier".
private struct PhoneItem: View {
    let phone: PhoneRow
    let onForget: () -> Void
    @State private var hovered = false

    var body: some View {
        Button(action: showMenu) {
            HStack(spacing: 9) {
                Image(systemName: "iphone")
                    .font(.system(size: 13))
                    .foregroundStyle(phone.connected ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
                    .frame(width: 26, height: 26)
                    .background(Circle().fill(phone.connected ? Color.accentColor : Color.primary.opacity(0.09)))
                VStack(alignment: .leading, spacing: 1) {
                    Text(phone.name).font(.system(size: 13)).lineLimit(1)
                    Text(phone.detail).font(.system(size: 11)).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold)).foregroundStyle(.secondary)
                    .opacity(hovered ? 1 : 0)
            }
            .padding(.horizontal, 8).padding(.vertical, 5)
            .background(RoundedRectangle(cornerRadius: 7).fill(hovered ? Color.primary.opacity(0.08) : .clear))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovered = $0 }
        .contextMenu {
            Text(phone.connected ? "Connecté" : "Hors ligne")
            Text("Associé le \(phone.pairedOn)")
            Divider()
            Button("Oublier ce téléphone…", action: onForget)
        }
        .accessibilityLabel("\(phone.name), \(phone.detail)")
    }

    private func showMenu() {
        let menu = NSMenu()
        menu.autoenablesItems = false
        for line in [phone.connected ? "Connecté" : "Hors ligne", "Associé le \(phone.pairedOn)"] {
            let item = NSMenuItem(title: line, action: nil, keyEquivalent: "")
            item.isEnabled = false
            menu.addItem(item)
        }
        menu.addItem(.separator())
        let forget = ClosureMenuItem(title: "Oublier ce téléphone…", action: onForget)
        forget.image = NSImage(systemSymbolName: "trash", accessibilityDescription: nil)
        menu.addItem(forget)
        menu.popUp(positioning: nil, at: NSEvent.mouseLocation, in: nil)
    }
}

/// NSMenuItem that runs a closure.
private final class ClosureMenuItem: NSMenuItem {
    private let run: () -> Void

    init(title: String, action: @escaping () -> Void) {
        run = action
        super.init(title: title, action: #selector(fire), keyEquivalent: "")
        target = self
    }

    required init(coder: NSCoder) { fatalError("not used") }

    @objc private func fire() { run() }
}

/// "Associer un téléphone" window: opens and closes the single-use pairing token.
struct PairingView: View {
    @ObservedObject var model: AppModel
    @Environment(\.dismissWindow) private var dismissWindow
    @State private var copied = false

    var body: some View {
        PairingContent(
            qr: model.pairingURI.flatMap(QRCode.image(for:)), copied: copied,
            onCopy: {
                guard let uri = model.pairingURI else { return }
                NSPasteboard.general.clearContents()
                NSPasteboard.general.setString(uri, forType: .string)
                copied = true
            },
            onDone: { dismissWindow(id: "pairing") }
        )
        .onAppear {
            copied = false
            model.openPairing()
        }
        .onChange(of: model.pairingsCompleted) { dismissWindow(id: "pairing") }
        .onDisappear { model.closePairing() }
    }
}

/// Standard window content: QR code on the left, steps on the right, system buttons at the bottom.
struct PairingContent: View {
    let qr: NSImage?
    let copied: Bool
    let onCopy: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 24) {
                QRTile(qr: qr)
                VStack(alignment: .leading, spacing: 12) {
                    Text("Scanne ce code avec ton téléphone").font(.system(size: 17, weight: .semibold))
                        .fixedSize(horizontal: false, vertical: true)
                    VStack(alignment: .leading, spacing: 8) {
                        Step(number: 1, text: "Ouvre Rohy sur ton Android.")
                        Step(number: 2, text: "Touche « Scanner le QR code ».")
                        Step(number: 3, text: "Vise ce code : l'association se fait toute seule.")
                    }
                    Text("Pas de caméra ? Copie le code, envoie-le sur ton téléphone, puis touche « Coller le code d'association » dans l'app.")
                        .font(.system(size: 12)).foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.top, 6)
            }
            HStack(spacing: 10) {
                Spacer()
                Button(action: onCopy) {
                    if copied {
                        Label { Text("Code copié") } icon: { Image(systemName: "checkmark").foregroundStyle(.green) }
                    } else {
                        Label("Copier le code", systemImage: "doc.on.doc")
                    }
                }
                .disabled(qr == nil)
                Button("Terminé", action: onDone)
                    .keyboardShortcut(.defaultAction)
            }
            .controlSize(.large)
            .padding(.top, 20)
        }
        .padding(.horizontal, 24).padding(.top, 10).padding(.bottom, 20)
        .frame(width: 560)
    }
}

private struct Step: View {
    let number: Int
    let text: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text("\(number).").foregroundStyle(.secondary).frame(width: 14, alignment: .leading)
            Text(text).fixedSize(horizontal: false, vertical: true)
        }
        .font(.system(size: 13))
    }
}

/// QR code dark on white in both appearances so that every camera reads it, with the Rohy mark in the center.
private struct QRTile: View {
    let qr: NSImage?

    var body: some View {
        ZStack {
            if let qr {
                Image(nsImage: qr).interpolation(.none).resizable().frame(width: 184, height: 184)
                RohyMark(size: 34)
                    .padding(4).background(RoundedRectangle(cornerRadius: 10).fill(.white))
            } else {
                VStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("Le serveur démarre…").font(.system(size: 12)).foregroundStyle(.black.opacity(0.6))
                }
            }
        }
        .frame(width: 208, height: 208)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color(nsColor: .separatorColor), lineWidth: 0.5))
        .accessibilityLabel("QR code d'association")
    }
}

enum QRCode {
    static func image(for text: String) -> NSImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        // Level H tolerates the Rohy mark drawn over the center.
        filter.correctionLevel = "H"
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)) else { return nil }
        let rep = NSCIImageRep(ciImage: output)
        let image = NSImage(size: rep.size)
        image.addRepresentation(rep)
        return image
    }
}
