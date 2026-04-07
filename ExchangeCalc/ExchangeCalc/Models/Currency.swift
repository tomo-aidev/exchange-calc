import Foundation

enum Currency: String, CaseIterable, Codable, Sendable {
    case usd = "USD"
    case eur = "EUR"
    case gbp = "GBP"
    case jpy = "JPY"
    case krw = "KRW"
    case cny = "CNY"
    case twd = "TWD"
    case hkd = "HKD"
    case thb = "THB"
    case aud = "AUD"
    case php = "PHP"
    case vnd = "VND"
    case idr = "IDR"
    case myr = "MYR"
    case sgd = "SGD"
    case cad = "CAD"
    case inr = "INR"
    case chf = "CHF"
    case rub = "RUB"

    var symbol: String {
        switch self {
        case .usd: return "$"
        case .eur: return "\u{20AC}"
        case .gbp: return "\u{00A3}"
        case .jpy: return "\u{00A5}"
        case .krw: return "\u{20A9}"
        case .cny: return "\u{00A5}"
        case .twd: return "NT$"
        case .hkd: return "HK$"
        case .thb: return "\u{0E3F}"
        case .aud: return "A$"
        case .php: return "\u{20B1}"
        case .vnd: return "\u{20AB}"
        case .idr: return "Rp"
        case .myr: return "RM"
        case .sgd: return "S$"
        case .cad: return "C$"
        case .inr: return "\u{20B9}"
        case .chf: return "CHF"
        case .rub: return "\u{20BD}"
        }
    }

    var flag: String {
        switch self {
        case .usd: return "\u{1F1FA}\u{1F1F8}"
        case .eur: return "\u{1F1EA}\u{1F1FA}"
        case .gbp: return "\u{1F1EC}\u{1F1E7}"
        case .jpy: return "\u{1F1EF}\u{1F1F5}"
        case .krw: return "\u{1F1F0}\u{1F1F7}"
        case .cny: return "\u{1F1E8}\u{1F1F3}"
        case .twd: return "\u{1F1F9}\u{1F1FC}"
        case .hkd: return "\u{1F1ED}\u{1F1F0}"
        case .thb: return "\u{1F1F9}\u{1F1ED}"
        case .aud: return "\u{1F1E6}\u{1F1FA}"
        case .php: return "\u{1F1F5}\u{1F1ED}"
        case .vnd: return "\u{1F1FB}\u{1F1F3}"
        case .idr: return "\u{1F1EE}\u{1F1E9}"
        case .myr: return "\u{1F1F2}\u{1F1FE}"
        case .sgd: return "\u{1F1F8}\u{1F1EC}"
        case .cad: return "\u{1F1E8}\u{1F1E6}"
        case .inr: return "\u{1F1EE}\u{1F1F3}"
        case .chf: return "\u{1F1E8}\u{1F1ED}"
        case .rub: return "\u{1F1F7}\u{1F1FA}"
        }
    }

    var decimalPlaces: Int {
        switch self {
        case .jpy, .krw, .vnd, .idr: return 0
        default: return 2
        }
    }

    var nameKey: String {
        return "currency_\(rawValue.lowercased())"
    }
}
