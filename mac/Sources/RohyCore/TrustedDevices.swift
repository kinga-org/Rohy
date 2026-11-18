import Foundation

/// A phone allowed to connect. Only its public key is stored: it is not a secret.
public struct TrustedDevice: Codable, Equatable, Sendable, Identifiable {
    public let publicKey: Data
    public var name: String
    public let pairedAt: Date

    public var id: String { Fingerprint.of(publicKey: publicKey) }

    public init(publicKey: Data, name: String, pairedAt: Date = Date()) {
        self.publicKey = publicKey
        self.name = name
        self.pairedAt = pairedAt
    }
}

public protocol TrustedDeviceStore: AnyObject {
    func all() -> [TrustedDevice]
    func find(publicKey: Data) -> TrustedDevice?
    func save(_ device: TrustedDevice)
    func remove(id: String)
}

public final class InMemoryTrustedDeviceStore: TrustedDeviceStore {
    private var devices: [TrustedDevice]

    public init(devices: [TrustedDevice] = []) { self.devices = devices }

    public func all() -> [TrustedDevice] { devices }

    public func find(publicKey: Data) -> TrustedDevice? {
        devices.first { $0.publicKey == publicKey }
    }

    public func save(_ device: TrustedDevice) {
        devices.removeAll { $0.publicKey == device.publicKey }
        devices.append(device)
    }

    public func remove(id: String) { devices.removeAll { $0.id == id } }
}

/// Persists trusted devices as JSON in a file (Application Support in the app).
public final class FileTrustedDeviceStore: TrustedDeviceStore {
    private let url: URL
    private var cache: [TrustedDevice]

    public init(url: URL) {
        self.url = url
        if let data = try? Data(contentsOf: url),
           let devices = try? JSONDecoder.rohyDates.decode([TrustedDevice].self, from: data) {
            cache = devices
        } else {
            cache = []
        }
    }

    public func all() -> [TrustedDevice] { cache }

    public func find(publicKey: Data) -> TrustedDevice? {
        cache.first { $0.publicKey == publicKey }
    }

    public func save(_ device: TrustedDevice) {
        cache.removeAll { $0.publicKey == device.publicKey }
        cache.append(device)
        persist()
    }

    public func remove(id: String) {
        cache.removeAll { $0.id == id }
        persist()
    }

    private func persist() {
        let encoder = JSONEncoder.rohy
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(cache) else { return }
        try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try? data.write(to: url, options: .atomic)
    }
}

extension JSONDecoder {
    static var rohyDates: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}
