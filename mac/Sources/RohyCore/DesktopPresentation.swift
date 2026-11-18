import Foundation

/// Texts of the desktop windows. Mirrors `desktop/apps/rohy-desktop/src/state.rs`: both apps must
/// show the same thing (see "Deux apps bureau" in the README), so both test the same values.
public enum DesktopPresentation {
    /// Top of the paired phones panel.
    public enum Header: Equatable {
        case starting
        case failed(String)
        case waiting
        case connected(names: String, activity: String?)

        public var title: String {
            switch self {
            case .starting: return "Démarrage…"
            case .failed: return "Erreur réseau"
            case .waiting: return "En attente du téléphone"
            case .connected(let names, _): return "Connecté à \(names)"
            }
        }

        public var subtitle: String? {
            switch self {
            case .starting: return "Rohy prépare le serveur local."
            case .failed(let error): return error
            case .waiting: return "Rohy écoute sur le réseau local."
            case .connected: return nil
            }
        }
    }

    /// Last thing that happened, shown under the connection banner.
    public struct Activity: Equatable {
        public let text: String
        public let at: Date

        public init(text: String, at: Date) {
            self.text = text
            self.at = at
        }

        public static func paired(_ name: String, at: Date = Date()) -> Activity { Activity(text: "\(name) associé", at: at) }
        public static func received(from name: String, at: Date = Date()) -> Activity {
            Activity(text: "Texte reçu de \(name)", at: at)
        }
    }

    /// Server not started yet (`listening == nil`), failed, or listening with these connected phones.
    public static func header(listening: Bool, failure: String?, connected: [String], activity: Activity?, now: Date) -> Header {
        if let failure { return .failed(failure) }
        guard listening else { return .starting }
        guard !connected.isEmpty else { return .waiting }
        let line = activity.map { "\($0.text) · \(relativeTime(now.timeIntervalSince($0.at)))" }
        return .connected(names: connected.joined(separator: ", "), activity: line)
    }

    /// "Connecté · associé le 18 nov."
    public static func deviceDetail(connected: Bool, pairedAt: Date, timeZone: TimeZone = .current) -> String {
        "\(connected ? "Connecté" : "Hors ligne") · associé le \(shortDate(pairedAt, timeZone: timeZone))"
    }

    /// "Visible sur le réseau local comme « MacBook-Pro » · port 47823"
    public static func footer(computerName: String, port: UInt16?) -> String {
        let base = "Visible sur le réseau local comme « \(computerName) »"
        return port.map { "\(base) · port \($0)" } ?? base
    }

    /// "à l'instant", "il y a 12 s", "il y a 3 min", "il y a 2 h", "il y a 4 j".
    public static func relativeTime(_ elapsed: TimeInterval) -> String {
        let seconds = max(0, Int(elapsed))
        switch seconds {
        case ..<5: return "à l'instant"
        case ..<60: return "il y a \(seconds) s"
        case ..<3600: return "il y a \(seconds / 60) min"
        case ..<86_400: return "il y a \(seconds / 3600) h"
        default: return "il y a \(seconds / 86_400) j"
        }
    }

    private static let months = ["janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."]

    /// "18 nov." in the given time zone.
    public static func shortDate(_ date: Date, timeZone: TimeZone = .current) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let parts = calendar.dateComponents([.day, .month], from: date)
        return "\(parts.day ?? 0) \(months[(parts.month ?? 1) - 1])"
    }
}
