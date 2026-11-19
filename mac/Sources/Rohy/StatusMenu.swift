import AppKit
import Combine
import RohyCore
import SwiftUI

/// The Rohy menu in the menu bar, built only from standard `NSMenuItem`s (titles, subtitles, SF Symbols,
/// section headers, submenus): its look, highlight and behavior are the system's own
/// (canvas "Rohy macOS — natif"). SwiftUI's `.window` MenuBarExtra lost its rounded corners whenever its
/// content changed size.
@MainActor
final class StatusMenu: NSObject, NSMenuDelegate {
    private let model: AppModel
    private let pairing: PairingWindowController
    private let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private let menu = NSMenu()
    private let status = NSMenuItem()
    private let failure = NSMenuItem()
    private var changes: AnyCancellable?
    private var clock: Timer?
    private var shown: [String]?

    init(model: AppModel) {
        self.model = model
        pairing = PairingWindowController(model: model)
        super.init()
        menu.autoenablesItems = false
        menu.delegate = self
        item.menu = menu
        status.isEnabled = false
        failure.isEnabled = false
        failure.image = Self.symbol("exclamationmark.triangle.fill", colors: [.systemRed])
        refresh()
        changes = model.objectWillChange.sink { [weak self] _ in
            // objectWillChange fires before the new value is stored: read it on the next turn.
            Task { @MainActor in self?.refresh() }
        }
    }

    // Relative times ("il y a 12 s") move on while the menu is open.

