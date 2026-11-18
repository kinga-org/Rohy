import CryptoKit
import Foundation
import Network
import RohyCore

public protocol SyncServerDelegate: AnyObject {
    func syncServer(_ server: SyncServer, didPair device: TrustedDevice)
    func syncServer(_ server: SyncServer, didReceive clip: Clip, from device: TrustedDevice)
    func syncServerConnectionsDidChange(_ server: SyncServer)
    func syncServer(_ server: SyncServer, didChangeState state: SyncServer.State)
}

/// WebSocket server on the local network, advertised over Bonjour (see docs/protocol.md).
/// All callbacks and state changes happen on `queue`.
public final class SyncServer {
    public enum State: Equatable {
        case stopped
        case listening(port: UInt16)
        case failed(String)
    }

    public weak var delegate: SyncServerDelegate?
    public private(set) var state: State = .stopped

    private let identity: Curve25519.KeyAgreement.PrivateKey
    private let name: String
    private let store: TrustedDeviceStore
    private let pairing: PairingWindow
    private let requestedPort: UInt16
    private let advertise: Bool
    private let queue: DispatchQueue
    private var listener: NWListener?
    private var connections: [ObjectIdentifier: PhoneConnection] = [:]

    public init(
        identity: Curve25519.KeyAgreement.PrivateKey,
        name: String,
        store: TrustedDeviceStore,
        pairing: PairingWindow,
        port: UInt16 = RohyProtocol.defaultPort,
        advertise: Bool = true,
        queue: DispatchQueue = .main
    ) {
        self.identity = identity
        self.name = name
        self.store = store
        self.pairing = pairing
        self.requestedPort = port
        self.advertise = advertise
        self.queue = queue
    }

    public var connectedDevices: [TrustedDevice] {
        connections.values.compactMap { $0.session.device }
    }

    public func start() throws {
        let webSocket = NWProtocolWebSocket.Options()
        webSocket.autoReplyPing = true
        webSocket.maximumMessageSize = 2 * RohyProtocol.maxTextBytes
        let parameters = NWParameters.tcp
        parameters.defaultProtocolStack.applicationProtocols.insert(webSocket, at: 0)
        parameters.allowLocalEndpointReuse = true

        let listener = try NWListener(using: parameters, on: NWEndpoint.Port(rawValue: requestedPort) ?? .any)
        if advertise {
            let txt = NWTXTRecord(["fp": Fingerprint.of(publicKey: identity.publicKey.rawRepresentation)])
            listener.service = NWListener.Service(name: name, type: RohyProtocol.bonjourType, domain: nil, txtRecord: txt)
        }
        listener.stateUpdateHandler = { [weak self] state in self?.listenerStateChanged(state) }
        listener.newConnectionHandler = { [weak self] connection in self?.accept(connection) }
        self.listener = listener
        listener.start(queue: queue)
    }

    public func stop() {
        listener?.cancel()
        listener = nil
        connections.values.forEach { $0.cancel() }
        connections.removeAll()
        update(.stopped)
    }

    /// Sends a clip to every authenticated phone.
    public func broadcast(_ clip: Clip) {
        connections.values.forEach { $0.deliver(.clip(clip)) }
    }

    /// Closes connections of a device that is no longer trusted.
    public func disconnect(deviceID: String) {
        connections.values.filter { $0.session.device?.id == deviceID }.forEach { $0.cancel() }
    }

    private func listenerStateChanged(_ newState: NWListener.State) {
        switch newState {
        case .ready:
            update(.listening(port: listener?.port?.rawValue ?? requestedPort))
        case .failed(let error):
            update(.failed(error.localizedDescription))
        case .cancelled:
            update(.stopped)
        default:
            break
        }
    }

    private func update(_ newState: State) {
        guard newState != state else { return }
        state = newState
        delegate?.syncServer(self, didChangeState: newState)
    }

    private func accept(_ connection: NWConnection) {
        let session = ServerSession(identity: identity, serverName: name, store: store, pairing: pairing)
        let phone = PhoneConnection(connection: connection, session: session, queue: queue)
        let id = ObjectIdentifier(phone)
        phone.onAction = { [weak self, weak phone] action in
            guard let self, let phone else { return }
            switch action {
            case .paired(let device):
                self.delegate?.syncServer(self, didPair: device)
                self.delegate?.syncServerConnectionsDidChange(self)
            case .authenticated:
                self.delegate?.syncServerConnectionsDidChange(self)
            case .received(let clip):
                if let device = phone.session.device { self.delegate?.syncServer(self, didReceive: clip, from: device) }
            default:
                break
            }
        }
        phone.onEnd = { [weak self] in
            guard let self else { return }
            self.connections.removeValue(forKey: id)
            self.delegate?.syncServerConnectionsDidChange(self)
        }
        connections[id] = phone
        phone.start()
    }
}

/// One WebSocket connection with its protocol session.
final class PhoneConnection {
    let session: ServerSession
    var onAction: ((SessionAction) -> Void)?
    var onEnd: (() -> Void)?

    private let connection: NWConnection
    private let queue: DispatchQueue
    private var ended = false

    init(connection: NWConnection, session: ServerSession, queue: DispatchQueue) {
        self.connection = connection
        self.session = session
        self.queue = queue
    }

    func start() {
        connection.stateUpdateHandler = { [weak self] state in
            switch state {
            case .failed, .cancelled: self?.end()
            default: break
            }
        }
        connection.start(queue: queue)
        receive()
    }

    func deliver(_ message: SecureMessage) {
        perform(session.send(message))
    }

    func cancel() {
        connection.cancel()
        end()
    }

    private func receive() {
        connection.receiveMessage { [weak self] content, context, _, error in
            guard let self, !self.ended else { return }
            if error != nil { return self.cancel() }
            let metadata = context?.protocolMetadata(definition: NWProtocolWebSocket.definition) as? NWProtocolWebSocket.Metadata
            switch metadata?.opcode {
            case .text?:
                self.perform(self.session.receiveText(String(decoding: content ?? Data(), as: UTF8.self)))
            case .binary?:
                self.perform(self.session.receiveBinary(content ?? Data()))
            case .close?:
                return self.cancel()
            default:
                break
            }
            if !self.ended { self.receive() }
        }
    }

    private func perform(_ actions: [SessionAction]) {
        for action in actions {
            switch action {
            case .sendText(let text): send(Data(text.utf8), opcode: .text)
            case .sendBinary(let data): send(data, opcode: .binary)
            case .close(let code): close(code)
            default: onAction?(action)
            }
        }
    }

    private func send(_ data: Data, opcode: NWProtocolWebSocket.Opcode) {
        let metadata = NWProtocolWebSocket.Metadata(opcode: opcode)
        let context = NWConnection.ContentContext(identifier: "rohy", metadata: [metadata])
        connection.send(content: data, contentContext: context, isComplete: true, completion: .contentProcessed { _ in })
    }

    private func close(_ code: CloseCode) {
        let metadata = NWProtocolWebSocket.Metadata(opcode: .close)
        metadata.closeCode = .privateCode(code.rawValue)
        let context = NWConnection.ContentContext(identifier: "close", metadata: [metadata])
        connection.send(content: nil, contentContext: context, isComplete: true, completion: .contentProcessed { [weak self] _ in
            self?.cancel()
        })
    }

    private func end() {
        guard !ended else { return }
        ended = true
        onEnd?()
    }
}
