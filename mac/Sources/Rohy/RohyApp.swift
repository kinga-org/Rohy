import AppKit
import CoreImage.CIFilterBuiltins
import RohyCore
import SwiftUI

/// Starts Rohy as a menu bar app (LSUIElement). Pure AppKit at the top: the menu is an `NSMenu` and the
/// pairing window an `NSWindow`; SwiftUI only draws the pairing window's content. Without a SwiftUI `App`,
/// no scene can open an empty window of its own (one did after the pairing window closed).
@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusMenu: StatusMenu?

    func applicationDidFinishLaunching(_ notification: Notification) {
        statusMenu = StatusMenu(model: AppModel())
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }
}

/// Content of the "Associer un téléphone" window; closes itself once a phone is paired.
struct PairingView: View {
    @ObservedObject var model: AppModel
    let close: () -> Void
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
            onDone: close
        )
        .onChange(of: model.pairingsCompleted) { close() }
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
