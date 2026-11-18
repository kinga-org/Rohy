import AppKit
import CoreImage.CIFilterBuiltins
import RohyCore
import SwiftUI

@main
struct RohyApp: App {
    @StateObject private var model = AppModel()

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

struct MenuPanel: View {
    @ObservedObject var model: AppModel
    @Environment(\.openWindow) private var openWindow

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(statusText, systemImage: statusIcon)
                .font(.headline)
            if let error = model.startupError {
                Text(error).foregroundStyle(.red).font(.callout)
            }
            if let event = model.lastEvent {
                Label(event, systemImage: "clock").font(.callout).foregroundStyle(.secondary)
            }
            Divider()
            Text("Téléphones associés").font(.subheadline).foregroundStyle(.secondary)
            if model.trusted.isEmpty {
                Text("Aucun téléphone pour l'instant.").font(.callout)
            }
            ForEach(model.trusted) { device in
                HStack {
                    Image(systemName: "iphone.gen3")
                    Text(device.name)
                    if model.connected.contains(where: { $0.id == device.id }) {
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.green).help("Connecté")
                    }
                    Spacer()
                    Button { model.remove(device) } label: { Image(systemName: "trash") }
                        .buttonStyle(.borderless)
                        .help("Oublier ce téléphone")
                }
            }
            Divider()
            Button {
                openWindow(id: "pairing")
                NSApp.activate(ignoringOtherApps: true)
            } label: {
                Label("Associer un téléphone", systemImage: "qrcode")
            }
            Button {
                NSApp.terminate(nil)
            } label: {
                Label("Quitter Rohy", systemImage: "power")
            }
        }
        .padding(14)
        .frame(width: 300)
    }

    private var statusText: String {
        switch model.serverState {
        case .listening:
            return model.connected.isEmpty ? "En attente du téléphone" : "Connecté à \(model.connected.map(\.name).joined(separator: ", "))"
        case .failed(let message): return "Erreur réseau : \(message)"
        case .stopped: return "Arrêté"
        }
    }

    private var statusIcon: String {
        switch model.serverState {
        case .listening: return model.connected.isEmpty ? "antenna.radiowaves.left.and.right" : "checkmark.circle"
        case .failed: return "exclamationmark.triangle"
        case .stopped: return "pause.circle"
        }
    }
}

struct PairingView: View {
    @ObservedObject var model: AppModel

    var body: some View {
        VStack(spacing: 16) {
            Text("Scanne ce code avec l'app Rohy sur ton téléphone")
                .font(.headline)
            if let uri = model.pairingURI, let image = QRCode.image(for: uri) {
                Image(nsImage: image)
                    .interpolation(.none)
                    .resizable()
                    .frame(width: 240, height: 240)
                Text("Sans caméra : copie le code et colle-le dans l'app.")
                    .font(.callout).foregroundStyle(.secondary)
                Button {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(uri, forType: .string)
                } label: {
                    Label("Copier le code d'association", systemImage: "doc.on.doc")
                }
                Text("Le code n'est valable qu'une fois, tant que cette fenêtre est ouverte.")
                    .font(.footnote).foregroundStyle(.secondary)
            } else {
                ProgressView()
            }
        }
        .padding(24)
        .frame(width: 360)
        .onAppear { model.openPairing() }
        .onDisappear { model.closePairing() }
    }
}

enum QRCode {
    static func image(for text: String) -> NSImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 8, y: 8)) else { return nil }
        let rep = NSCIImageRep(ciImage: output)
        let image = NSImage(size: rep.size)
        image.addRepresentation(rep)
        return image
    }
}
