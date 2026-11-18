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

/// Menu bar panel: the "Téléphones associés" screen of the design board, plus Apparence and Quitter.
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
                        detail: DesktopPresentation.deviceDetail(connected: connected, pairedAt: device.pairedAt)
                    )
                },
                footer: DesktopPresentation.footer(computerName: model.macName, port: model.port),
                error: model.startupError,
                appearance: $model.appearance,
                onPair: {
                    openWindow(id: "pairing")
                    NSApp.activate(ignoringOtherApps: true)
                },
                onForget: { id in model.trusted.first { $0.id == id }.map(model.remove) },
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
}

struct PhoneRow: Identifiable {
    let id: String
    let name: String
    let connected: Bool
    let detail: String
}

struct PanelContent: View {
    let header: DesktopPresentation.Header
    let phones: [PhoneRow]
    let footer: String
    let error: String?
    @Binding var appearance: Appearance
    let onPair: () -> Void
    let onForget: (String) -> Void
    let onQuit: () -> Void
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let p = Palette.of(scheme)
        VStack(alignment: .leading, spacing: 18) {
            HeaderView(header: header, palette: p)
            if phones.isEmpty {
                EmptyPhones(palette: p)
            } else {
                PhoneList(phones: phones, palette: p, onForget: onForget)
            }
            VStack(spacing: 10) {
                Button(action: onPair) { Label("Associer un téléphone", systemImage: "plus") }
                    .buttonStyle(PrimaryButtonStyle(palette: p))
                Text(footer)
                    .font(Typeface.inter(12)).foregroundStyle(p.muted)
                    .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                if let error {
                    Text(error).font(Typeface.inter(12)).foregroundStyle(p.danger).frame(maxWidth: .infinity)
                }
            }
            Rectangle().fill(p.divider).frame(height: 1)
            HStack {
                Menu {
                    Picker("Apparence", selection: $appearance) {
                        ForEach(Appearance.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.inline)
                } label: {
                    Label("Apparence", systemImage: "circle.lefthalf.filled")
                }
                .menuStyle(.borderlessButton)
                .fixedSize()
                .font(Typeface.inter(13, .medium))
                .foregroundStyle(p.ink)
                Spacer()
                Button("Quitter Rohy", action: onQuit)
                    .buttonStyle(QuietButtonStyle(color: p.muted, hoverFill: p.accentSoft))
            }
        }
        .padding(20)
        .frame(width: 420)
        .background(p.ground)
        .foregroundStyle(p.ink)
    }
}

private struct HeaderView: View {
    let header: DesktopPresentation.Header
    let palette: Palette

    var body: some View {
        if case .connected(_, let activity) = header {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "link")
                        .font(.system(size: 17, weight: .semibold)).foregroundStyle(.white)
                        .frame(width: 40, height: 40).background(Circle().fill(.white.opacity(0.14)))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("SYNCHRONISATION ACTIVE").font(Typeface.inter(12, .medium)).foregroundStyle(Palette.onBrandLabel)
                        Text(header.title).font(Typeface.inter(19, .bold)).foregroundStyle(.white)
                    }
                }
                if let activity {
                    Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
                    Label(activity, systemImage: "arrow.down.to.line")
                        .font(Typeface.inter(13)).foregroundStyle(Palette.onBrandBody)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 16).fill(Palette.brand))
        } else {
            HStack(spacing: 12) {
                if case .failed = header {
                    Dot(color: palette.danger, size: 10)
                } else {
                    Dot(color: palette.offline, size: 10, hollow: true)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(header.title).font(Typeface.inter(16, .semibold))
                    if let subtitle = header.subtitle {
                        Text(subtitle).font(Typeface.inter(12)).foregroundStyle(palette.muted)
                    }
                }
            }
            .padding(.horizontal, 20).padding(.vertical, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .card(palette)
        }
    }
}

