import Foundation

enum AppLanguage: String, CaseIterable {
    case system = "system"
    case en = "en"
    case ja = "ja"
    case ko = "ko"
    case zhHans = "zh-Hans"
    case zhHant = "zh-Hant"
    case th = "th"
    case vi = "vi"
    case ru = "ru"
    case id = "id"
    case fr = "fr"
    case de = "de"
    case es = "es"

    var displayName: String {
        switch self {
        case .system: return "System Default"
        case .en: return "English"
        case .ja: return "日本語"
        case .ko: return "한국어"
        case .zhHans: return "简体中文"
        case .zhHant: return "繁體中文"
        case .th: return "ไทย"
        case .vi: return "Tiếng Việt"
        case .ru: return "Русский"
        case .id: return "Bahasa Indonesia"
        case .fr: return "Français"
        case .de: return "Deutsch"
        case .es: return "Español"
        }
    }
}

@MainActor
@Observable
final class AppSettings: Sendable {
    static let shared = AppSettings()

    private let fromCurrencyKey = "lastFromCurrency"
    private let toCurrencyKey = "lastToCurrency"
    private let darkModeKey = "darkModeEnabled"
    private let languageKey = "appLanguage"

    var lastFromCurrency: Currency {
        didSet {
            UserDefaults.standard.set(lastFromCurrency.rawValue, forKey: fromCurrencyKey)
        }
    }

    var lastToCurrency: Currency {
        didSet {
            UserDefaults.standard.set(lastToCurrency.rawValue, forKey: toCurrencyKey)
        }
    }

    var darkModeEnabled: Bool {
        didSet {
            UserDefaults.standard.set(darkModeEnabled, forKey: darkModeKey)
        }
    }

    var appLanguage: AppLanguage {
        didSet {
            UserDefaults.standard.set(appLanguage.rawValue, forKey: languageKey)
            if let id = appLanguage == .system ? nil : appLanguage.rawValue {
                UserDefaults.standard.set([id], forKey: "AppleLanguages")
            } else {
                UserDefaults.standard.removeObject(forKey: "AppleLanguages")
            }
            UserDefaults.standard.synchronize()
        }
    }

    private init() {
        let fromRaw = UserDefaults.standard.string(forKey: fromCurrencyKey) ?? "USD"
        lastFromCurrency = Currency(rawValue: fromRaw) ?? .usd

        let toRaw = UserDefaults.standard.string(forKey: toCurrencyKey) ?? "JPY"
        lastToCurrency = Currency(rawValue: toRaw) ?? .jpy

        darkModeEnabled = UserDefaults.standard.bool(forKey: darkModeKey)

        let langRaw = UserDefaults.standard.string(forKey: languageKey) ?? "system"
        appLanguage = AppLanguage(rawValue: langRaw) ?? .system
    }
}
