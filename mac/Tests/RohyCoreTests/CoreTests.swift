import CryptoKit
import Foundation
import XCTest
@testable import RohyCore

final class EncodingTests: XCTestCase {
    func testBase64URLRoundTripsWithoutPadding() {
        for length in 0...40 {
            let data = Data((0..<length).map { UInt8(($0 * 37 + 250) % 256) })
            let encoded = Base64URL.encode(data)
            XCTAssertFalse(encoded.contains("="))
            XCTAssertFalse(encoded.contains("+"))
            XCTAssertFalse(encoded.contains("/"))
            XCTAssertEqual(Base64URL.decode(encoded), data)
        }
    }

    func testBase64URLRejectsInvalidInput() {
        XCTAssertNil(Base64URL.decode("abcde"))
        XCTAssertNil(Base64URL.decode("ab=="))
        XCTAssertNil(Base64URL.decode("a$bc"))
    }

    func testHexRejectsInvalidInput() {
        XCTAssertNil(Hex.decode("abc"))
        XCTAssertNil(Hex.decode("zz"))
        XCTAssertEqual(Hex.decode("00ff"), Data([0, 255]))
    }
}

final class FrameTests: XCTestCase {
    let key = SymmetricKey(size: .bits256)

    func testRejectsReplayAndReordering() throws {
        var sealer = FrameSealer(key: key)
        var opener = FrameOpener(key: key)
        let first = try sealer.seal(Data("a".utf8))
        let second = try sealer.seal(Data("b".utf8))
        XCTAssertThrowsError(try opener.open(second)) { XCTAssertEqual($0 as? FrameError, .unexpectedCounter) }
        XCTAssertEqual(try opener.open(first), Data("a".utf8))
        XCTAssertThrowsError(try opener.open(first)) { XCTAssertEqual($0 as? FrameError, .unexpectedCounter) }
        XCTAssertEqual(try opener.open(second), Data("b".utf8))
    }

    func testRejectsTamperedAndShortFrames() throws {
        var sealer = FrameSealer(key: key)
        var opener = FrameOpener(key: key)
        var frame = try sealer.seal(Data("secret".utf8))
        frame[frame.count - 1] ^= 0x01
        XCTAssertThrowsError(try opener.open(frame)) { XCTAssertEqual($0 as? FrameError, .authenticationFailed) }
        XCTAssertThrowsError(try opener.open(Data(count: 10))) { XCTAssertEqual($0 as? FrameError, .tooShort) }
        XCTAssertEqual(opener.expectedCounter, 0)
    }

    func testConstantTimeEquals() {
        XCTAssertTrue(constantTimeEquals(Data([1, 2]), Data([1, 2])))
        XCTAssertFalse(constantTimeEquals(Data([1, 2]), Data([1, 3])))
        XCTAssertFalse(constantTimeEquals(Data([1]), Data([1, 2])))
        XCTAssertEqual(Random.bytes(16).count, 16)
    }
}

final class MessageTests: XCTestCase {
    func testSecureMessagesRoundTrip() {
        let messages: [SecureMessage] = [
            .pair(token: Data(repeating: 7, count: 32)),
            .paired(name: "Mac"),
            .clip(Clip(id: "1", text: "héllo / \"quoted\"", sensitive: true)),
            .ping,
            .pong,
        ]
        for message in messages {
            XCTAssertEqual(SecureMessage.decode(message.encoded()), message)
        }
    }

