import CryptoKit
import Foundation
import Security

/// Keeps the Mac's X25519 private key in the login Keychain. It never leaves the Mac.
enum IdentityStore {
    private static let service = "app.rohy.mac.identity"
    private static let account = "x25519"

    static func loadOrCreate() throws -> Curve25519.KeyAgreement.PrivateKey {
        if let data = read() { return try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: data) }
        let key = Curve25519.KeyAgreement.PrivateKey()
        try write(key.rawRepresentation)
        return key
    }

    private static func read() -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess else { return nil }
        return result as? Data
    }

    private static func write(_ data: Data) throws {
        let attributes: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: data,
        ]
        let status = SecItemAdd(attributes as CFDictionary, nil)
        guard status == errSecSuccess else { throw NSError(domain: NSOSStatusErrorDomain, code: Int(status)) }
    }
}
