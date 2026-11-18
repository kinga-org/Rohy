import CryptoKit
import Foundation
import XCTest
@testable import RohyCore

/// Minimal phone-side implementation used to drive the server session in tests.
struct TestPhone {
    let key = Curve25519.KeyAgreement.PrivateKey()
    let nonce = Random.bytes(RohyProtocol.nonceLength)
    var sealer: FrameSealer?
    var opener: FrameOpener?

    var publicKey: Data { key.publicKey.rawRepresentation }

    func hello(name: String = "Galaxy", version: Int = RohyProtocol.version) -> String {
        let hello = Hello(v: version, pk: Base64URL.encode(publicKey), n: Base64URL.encode(nonce), name: name)
        return String(decoding: try! JSONEncoder().encode(hello), as: UTF8.self)
    }

    mutating func accept(_ welcomeText: String) throws -> Welcome {
        let welcome = try JSONDecoder().decode(Welcome.self, from: Data(welcomeText.utf8))
        let keys = try SessionKeys.derive(
            privateKey: key, peerPublicKey: Base64URL.decode(welcome.pk)!,
            clientNonce: nonce, serverNonce: Base64URL.decode(welcome.n)!)
        sealer = FrameSealer(key: keys.clientToServer)
        opener = FrameOpener(key: keys.serverToClient)
        return welcome
    }

    mutating func seal(_ message: SecureMessage) throws -> Data {
        try sealer!.seal(message.encoded())
    }

    mutating func open(_ frame: Data) throws -> SecureMessage? {
        SecureMessage.decode(try opener!.open(frame))
    }
}

final class ServerSessionTests: XCTestCase {
    let serverKey = Curve25519.KeyAgreement.PrivateKey()
    var store = InMemoryTrustedDeviceStore()
    let pairing = PairingWindow(makeToken: { Data(repeating: 42, count: 32) })

    override func setUp() {
        store = InMemoryTrustedDeviceStore()
    }

    func makeSession() -> ServerSession {
        ServerSession(identity: serverKey, serverName: "Mac", store: store, pairing: pairing)
    }

    func text(_ actions: [SessionAction]) -> String? {
        for case .sendText(let text) in actions { return text }
        return nil
    }

    func binary(_ actions: [SessionAction]) -> Data? {
        for case .sendBinary(let data) in actions { return data }
        return nil
    }

    func testPairsWithValidTokenThenExchangesClips() throws {
        pairing.open()
        let session = makeSession()
        var phone = TestPhone()

        let welcome = try phone.accept(XCTUnwrap(text(session.receiveText(phone.hello()))))
        XCTAssertEqual(welcome.status, .pairing)
        XCTAssertEqual(Base64URL.decode(welcome.pk), serverKey.publicKey.rawRepresentation)
        XCTAssertNil(session.device)

        let actions = session.receiveBinary(try phone.seal(.pair(token: Data(repeating: 42, count: 32))))
        guard case .paired(let device) = actions.first else { return XCTFail("not paired: \(actions)") }
        XCTAssertEqual(device.name, "Galaxy")
        XCTAssertEqual(store.find(publicKey: phone.publicKey), device)
        XCTAssertEqual(try phone.open(XCTUnwrap(binary(actions))), .paired(name: "Mac"))
        XCTAssertFalse(pairing.isOpen)
        XCTAssertEqual(session.device, device)

        let clip = Clip(text: "depuis le téléphone")
        XCTAssertEqual(session.receiveBinary(try phone.seal(.clip(clip))), [.received(clip)])

        let outgoing = Clip(text: "depuis le Mac", sensitive: true)
        XCTAssertEqual(try phone.open(XCTUnwrap(binary(session.send(.clip(outgoing))))), .clip(outgoing))

        XCTAssertEqual(try phone.open(XCTUnwrap(binary(session.receiveBinary(try phone.seal(.ping))))), .pong)
        XCTAssertEqual(session.receiveBinary(try phone.seal(.pong)), [])
    }