private struct PhoneList: View {
    let phones: [PhoneRow]
    let palette: Palette
    let onForget: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("TÉLÉPHONES ASSOCIÉS").font(Typeface.inter(13, .semibold)).foregroundStyle(palette.muted)
            VStack(spacing: 0) {
                ForEach(Array(phones.enumerated()), id: \.element.id) { index, phone in
                    if index > 0 { Rectangle().fill(palette.divider).frame(height: 1).padding(.horizontal, 16) }
                    row(phone)
                }
            }
            .card(palette)
        }
    }

    private func row(_ phone: PhoneRow) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "iphone")
                .font(.system(size: 17)).foregroundStyle(phone.connected ? palette.accentText : palette.muted)
                .frame(width: 40, height: 40)
                .background(RoundedRectangle(cornerRadius: 10).fill(phone.connected ? palette.accentSoft : palette.neutralSoft))
            VStack(alignment: .leading, spacing: 3) {
                Text(phone.name).font(Typeface.inter(15, .semibold)).lineLimit(1)
                HStack(spacing: 6) {
                    Dot(color: phone.connected ? palette.success : palette.offline, size: 7, hollow: !phone.connected)
                    Text(phone.detail).font(Typeface.inter(12)).foregroundStyle(phone.connected ? palette.successText : palette.muted)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            Button("Oublier") { onForget(phone.id) }
                .buttonStyle(QuietButtonStyle(color: palette.danger, hoverFill: palette.dangerSoft))
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
    }
}

private struct EmptyPhones: View {
    let palette: Palette

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "iphone")
                .font(.system(size: 28)).foregroundStyle(palette.accentText)
                .frame(width: 64, height: 64).background(Circle().fill(palette.accentSoft))
            Text("Aucun téléphone associé").font(Typeface.inter(16, .bold))
            Text("Associe ton téléphone Android pour copier d'un côté et coller de l'autre.")
                .font(Typeface.inter(13)).foregroundStyle(palette.muted).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

/// "Associer un téléphone" window: opens and closes the single-use pairing token.
struct PairingView: View {
    @ObservedObject var model: AppModel
    @Environment(\.dismissWindow) private var dismissWindow
    @State private var copied = false

    var body: some View {
        PairingContent(qr: model.pairingURI.flatMap(QRCode.image(for:)), copied: copied) {
            guard let uri = model.pairingURI else { return }
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(uri, forType: .string)
            copied = true
        }
        .onAppear {
            copied = false
            model.openPairing()
        }
        .onChange(of: model.pairingsCompleted) { dismissWindow(id: "pairing") }
        .onDisappear { model.closePairing() }
    }
}

/// "Associer un téléphone" screen of the design board.
struct PairingContent: View {
    let qr: NSImage?
    let copied: Bool
    let onCopy: () -> Void
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let p = Palette.of(scheme)
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 12) {
                RohyMark(size: 40, disc: p.logo)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Associer un téléphone").font(Typeface.inter(20, .bold))
                    Text("Ouvre Rohy sur ton téléphone, puis scanne ce code.").font(Typeface.inter(13)).foregroundStyle(p.muted)
                }
            }
            VStack(spacing: 18) {
                if let qr {
                    // Dark on white in both appearances so that every camera reads it.
                    ZStack {
                        Image(nsImage: qr).interpolation(.none).resizable().frame(width: 232, height: 232)
                        RohyMark(size: 36, disc: Palette.brand)
                            .padding(4).background(RoundedRectangle(cornerRadius: 12).fill(.white))
                    }
                    .padding(8)
                    .background(RoundedRectangle(cornerRadius: 10).fill(.white))
                } else {
                    Text("Le serveur démarre…").font(Typeface.inter(14)).foregroundStyle(p.muted).frame(width: 248, height: 248)
                }
                HStack(spacing: 10) {
                    Dot(color: p.accentText)
                    Text("En attente du scan…").font(Typeface.inter(13, .medium)).foregroundStyle(p.accentText)
                }
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Capsule().fill(p.accentSoft))
            }
            .padding(24)
            .frame(maxWidth: .infinity)
            .card(p)
            HStack(spacing: 12) {
                Rectangle().fill(p.line).frame(height: 1)
                Text("Sans caméra").font(Typeface.inter(12)).foregroundStyle(p.muted).fixedSize()
                Rectangle().fill(p.line).frame(height: 1)
            }
            VStack(spacing: 8) {
                Button(action: onCopy) {
                    Label(copied ? "Code copié" : "Copier le code d'association", systemImage: copied ? "checkmark" : "doc.on.doc")
                }
                .buttonStyle(SecondaryButtonStyle(palette: p))
                .disabled(qr == nil)
                Text("Colle-le ensuite dans l'app Rohy du téléphone.")
                    .font(Typeface.inter(12)).foregroundStyle(p.muted).frame(maxWidth: .infinity)
            }
            Spacer(minLength: 0)
            Label {
                Text("Code à usage unique, valable tant que cette fenêtre est ouverte. Les échanges sont chiffrés de bout en bout.")
            } icon: {
                Image(systemName: "lock")
            }
            .font(Typeface.inter(12)).foregroundStyle(p.muted)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(28)
        .frame(width: 440, height: 640)
        .background(p.ground)
        .foregroundStyle(p.ink)
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
