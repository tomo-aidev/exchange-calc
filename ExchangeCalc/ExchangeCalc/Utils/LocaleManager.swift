import Foundation
import SwiftUI

@MainActor
@Observable
final class LocaleManager: Sendable {
    static let shared = LocaleManager()

    private let languageKey = "appLanguage"
    private var strings: [String: String] = [:]

    var currentLanguage: String = "en" {
        didSet { loadStrings() }
    }

    private init() {
        let saved = UserDefaults.standard.string(forKey: languageKey) ?? "system"
        if saved == "system" {
            let systemLang = Locale.current.language.languageCode?.identifier ?? "en"
            currentLanguage = supportedLanguages.contains(systemLang) ? systemLang : "en"
        } else {
            currentLanguage = saved
        }
        loadStrings()
    }

    private let supportedLanguages = ["en", "ja", "ko", "zh-Hans", "zh-Hant", "th", "vi", "ru", "id", "fr", "de", "es"]

    func setLanguage(_ lang: AppLanguage) {
        if lang == .system {
            let systemLang = Locale.current.language.languageCode?.identifier ?? "en"
            currentLanguage = supportedLanguages.contains(systemLang) ? systemLang : "en"
            UserDefaults.standard.set("system", forKey: languageKey)
        } else {
            currentLanguage = lang.rawValue
            UserDefaults.standard.set(lang.rawValue, forKey: languageKey)
        }
    }

    var isSystem: Bool {
        UserDefaults.standard.string(forKey: languageKey) == "system"
    }

    func t(_ key: String) -> String {
        strings[key] ?? key
    }

    private func loadStrings() {
        // Try exact match, then prefix match, then English
        let candidates = [currentLanguage, String(currentLanguage.prefix(2)), "en"]
        for lang in candidates {
            if let path = Bundle.main.path(forResource: lang, ofType: "lproj"),
               let bundle = Bundle(path: path) {
                if let url = bundle.url(forResource: "Localizable", withExtension: "strings"),
                   let dict = NSDictionary(contentsOf: url) as? [String: String] {
                    strings = dict
                    return
                }
                // xcstrings compiled format
                if let url = bundle.url(forResource: "Localizable", withExtension: "xcstrings") {
                    // xcstrings are compiled to .strings at build time
                }
                // Try using bundle's localizedString
                strings = [:]
                return
            }
        }
        strings = [:]
    }

    /// Get localized string using Bundle mechanism with language override
    func localized(_ key: String) -> String {
        let candidates = [currentLanguage, String(currentLanguage.prefix(2)), "en"]
        for lang in candidates {
            if let path = Bundle.main.path(forResource: lang, ofType: "lproj"),
               let bundle = Bundle(path: path) {
                let value = bundle.localizedString(forKey: key, value: nil, table: nil)
                if value != key { return value }
            }
        }
        return key
    }
}
