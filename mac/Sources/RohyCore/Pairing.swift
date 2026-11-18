import Foundation

/// Content of the QR code shown by the Mac.
public struct PairingPayload: Equatable, Sendable {
    public let publicKey: Data
    public let token: Data
    public let name: String
    public let hosts: [String]
    public let port: UInt16

    public init(publicKey: Data, token: Data, name: String, hosts: [String], port: UInt16) {
        self.publicKey = publicKey
        self.token = token
        self.name = name
        self.hosts = hosts
        self.port = port
    }

    public var uri: String {
        var components = URLComponents()
        components.scheme = "rohy"
        components.host = "pair"
        components.queryItems = [
            URLQueryItem(name: "v", value: String(RohyProtocol.version)),
            URLQueryItem(name: "pk", value: Base64URL.encode(publicKey)),
            URLQueryItem(name: "t", value: Base64URL.encode(token)),
            URLQueryItem(name: "n", value: name),
            URLQueryItem(name: "h", value: hosts.joined(separator: ",")),
            URLQueryItem(name: "p", value: String(port)),
        ]
        return components.string!
    }

    public static func parse(_ uri: String) -> PairingPayload? {
        guard let components = URLComponents(string: uri),
              components.scheme == "rohy", components.host == "pair" else { return nil }
        var query: [String: String] = [:]
        for item in components.queryItems ?? [] { query[item.name] = item.value }
        guard query["v"] == String(RohyProtocol.version),
              let publicKey = query["pk"].flatMap(Base64URL.decode), publicKey.count == 32,
              let token = query["t"].flatMap(Base64URL.decode), token.count == RohyProtocol.tokenLength,
              let name = query["n"], !name.isEmpty,
              let port = query["p"].flatMap(UInt16.init) else { return nil }
        let hosts = (query["h"] ?? "").split(separator: ",").map(String.init)
        return PairingPayload(publicKey: publicKey, token: token, name: name, hosts: hosts, port: port)
    }
}

/// Single-use pairing token, valid while the pairing window is open on the Mac.
public final class PairingWindow {
    public private(set) var token: Data?
    private let makeToken: () -> Data

    public init(makeToken: @escaping () -> Data = { Random.bytes(RohyProtocol.tokenLength) }) {
        self.makeToken = makeToken
    }

    public var isOpen: Bool { token != nil }

    @discardableResult
    public func open() -> Data {
        let token = makeToken()
        self.token = token
        return token
    }

    public func close() { token = nil }

    /// Returns true when the candidate matches. The token is consumed either way.
    public func consume(_ candidate: Data) -> Bool {
        guard let token else { return false }
        self.token = nil
        return constantTimeEquals(token, candidate)
    }
}
