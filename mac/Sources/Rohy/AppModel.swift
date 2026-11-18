import AppKit
import CryptoKit
import Foundation
import RohyCore
import RohyServer
import UserNotifications

@MainActor
final class AppModel: ObservableObject {
    @Published private(set) var serverState: SyncServer.State = .stopped
    @Published private(set) var connected: [TrustedDevice] = []
    @Published private(set) var trusted: [TrustedDevice] = []
    @Published private(set) var pairingURI: String?
    @Published private(set) var lastEvent: String?
    @Published private(set) var startupError: String?
    /// Incremented on each successful pairing, so the pairing window can close itself.
    @Published private(set) var pairingsCompleted = 0

    private let store: FileTrustedDeviceStore
    private let pairing = PairingWindow()
    private let watcher = PasteboardWatcher()
    private var identity: Curve25519.KeyAgreement.PrivateKey?
    private var server: SyncServer?
    private let macName = Host.current().localizedName ?? "Mac"

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        store = FileTrustedDeviceStore(url: support.appendingPathComponent("Rohy/devices.json"))
        trusted = store.all()
        start()
    }

    private func start() {
        do {
            let identity = try IdentityStore.loadOrCreate()
            let server = SyncServer(identity: identity, name: macName, store: store, pairing: pairing)
            server.delegate = self
            try server.start()
            self.identity = identity
            self.server = server
        } catch {
            startupError = "Démarrage impossible : \(error.localizedDescription)"
            return
        }
        watcher.onCopy = { [weak self] text in self?.server?.broadcast(Clip(text: text)) }
        watcher.start()
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert]) { _, _ in }
    }

    private var pairingRequested = false

    func openPairing() {
        pairingRequested = true
        guard let identity, case .listening(let port) = serverState else { return }
        let token = pairing.open()
        pairingURI = PairingPayload(
            publicKey: identity.publicKey.rawRepresentation, token: token, name: macName,
            hosts: NetworkInterfaces.ipv4Addresses(), port: port
        ).uri
    }

    func closePairing() {
        pairingRequested = false
        pairing.close()
        pairingURI = nil
    }

    func remove(_ device: TrustedDevice) {
        store.remove(id: device.id)
        server?.disconnect(deviceID: device.id)
        trusted = store.all()
    }

    private func notify(_ title: String, _ body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil))
    }
}

extension AppModel: SyncServerDelegate {
    nonisolated func syncServer(_ server: SyncServer, didChangeState state: SyncServer.State) {
        MainActor.assumeIsolated {
            serverState = state
            if pairingRequested, pairingURI == nil { openPairing() }
        }
    }

    nonisolated func syncServer(_ server: SyncServer, didPair device: TrustedDevice) {
        MainActor.assumeIsolated {
            trusted = store.all()
            closePairing()
            pairingsCompleted += 1
            lastEvent = "\(device.name) associé"
        }
    }

    nonisolated func syncServer(_ server: SyncServer, didReceive clip: Clip, from device: TrustedDevice) {
        MainActor.assumeIsolated {
            watcher.write(clip)
            lastEvent = "Reçu de \(device.name)"
            notify("Reçu de \(device.name)", clip.sensitive ? "Contenu masqué" : String(clip.text.prefix(80)))
        }
    }

    nonisolated func syncServerConnectionsDidChange(_ server: SyncServer) {
        MainActor.assumeIsolated { connected = server.connectedDevices }
    }
}
