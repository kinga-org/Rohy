import CryptoKit
import Foundation
import RohyCore
import RohyServer

// Interoperability harness: runs the real server on a random local port so the Kotlin client
// can be tested against it (scripts/check-interop.sh). Protocol on stdio:
//   stdout: "READY <pairing uri>", "PAIRED <name>", "RECEIVED <text>"
//   stdin:  "SEND <text>"
setvbuf(stdout, nil, _IOLBF, 0)

final class Harness: SyncServerDelegate {
    let pairing = PairingWindow()
    let identity = Curve25519.KeyAgreement.PrivateKey()
    lazy var server = SyncServer(identity: identity, name: "Rohy harness", store: InMemoryTrustedDeviceStore(),
                                 pairing: pairing, port: 0, advertise: false)

    func syncServer(_ server: SyncServer, didChangeState state: SyncServer.State) {
        switch state {
        case .listening(let port):
            let payload = PairingPayload(publicKey: identity.publicKey.rawRepresentation, token: pairing.open(),
                                         name: "Rohy harness", hosts: ["127.0.0.1"], port: port)
            print("READY \(payload.uri)")
        case .failed(let message):
            print("FAILED \(message)")
            exit(1)
        case .stopped:
            break
        }
    }

    func syncServer(_ server: SyncServer, didPair device: TrustedDevice) { print("PAIRED \(device.name)") }

    func syncServer(_ server: SyncServer, didReceive clip: Clip, from device: TrustedDevice) {
        print("RECEIVED \(clip.text)")
    }

    func syncServerConnectionsDidChange(_ server: SyncServer) {}
}

let harness = Harness()
harness.server.delegate = harness
try harness.server.start()

Thread.detachNewThread {
    while let line = readLine() {
        guard line.hasPrefix("SEND ") else { continue }
        let text = String(line.dropFirst(5))
        DispatchQueue.main.async { harness.server.broadcast(Clip(text: text)) }
    }
    exit(0)
}
dispatchMain()
