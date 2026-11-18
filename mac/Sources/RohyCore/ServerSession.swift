import CryptoKit
import Foundation

/// What the network layer must do after the session processed an input.
public enum SessionAction: Equatable {
    case sendText(String)
    case sendBinary(Data)
    case close(CloseCode)
    case paired(TrustedDevice)
    case authenticated(TrustedDevice)
    case received(Clip)
}

/// Server side of one phone connection, without any networking (see docs/protocol.md).
public final class ServerSession {
    private enum State {
        case awaitingHello
        case awaitingPair(Channel, Hello)
        case ready(Channel, TrustedDevice)
        case closed
    }

    private struct Channel {
        var sealer: FrameSealer
        var opener: FrameOpener
    }

    private let identity: Curve25519.KeyAgreement.PrivateKey
    private let serverName: String
    private let store: TrustedDeviceStore
    private let pairing: PairingWindow
    private let makeNonce: () -> Data
    private var state: State = .awaitingHello

    public init(
        identity: Curve25519.KeyAgreement.PrivateKey,
        serverName: String,
        store: TrustedDeviceStore,
        pairing: PairingWindow,
        makeNonce: @escaping () -> Data = { Random.bytes(RohyProtocol.nonceLength) }
    ) {
        self.identity = identity
        self.serverName = serverName
        self.store = store
        self.pairing = pairing
        self.makeNonce = makeNonce
    }

    public var device: TrustedDevice? {
        if case .ready(_, let device) = state { return device }
        return nil
    }

    public var isClosed: Bool {
        if case .closed = state { return true }
        return false
    }

    public func receiveText(_ text: String) -> [SessionAction] {
        guard case .awaitingHello = state else { return fail(.invalidFrame) }
        guard let hello = try? JSONDecoder().decode(Hello.self, from: Data(text.utf8)), hello.t == "hello" else {
            return fail(.invalidFrame)
        }
        guard hello.v == RohyProtocol.version else { return fail(.unsupportedVersion) }
        guard let clientKey = Base64URL.decode(hello.pk), clientKey.count == 32,
              let clientNonce = Base64URL.decode(hello.n), clientNonce.count == RohyProtocol.nonceLength else {
            return fail(.invalidFrame)
        }

        let known = store.find(publicKey: clientKey)
        guard known != nil || pairing.isOpen else { return fail(.forbidden) }

        let serverNonce = makeNonce()
        guard let keys = try? SessionKeys.derive(
            privateKey: identity, peerPublicKey: clientKey, clientNonce: clientNonce, serverNonce: serverNonce
        ) else { return fail(.invalidFrame) }
        let channel = Channel(sealer: FrameSealer(key: keys.serverToClient), opener: FrameOpener(key: keys.clientToServer))

        let welcome = Welcome(
            pk: Base64URL.encode(identity.publicKey.rawRepresentation),
            n: Base64URL.encode(serverNonce),
            status: known == nil ? .pairing : .trusted
        )
        let reply = String(decoding: try! JSONEncoder.rohy.encode(welcome), as: UTF8.self)

        if let known {
            state = .ready(channel, known)
            return [.sendText(reply), .authenticated(known)]
        }
        state = .awaitingPair(channel, hello)
        return [.sendText(reply)]
    }

    public func receiveBinary(_ frame: Data) -> [SessionAction] {
        switch state {
        case .awaitingHello, .closed:
            return fail(.invalidFrame)
        case .awaitingPair(var channel, let hello):
            guard let message = open(frame, with: &channel) else { return fail(.invalidFrame) }
            guard case .pair(let token) = message, pairing.consume(token),
                  let publicKey = Base64URL.decode(hello.pk) else { return fail(.forbidden) }
            let device = TrustedDevice(publicKey: publicKey, name: hello.name)
            store.save(device)
            state = .ready(channel, device)
            return [.paired(device)] + send(.paired(name: serverName))
        case .ready(var channel, let device):
            guard let message = open(frame, with: &channel) else { return fail(.invalidFrame) }
            state = .ready(channel, device)
            switch message {
            case .clip(let clip): return [.received(clip)]
            case .ping: return send(.pong)
            case .pong: return []
            case .pair, .paired: return fail(.invalidFrame)
            }
        }
    }

    /// Encrypts a message for the phone. Returns nothing when the session is not authenticated.
    public func send(_ message: SecureMessage) -> [SessionAction] {
        guard case .ready(var channel, let device) = state,
              let frame = try? channel.sealer.seal(message.encoded()) else { return [] }
        state = .ready(channel, device)
        return [.sendBinary(frame)]
    }

    private func open(_ frame: Data, with channel: inout Channel) -> SecureMessage? {
        guard let plaintext = try? channel.opener.open(frame) else { return nil }
        return SecureMessage.decode(plaintext)
    }

    private func fail(_ code: CloseCode) -> [SessionAction] {
        state = .closed
        return [.close(code)]
    }
}
