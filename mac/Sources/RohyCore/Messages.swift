import Foundation

/// Plaintext handshake message sent by the phone.
public struct Hello: Codable, Equatable, Sendable {
    public var t = "hello"
    public let v: Int
    public let pk: String
    public let n: String
    public let name: String

    public init(v: Int = RohyProtocol.version, pk: String, n: String, name: String) {
        self.v = v
        self.pk = pk
        self.n = n
        self.name = name
    }
}

/// Plaintext handshake answer sent by the Mac.
public struct Welcome: Codable, Equatable, Sendable {
    public enum Status: String, Codable, Sendable {
        case trusted
        case pairing
    }

    public var t = "welcome"
    public let v: Int
    public let pk: String
    public let n: String
    public let status: Status

    public init(v: Int = RohyProtocol.version, pk: String, n: String, status: Status) {
        self.v = v
        self.pk = pk
        self.n = n
        self.status = status
    }
}

public struct Clip: Codable, Equatable, Sendable {
    public let id: String
    public let text: String
    public let sensitive: Bool

    public init(id: String = UUID().uuidString.lowercased(), text: String, sensitive: Bool = false) {
        self.id = id
        self.text = text
        self.sensitive = sensitive
    }
}

/// Messages carried inside encrypted frames.
public enum SecureMessage: Equatable, Sendable {
    case pair(token: Data)
    case paired(name: String)
    case clip(Clip)
    case ping
    case pong

    private struct Envelope: Codable {
        let t: String
        var token: String?
        var name: String?
        var id: String?
        var text: String?
        var sensitive: Bool?
    }

    public func encoded() -> Data {
        let envelope: Envelope
        switch self {
        case .pair(let token): envelope = Envelope(t: "pair", token: Base64URL.encode(token))
        case .paired(let name): envelope = Envelope(t: "paired", name: name)
        case .clip(let clip): envelope = Envelope(t: "clip", id: clip.id, text: clip.text, sensitive: clip.sensitive)
        case .ping: envelope = Envelope(t: "ping")
        case .pong: envelope = Envelope(t: "pong")
        }
        // Encoding a struct of strings and booleans cannot fail.
        return try! JSONEncoder.rohy.encode(envelope)
    }

    public static func decode(_ data: Data) -> SecureMessage? {
        guard let envelope = try? JSONDecoder().decode(Envelope.self, from: data) else { return nil }
        switch envelope.t {
        case "pair":
            guard let token = envelope.token.flatMap(Base64URL.decode) else { return nil }
            return .pair(token: token)
        case "paired":
            guard let name = envelope.name else { return nil }
            return .paired(name: name)
        case "clip":
            guard let id = envelope.id, let text = envelope.text else { return nil }
            return .clip(Clip(id: id, text: text, sensitive: envelope.sensitive ?? false))
        case "ping": return .ping
        case "pong": return .pong
        default: return nil
        }
    }
}

extension JSONEncoder {
    static var rohy: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]
        return encoder
    }
}
