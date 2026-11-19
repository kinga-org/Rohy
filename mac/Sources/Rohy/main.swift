import AppKit

// The process starts on the main thread: run the AppKit setup on the main actor.
MainActor.assumeIsolated {
    // `Rohy --render-previews <dir>`: PNG renders of the pairing window, light and dark, then quit.
    if let index = CommandLine.arguments.firstIndex(of: "--render-previews") {
        let dir = CommandLine.arguments.dropFirst(index + 1).first ?? "previews"
        Previews.render(to: URL(fileURLWithPath: dir))
        exit(0)
    }

    let app = NSApplication.shared
    let delegate = AppDelegate()
    app.delegate = delegate
    app.setActivationPolicy(.accessory)
    withExtendedLifetime(delegate) { app.run() }
}
