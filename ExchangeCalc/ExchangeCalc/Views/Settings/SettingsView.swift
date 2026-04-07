import SwiftUI

struct SettingsView: View {
    let onBack: () -> Void
    @State private var settings = AppSettings.shared
    @State private var locale = LocaleManager.shared

    var body: some View {
        NavigationStack {
            List {
                // Default Currencies
                Section(locale.localized("default_currencies")) {
                    HStack {
                        Text(locale.localized("from_currency"))
                        Spacer()
                        Text("\(settings.lastFromCurrency.flag) \(settings.lastFromCurrency.rawValue)")
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Text(locale.localized("to_currency"))
                        Spacer()
                        Text("\(settings.lastToCurrency.flag) \(settings.lastToCurrency.rawValue)")
                            .foregroundStyle(.secondary)
                    }
                }

                // Language
                Section(locale.localized("language")) {
                    ForEach(AppLanguage.allCases, id: \.self) { lang in
                        Button {
                            withAnimation {
                                locale.setLanguage(lang)
                                settings.appLanguage = lang
                            }
                        } label: {
                            HStack {
                                Text(lang.displayName)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if (lang == .system && locale.isSystem) ||
                                   (lang != .system && settings.appLanguage == lang && !locale.isSystem) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppTheme.primary)
                                }
                            }
                        }
                    }
                }

                // Appearance
                Section(locale.localized("appearance")) {
                    Toggle(locale.localized("dark_mode"), isOn: Binding(
                        get: { settings.darkModeEnabled },
                        set: { settings.darkModeEnabled = $0 }
                    ))
                }

                // Legal
                Section(locale.localized("legal")) {
                    Link(locale.localized("terms_of_service"), destination: URL(string: "https://tomo-aidev.github.io/exchange-calc/terms.html")!)
                    Link(locale.localized("privacy_policy"), destination: URL(string: "https://tomo-aidev.github.io/exchange-calc/privacy.html")!)
                }

                // About
                Section(locale.localized("about")) {
                    Button(locale.localized("rate_app")) {
                        ReviewManager.shared.requestReviewManually()
                    }

                    HStack {
                        Text(locale.localized("version"))
                        Spacer()
                        Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle(locale.localized("settings"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(locale.localized("back")) {
                        onBack()
                    }
                }
            }
        }
    }
}