    func testKnownDeviceIsAuthenticatedWithoutPairing() throws {
        var phone = TestPhone()
        let device = TrustedDevice(publicKey: phone.publicKey, name: "Galaxy")
        store.save(device)
        let session = makeSession()
        let actions = session.receiveText(phone.hello())
        XCTAssertEqual(actions.last, .authenticated(device))
        XCTAssertEqual(try phone.accept(XCTUnwrap(text(actions))).status, .trusted)
        XCTAssertEqual(session.receiveBinary(try phone.seal(.clip(Clip(id: "a", text: "x")))),
                       [.received(Clip(id: "a", text: "x"))])
    }

    func testUnknownDeviceIsRejectedWhenPairingIsClosed() {
        pairing.close()
        let session = makeSession()
        XCTAssertEqual(session.receiveText(TestPhone().hello()), [.close(.forbidden)])
        XCTAssertTrue(session.isClosed)
        XCTAssertEqual(session.send(.ping), [])
    }

    func testWrongTokenIsRejectedAndConsumed() throws {
        pairing.open()
        let session = makeSession()
        var phone = TestPhone()
        _ = try phone.accept(XCTUnwrap(text(session.receiveText(phone.hello()))))
        XCTAssertEqual(session.receiveBinary(try phone.seal(.pair(token: Data(repeating: 1, count: 32)))),
                       [.close(.forbidden)])
        XCTAssertFalse(pairing.isOpen)
        XCTAssertNil(store.find(publicKey: phone.publicKey))
    }

    func testClipBeforePairingIsForbidden() throws {
        pairing.open()
        let session = makeSession()
        var phone = TestPhone()
        _ = try phone.accept(XCTUnwrap(text(session.receiveText(phone.hello()))))
        XCTAssertEqual(session.receiveBinary(try phone.seal(.clip(Clip(text: "x")))), [.close(.forbidden)])
        XCTAssertEqual(session.send(.ping), [])
    }

    func testReplayedFrameClosesSession() throws {
        var phone = TestPhone()
        store.save(TrustedDevice(publicKey: phone.publicKey, name: "Galaxy"))
        let session = makeSession()
        _ = try phone.accept(XCTUnwrap(text(session.receiveText(phone.hello()))))
        let frame = try phone.seal(.ping)
        XCTAssertNotNil(binary(session.receiveBinary(frame)))
        XCTAssertEqual(session.receiveBinary(frame), [.close(.invalidFrame)])
    }

    func testPairingMessagesAfterAuthenticationAreInvalid() throws {
        var phone = TestPhone()
        store.save(TrustedDevice(publicKey: phone.publicKey, name: "Galaxy"))
        let session = makeSession()
        _ = try phone.accept(XCTUnwrap(text(session.receiveText(phone.hello()))))
        XCTAssertEqual(session.receiveBinary(try phone.seal(.paired(name: "x"))), [.close(.invalidFrame)])
    }

    func testRejectsInvalidHellos() {
        pairing.open()
        let phone = TestPhone()
        let cases: [(String, CloseCode)] = [
            ("not json", .invalidFrame),
            (#"{"t":"welcome","v":1,"pk":"","n":"","name":""}"#, .invalidFrame),
            (phone.hello(version: 2), .unsupportedVersion),
            (#"{"t":"hello","v":1,"pk":"AAAA","n":"AAAAAAAAAAAAAAAAAAAAAA","name":"x"}"#, .invalidFrame),
        ]
        for (hello, code) in cases {
            XCTAssertEqual(makeSession().receiveText(hello), [.close(code)], hello)
        }
    }

    func testRejectsFramesInWrongState() throws {
        let session = makeSession()
        XCTAssertEqual(session.receiveBinary(Data(count: 40)), [.close(.invalidFrame)])
        XCTAssertEqual(session.receiveBinary(Data(count: 40)), [.close(.invalidFrame)])

        var phone = TestPhone()
        store.save(TrustedDevice(publicKey: phone.publicKey, name: "Galaxy"))
        let second = makeSession()
        _ = try phone.accept(XCTUnwrap(text(second.receiveText(phone.hello()))))
        XCTAssertEqual(second.receiveText(phone.hello()), [.close(.invalidFrame)])

        pairing.open()
        let third = makeSession()
        var other = TestPhone()
        _ = try other.accept(XCTUnwrap(text(third.receiveText(other.hello()))))
        XCTAssertEqual(third.receiveBinary(Data(count: 40)), [.close(.invalidFrame)])
    }
}
