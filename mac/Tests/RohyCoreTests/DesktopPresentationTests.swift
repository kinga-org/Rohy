import Foundation
import RohyCore
import XCTest

/// Same expected values as `desktop/apps/rohy-desktop/src/state.rs` tests.
final class DesktopPresentationTests: XCTestCase {
    typealias P = DesktopPresentation

    func testHeaderDescribesEachState() {
        let now = Date(timeIntervalSince1970: 112)
        XCTAssertEqual(P.header(listening: false, failure: nil, connected: [], activity: nil, now: now), .starting)
        XCTAssertEqual(P.header(listening: false, failure: "port occupé", connected: [], activity: nil, now: now), .failed("port occupé"))
        XCTAssertEqual(P.header(listening: true, failure: nil, connected: [], activity: nil, now: now), .waiting)
        XCTAssertEqual(
            P.header(listening: true, failure: nil, connected: ["Galaxy"], activity: nil, now: now),
            .connected(names: "Galaxy", activity: nil)
        )
        let received = P.Activity.received(from: "Galaxy", at: Date(timeIntervalSince1970: 100))
        XCTAssertEqual(
            P.header(listening: true, failure: nil, connected: ["Galaxy", "Pixel"], activity: received, now: now),
            .connected(names: "Galaxy, Pixel", activity: "Texte reçu de Galaxy · il y a 12 s")
        )
        XCTAssertEqual(P.Activity.paired("Galaxy").text, "Galaxy associé")
    }

    func testHeaderTexts() {
        XCTAssertEqual(P.Header.starting.title, "Démarrage…")
        XCTAssertEqual(P.Header.starting.subtitle, "Rohy prépare le serveur local.")
        XCTAssertEqual(P.Header.waiting.title, "En attente du téléphone")
        XCTAssertEqual(P.Header.waiting.subtitle, "Rohy écoute sur le réseau local.")
        XCTAssertEqual(P.Header.failed("port occupé").title, "Erreur réseau")
        XCTAssertEqual(P.Header.failed("port occupé").subtitle, "port occupé")
        XCTAssertEqual(P.Header.connected(names: "Galaxy", activity: nil).title, "Connecté à Galaxy")
        XCTAssertNil(P.Header.connected(names: "Galaxy", activity: nil).subtitle)
    }

    func testTimesAndDatesReadNaturally() {
        XCTAssertEqual(P.relativeTime(-3), "à l'instant")
        XCTAssertEqual(P.relativeTime(2), "à l'instant")
        XCTAssertEqual(P.relativeTime(12), "il y a 12 s")
        XCTAssertEqual(P.relativeTime(180), "il y a 3 min")
        XCTAssertEqual(P.relativeTime(7200), "il y a 2 h")
        XCTAssertEqual(P.relativeTime(4 * 86_400), "il y a 4 j")
        // 2026-11-17 12:00 UTC: the 17th from UTC−11 to UTC+11.
        let paired = Date(timeIntervalSince1970: 1_794_916_800)
        XCTAssertEqual(P.shortDate(paired, timeZone: TimeZone(identifier: "UTC")!), "17 nov.")
        XCTAssertEqual(P.deviceDetail(connected: true, pairedAt: paired, timeZone: TimeZone(identifier: "UTC")!), "Connecté · associé le 17 nov.")
        XCTAssertEqual(P.deviceDetail(connected: false, pairedAt: paired, timeZone: TimeZone(identifier: "UTC")!), "Hors ligne · associé le 17 nov.")
        XCTAssertEqual(P.footer(computerName: "MacBook-Pro", port: 47823), "Visible sur le réseau local comme « MacBook-Pro » · port 47823")
        XCTAssertEqual(P.footer(computerName: "MacBook-Pro", port: nil), "Visible sur le réseau local comme « MacBook-Pro »")
    }
}
