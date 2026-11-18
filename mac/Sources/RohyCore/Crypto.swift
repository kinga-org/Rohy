import CryptoKit
import Foundation

public enum RohyProtocol {
    public static let version = 1
    public static let defaultPort: UInt16 = 47823
    public static let bonjourType = "_rohy._tcp"
    public static let path = "/rohy"
    public static let nonceLength = 16
    public static let tokenLength = 32
    public static let maxTextBytes = 1 << 20
    static let aad = Data("rohy/v1".utf8)
    static let infoClientToServer = Data("rohy/v1 c2s".utf8)
    static let infoServerToClient = Data("rohy/v1 s2c".utf8)
}

public enum CloseCode: UInt16, Sendable {
    case invalidFrame = 4400
    case forbidden = 4403
    case unsupportedVersion = 4426
}

/// Per-direction session keys derived from the static X25519 keys and both session nonces.
public struct SessionKeys: Sendable {
    public let clientToServer: SymmetricKey
    public let serverToClient: SymmetricKey

    public static func derive(
        privateKey: Curve25519.KeyAgreement.PrivateKey,
        peerPublicKey: Data,
        clientNonce: Data,
        serverNonce: Data
    ) throws -> SessionKeys {
        let peer = try Curve25519.KeyAgreement.PublicKey(rawRepresentation: peerPublicKey)
        let shared = try privateKey.sharedSecretFromKeyAgreement(with: peer)
        let salt = clientNonce + serverNonce
        func derive(_ info: Data) -> SymmetricKey {
            shared.hkdfDerivedSymmetricKey(using: SHA256.self, salt: salt, sharedInfo: info, outputByteCount: 32)
        }
        return SessionKeys(
            clientToServer: derive(RohyProtocol.infoClientToServer),
            serverToClient: derive(RohyProtocol.infoServerToClient)
        )
    }
}

public enum FrameError: Error, Equatable {
    case tooShort
    case unexpectedCounter
    case authenticationFailed
    case counterExhausted
}

/// Seals outgoing frames with a strictly increasing counter nonce.
public struct FrameSealer {
    private let key: SymmetricKey
    public private(set) var counter: UInt64 = 0

    public init(key: SymmetricKey) { self.key = key }

    public mutating func seal(_ plaintext: Data) throws -> Data {
        guard counter < UInt64.max else { throw FrameError.counterExhausted }
        let nonce = try ChaChaPoly.Nonce(data: Self.nonce(for: counter))
        let box = try ChaChaPoly.seal(plaintext, using: key, nonce: nonce, authenticating: RohyProtocol.aad)
        counter += 1
        return box.combined
    }

    static func nonce(for counter: UInt64) -> Data {
        var bigEndian = counter.bigEndian
        return Data(count: 4) + Data(bytes: &bigEndian, count: 8)
    }
}

/// Opens incoming frames and rejects any frame whose counter is not the next expected one.
public struct FrameOpener {
    private let key: SymmetricKey
    public private(set) var expectedCounter: UInt64 = 0

    public init(key: SymmetricKey) { self.key = key }

    public mutating func open(_ frame: Data) throws -> Data {
        guard frame.count >= 12 + 16 else { throw FrameError.tooShort }
        let nonce = frame.prefix(12)
        guard nonce == FrameSealer.nonce(for: expectedCounter) else { throw FrameError.unexpectedCounter }
        do {
            let box = try ChaChaPoly.SealedBox(combined: frame)
            let plaintext = try ChaChaPoly.open(box, using: key, authenticating: RohyProtocol.aad)
            expectedCounter += 1
            return plaintext
        } catch {
            throw FrameError.authenticationFailed
        }
    }
}

public enum Random {
    public static func bytes(_ count: Int) -> Data {
        var generator = SystemRandomNumberGenerator()
        return Data((0..<count).map { _ in UInt8.random(in: .min ... .max, using: &generator) })
    }
}

/// Constant-time comparison for secrets such as the pairing token.
public func constantTimeEquals(_ lhs: Data, _ rhs: Data) -> Bool {
    guard lhs.count == rhs.count else { return false }
    var difference: UInt8 = 0
    for (a, b) in zip(lhs, rhs) { difference |= a ^ b }
    return difference == 0
}