    func menuWillOpen(_ menu: NSMenu) {
        refresh()
        let clock = Timer(timeInterval: 5, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.updateStatus() }
        }
        RunLoop.main.add(clock, forMode: .common)
        self.clock = clock
    }

    func menuDidClose(_ menu: NSMenu) {
        clock?.invalidate()
        clock = nil
    }

    /// Updates the status line in place, and rebuilds the other items only when what they show changed,
    /// so an open submenu is not closed for nothing.
    private func refresh() {
        let connected = Set(model.connected.map(\.id))
        item.button?.image = NSImage(systemSymbolName: connected.isEmpty ? "link" : "link.circle.fill",
                                     accessibilityDescription: "Rohy")
        updateStatus()
        let signature = model.trusted.map { "\($0.id):\($0.name):\(connected.contains($0.id))" }
            + ["port:\(model.port.map(String.init) ?? "-")", "error:\(model.startupError ?? "")"]
        guard signature != shown else { return }
        shown = signature

        menu.removeAllItems()
        menu.addItem(.sectionHeader(title: "Rohy"))
        menu.addItem(status)
        if let error = model.startupError {
            failure.title = error
            menu.addItem(failure)
        }

        menu.addItem(.separator())
        menu.addItem(.sectionHeader(title: "Téléphones associés"))
        if model.trusted.isEmpty {
            menu.addItem(info("Aucun téléphone associé"))
        }
        for device in model.trusted {
            menu.addItem(phoneItem(device, connected: connected.contains(device.id)))
        }
        menu.addItem(action("Associer un téléphone…", symbol: "qrcode", key: "n", selector: #selector(openPairing)))

        menu.addItem(.separator())
        menu.addItem(.sectionHeader(title: "Ce Mac"))
        let mac = info(model.macName, subtitle: model.port.map { "Visible sur le réseau local · port \($0)" }
            ?? "Visible sur le réseau local")
        mac.image = NSImage(systemSymbolName: "laptopcomputer", accessibilityDescription: nil)
        menu.addItem(mac)

        menu.addItem(.separator())
        let quit = NSMenuItem(title: "Quitter Rohy", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        quit.image = NSImage(systemSymbolName: "power", accessibilityDescription: nil)
        menu.addItem(quit)
    }

    /// Status dot, header title and last activity, from the texts shared with the Windows/Linux app.
    private func updateStatus() {
        var failure: String?
        if case .failed(let message) = model.serverState { failure = message }
        let header = DesktopPresentation.header(
            listening: model.port != nil, failure: failure,
            connected: model.connected.map(\.name), activity: model.lastActivity, now: Date()
        )
        let detail: String?
        switch header {
        case .connected(_, let activity):
            detail = activity
            status.image = Self.symbol("circle.fill", colors: [.systemGreen], size: 9)
        case .failed:
            detail = header.subtitle
            status.image = Self.symbol("circle.fill", colors: [.systemRed], size: 9)
        case .starting, .waiting:
            detail = header.subtitle
            status.image = Self.symbol("circle", colors: [.secondaryLabelColor], size: 9)
        }
        setText(status, title: header.title, subtitle: detail)
    }

    private func phoneItem(_ device: TrustedDevice, connected: Bool) -> NSMenuItem {
        let item = NSMenuItem(title: device.name, action: nil, keyEquivalent: "")
        setText(item, title: device.name,
                subtitle: DesktopPresentation.deviceDetail(connected: connected, pairedAt: device.pairedAt))
        item.image = Self.rowIcon("iphone.circle.fill", colors: connected
            ? [.white, .controlAccentColor] : [.labelColor, .quaternaryLabelColor])

        let submenu = NSMenu()
        submenu.autoenablesItems = false
        submenu.addItem(info(connected ? "Connecté" : "Hors ligne"))
        submenu.addItem(info("Associé le \(DesktopPresentation.shortDate(device.pairedAt))"))
        submenu.addItem(.separator())
        let forget = action("Oublier ce téléphone…", symbol: "trash", key: "", selector: #selector(forget(_:)))
        forget.representedObject = device.id
        submenu.addItem(forget)
        item.submenu = submenu
        return item
    }

    private func info(_ title: String, subtitle: String? = nil) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: nil, keyEquivalent: "")
        setText(item, title: title, subtitle: subtitle)
        item.isEnabled = false
        return item
    }

    private func action(_ title: String, symbol: String, key: String, selector: Selector) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: selector, keyEquivalent: key)
        item.target = self
        item.image = NSImage(systemSymbolName: symbol, accessibilityDescription: nil)
        return item
    }

    /// Second line under the title (macOS 14.4+); on 14.0–14.3, one line "title — subtitle".
    private func setText(_ item: NSMenuItem, title: String, subtitle: String?) {
        if #available(macOS 14.4, *) {
            item.title = title
            item.subtitle = subtitle
        } else {
            item.title = subtitle.map { "\(title) — \($0)" } ?? title
        }
    }

    /// A system symbol at the height of a row with a subtitle (28 pt). Menus draw symbol images at text
    /// size whatever their configuration, so the symbol is drawn into a plain image, on demand: it still
    /// follows the accent color and the light/dark mode.
    private static func rowIcon(_ name: String, colors: [NSColor]) -> NSImage {
        let side: CGFloat = 28
        let image = NSImage(size: NSSize(width: side, height: side), flipped: false) { rect in
            guard let glyph = symbol(name, colors: colors, size: side) else { return false }
            let size = glyph.size
            let scale = min(rect.width / size.width, rect.height / size.height)
            let drawn = NSSize(width: size.width * scale, height: size.height * scale)
            glyph.draw(in: NSRect(x: rect.midX - drawn.width / 2, y: rect.midY - drawn.height / 2,
                                  width: drawn.width, height: drawn.height))
            return true
        }
        image.accessibilityDescription = nil
        return image
    }

    private static func symbol(_ name: String, colors: [NSColor], size: CGFloat = 13) -> NSImage? {
        let config = NSImage.SymbolConfiguration(pointSize: size, weight: .regular)
            .applying(.init(paletteColors: colors))
        return NSImage(systemSymbolName: name, accessibilityDescription: nil)?.withSymbolConfiguration(config)
    }

    @objc private func openPairing() { pairing.show() }

    /// Same confirmation as the Android app before forgetting.
    @objc private func forget(_ sender: NSMenuItem) {
        guard let id = sender.representedObject as? String,
              let device = model.trusted.first(where: { $0.id == id }) else { return }
        let alert = NSAlert()
        alert.messageText = "Oublier \(device.name) ?"
        alert.informativeText = "Il faudra scanner à nouveau le QR code de ce Mac pour le réassocier."
        alert.addButton(withTitle: "Oublier").hasDestructiveAction = true
        alert.addButton(withTitle: "Annuler")
        NSApp.activate(ignoringOtherApps: true)
        if alert.runModal() == .alertFirstButtonReturn { model.remove(device) }
    }
}

/// "Associer un téléphone" window: opens the single-use pairing token while it is shown.
@MainActor
final class PairingWindowController: NSObject, NSWindowDelegate {
    private let model: AppModel
    private var window: NSWindow?

    init(model: AppModel) {
        self.model = model
    }

    func show() {
        if window == nil {
            let window = NSWindow(contentRect: .zero, styleMask: [.titled, .closable, .miniaturizable],
                                  backing: .buffered, defer: false)
            window.title = "Associer un téléphone"
            window.isReleasedWhenClosed = false
            window.delegate = self
            window.contentViewController = NSHostingController(
                rootView: PairingView(model: model) { [weak window] in window?.close() }
            )
            window.center()
            self.window = window
            model.openPairing()
        }
        NSApp.activate(ignoringOtherApps: true)
        window?.makeKeyAndOrderFront(nil)
    }

    func windowWillClose(_ notification: Notification) {
        model.closePairing()
        window = nil
    }
}