    func testRejectsMalformedMessages() {
        let invalid = [
            "nope", #"{"t":"unknown"}"#, #"{"t":"pair"}"#, #"{"t":"pair","token":"*"}"#,
            #"{"t":"paired"}"#, #"{"t":"clip","id":"1"}"#,
        ]
        for json in invalid {
            XCTAssertNil(SecureMessage.decode(Data(json.utf8)), json)
        }
        XCTAssertEqual(SecureMessage.decode(Data(#"{"t":"clip","id":"1","text":"x"}"#.utf8)),
                       .clip(Clip(id: "1", text: "x", sensitive: false)))
    }

    func testClipDefaultsToLowercaseUUID() {
        let clip = Clip(text: "x")
        XCTAssertEqual(clip.id, clip.id.lowercased())
        XCTAssertFalse(clip.sensitive)
    }
}

final class PairingTests: XCTestCase {
    func testPayloadRoundTrip() {
        let payload = PairingPayload(
            publicKey: Data(repeating: 1, count: 32), token: Data(repeating: 2, count: 32),
            name: "Mac de test & co + plus", hosts: ["192.168.1.10", "10.0.0.2"], port: 47823)
        XCTAssertTrue(payload.uri.hasPrefix("rohy://pair?v=1&"))
        XCTAssertEqual(PairingPayload.parse(payload.uri), payload)
    }

    func testPayloadRejectsInvalidUris() {
        let pk = Base64URL.encode(Data(repeating: 1, count: 32))
        let t = Base64URL.encode(Data(repeating: 2, count: 32))
        let invalid = [
            "https://pair?v=1", "rohy://other?v=1",
            "rohy://pair?v=2&pk=\(pk)&t=\(t)&n=M&p=1",
            "rohy://pair?v=1&pk=AAAA&t=\(t)&n=M&p=1",
            "rohy://pair?v=1&pk=\(pk)&t=AAAA&n=M&p=1",
            "rohy://pair?v=1&pk=\(pk)&t=\(t)&n=&p=1",
            "rohy://pair?v=1&pk=\(pk)&t=\(t)&n=M&p=99999",
        ]
        for uri in invalid { XCTAssertNil(PairingPayload.parse(uri), uri) }
        XCTAssertEqual(PairingPayload.parse("rohy://pair?v=1&pk=\(pk)&t=\(t)&n=M&p=1")?.hosts, [])
    }

    func testWindowTokenIsSingleUse() {
        let window = PairingWindow(makeToken: { Data(repeating: 9, count: 32) })
        XCTAssertFalse(window.isOpen)
        XCTAssertFalse(window.consume(Data(repeating: 9, count: 32)))
        window.open()
        XCTAssertTrue(window.isOpen)
        XCTAssertTrue(window.consume(Data(repeating: 9, count: 32)))
        XCTAssertFalse(window.isOpen)
        window.open()
        XCTAssertFalse(window.consume(Data(repeating: 8, count: 32)))
        XCTAssertFalse(window.isOpen)
        window.open()
        window.close()
        XCTAssertNil(window.token)
        XCTAssertEqual(PairingWindow().open().count, 32)
    }
}

final class ClipboardPolicyTests: XCTestCase {
    func testFiltersSecretsEmptyAndOversizedText() {
        XCTAssertTrue(ClipboardPolicy.shouldSync(types: ["public.utf8-plain-text"], text: "hi"))
        XCTAssertFalse(ClipboardPolicy.shouldSync(types: ["org.nspasteboard.ConcealedType"], text: "pw"))
        XCTAssertFalse(ClipboardPolicy.shouldSync(types: ["org.nspasteboard.TransientType"], text: "x"))
        XCTAssertFalse(ClipboardPolicy.shouldSync(types: [], text: nil))
        XCTAssertFalse(ClipboardPolicy.shouldSync(types: [], text: ""))
        XCTAssertFalse(ClipboardPolicy.shouldSync(types: [], text: String(repeating: "a", count: RohyProtocol.maxTextBytes + 1)))
    }

    func testEchoGuardIgnoresOwnWritesAndUnchangedCounts() {
        var guardian = EchoGuard(currentChangeCount: 5)
        XCTAssertFalse(guardian.isExternalChange(changeCount: 5))
        XCTAssertTrue(guardian.isExternalChange(changeCount: 6))
        XCTAssertFalse(guardian.isExternalChange(changeCount: 6))
        guardian.markWritten(changeCount: 7)
        XCTAssertFalse(guardian.isExternalChange(changeCount: 7))
        XCTAssertTrue(guardian.isExternalChange(changeCount: 8))
        var fresh = EchoGuard()
        XCTAssertTrue(fresh.isExternalChange(changeCount: 1))
    }
}

final class TrustedDeviceStoreTests: XCTestCase {
    func testFileStorePersistsAcrossInstances() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString).appendingPathComponent("devices.json")
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }
        let store = FileTrustedDeviceStore(url: url)
        XCTAssertTrue(store.all().isEmpty)
        let device = TrustedDevice(publicKey: Data(repeating: 3, count: 32), name: "Galaxy")
        store.save(device)
        store.save(TrustedDevice(publicKey: device.publicKey, name: "Galaxy S25", pairedAt: device.pairedAt))

        let reloaded = FileTrustedDeviceStore(url: url)
        XCTAssertEqual(reloaded.all().count, 1)
        XCTAssertEqual(reloaded.find(publicKey: device.publicKey)?.name, "Galaxy S25")
        reloaded.remove(id: device.id)
        XCTAssertTrue(FileTrustedDeviceStore(url: url).all().isEmpty)
    }

    func testInMemoryStore() {
        let device = TrustedDevice(publicKey: Data(repeating: 4, count: 32), name: "Pixel")
        let store = InMemoryTrustedDeviceStore(devices: [device])
        XCTAssertEqual(store.find(publicKey: device.publicKey), device)
        store.save(device)
        XCTAssertEqual(store.all().count, 1)
        store.remove(id: device.id)
        XCTAssertNil(store.find(publicKey: device.publicKey))
    }
}
