import CryptoKit
import Foundation
import XCTest
@testable import RohyCore

struct Vectors: Decodable {
    struct Frame: Decodable {
        let direction: String
        let counter: UInt64
        let plaintext: String
        let frame: String
    }

    let client_private: String
    let client_public: String
    let server_private: String
    let server_public: String
    let server_fingerprint: String
    let client_nonce: String
    let server_nonce: String
    let key_c2s: String
    let key_s2c: String
    let frames: [Frame]

    static func load() throws -> Vectors {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("protocol/test-vectors.json")
        return try JSONDecoder().decode(Vectors.self, from: Data(contentsOf: url))
    }

    func data(_ hex: String) -> Data { Hex.decode(hex)! }
}

func raw(_ key: SymmetricKey) -> Data { key.withUnsafeBytes { Data($0) } }

final class VectorTests: XCTestCase {
    func testKeysAndFingerprintMatchVectors() throws {
        let v = try Vectors.load()
        let client = try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: v.data(v.client_private))
        let server = try Curve25519.KeyAgreement.PrivateKey(rawRepresentation: v.data(v.server_private))
        XCTAssertEqual(Hex.encode(client.publicKey.rawRepresentation), v.client_public)
        XCTAssertEqual(Hex.encode(server.publicKey.rawRepresentation), v.server_public)
        XCTAssertEqual(Fingerprint.of(publicKey: server.publicKey.rawRepresentation), v.server_fingerprint)

        let fromClient = try SessionKeys.derive(
            privateKey: client, peerPublicKey: v.data(v.server_public),
            clientNonce: v.data(v.client_nonce), serverNonce: v.data(v.server_nonce))
        let fromServer = try SessionKeys.derive(
            privateKey: server, peerPublicKey: v.data(v.client_public),
            clientNonce: v.data(v.client_nonce), serverNonce: v.data(v.server_nonce))
        XCTAssertEqual(Hex.encode(raw(fromClient.clientToServer)), v.key_c2s)
        XCTAssertEqual(Hex.encode(raw(fromClient.serverToClient)), v.key_s2c)
        XCTAssertEqual(raw(fromServer.clientToServer), raw(fromClient.clientToServer))
    }

    func testFramesMatchVectors() throws {
        let v = try Vectors.load()
        var sealers = [
            "c2s": FrameSealer(key: SymmetricKey(data: v.data(v.key_c2s))),
            "s2c": FrameSealer(key: SymmetricKey(data: v.data(v.key_s2c))),
        ]
        var openers = [
            "c2s": FrameOpener(key: SymmetricKey(data: v.data(v.key_c2s))),
            "s2c": FrameOpener(key: SymmetricKey(data: v.data(v.key_s2c))),
        ]
        for frame in v.frames {
            XCTAssertEqual(sealers[frame.direction]!.counter, frame.counter)
            let sealed = try sealers[frame.direction]!.seal(Data(frame.plaintext.utf8))
            XCTAssertEqual(Hex.encode(sealed), frame.frame)
            let opened = try openers[frame.direction]!.open(v.data(frame.frame))
            XCTAssertEqual(String(decoding: opened, as: UTF8.self), frame.plaintext)
            XCTAssertNotNil(SecureMessage.decode(opened))
        }
    }
}
