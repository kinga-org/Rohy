import AppKit
import RohyCore

/// Polls NSPasteboard.changeCount (macOS has no change notification) and reports text copied by other apps.
final class PasteboardWatcher {
    private let pasteboard: NSPasteboard
    private var echoGuard: EchoGuard
    private var timer: Timer?
    var onCopy: ((String) -> Void)?

    init(pasteboard: NSPasteboard = .general) {
        self.pasteboard = pasteboard
        self.echoGuard = EchoGuard(currentChangeCount: pasteboard.changeCount)
    }

    func start() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in self?.poll() }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
    }

    /// Writes text received from the phone without sending it back.
    func write(_ clip: Clip) {
        pasteboard.clearContents()
        var types: [NSPasteboard.PasteboardType] = [.string]
        if clip.sensitive { types.append(NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType")) }
        pasteboard.declareTypes(types, owner: nil)
        pasteboard.setString(clip.text, forType: .string)
        if clip.sensitive { pasteboard.setString("", forType: NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType")) }
        echoGuard.markWritten(changeCount: pasteboard.changeCount)
    }

    private func poll() {
        guard echoGuard.isExternalChange(changeCount: pasteboard.changeCount) else { return }
        let types = pasteboard.types?.map(\.rawValue) ?? []
        // Check the markers before reading the content.
        guard ClipboardPolicy.secretMarkers.isDisjoint(with: types) else { return }
        let text = pasteboard.string(forType: .string)
        guard ClipboardPolicy.shouldSync(types: types, text: text), let text else { return }
        onCopy?(text)
    }
}
