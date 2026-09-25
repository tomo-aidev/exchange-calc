import SwiftUI

struct SettingsView: View {
    let onBack: () -> Void
    @State private var settings = AppSettings.shared

    var body: some View {
        NavigationStack {
            List {
                // Default Currencies
                Section(String(localized: "default_currencies")) {
                    HStack {
                        Text(String(localized: "from_currency"))
                        Spacer()
                        Text("\(settings.lastFromCurrency.flag) \(settings.lastFromCurrency.rawValue)")
                            .foregroundStyle(.secondary)
                    }

                    HStack {
                        Text(String(localized: "to_currency"))
                        Spacer()
                        Text("\(settings.lastToCurrency.flag) \(settings.lastToCurrency.rawValue)")
                            .foregroundStyle(.secondary)
                    }
                }

                // Language
                Section(String(localized: "language")) {
                    ForEach(AppLanguage.allCases, id: \.self) { lang in
                        Button {
                            settings.appLanguage = lang
                        } label: {
                            HStack {
                                Text(lang.displayName)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if settings.appLanguage == lang {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppTheme.primary)
                                }
                            }
                        }
                    }
                }

                // Appearance
                Section(String(localized: "appearance")) {
                    Toggle(String(localized: "dark_mode"), isOn: Binding(
                        get: { settings.darkModeEnabled },
                        set: { settings.darkModeEnabled = $0 }
                    ))
                }

                // Legal
                Section(String(localized: "legal")) {
                    Link(String(localized: "terms_of_service"), destination: URL(string: "https://tomo-aidev.github.io/exchange-calc/terms.html")!)
                    Link(String(localized: "privacy_policy"), destination: URL(string: "https://tomo-aidev.github.io/exchange-calc/privacy.html")!)
                }

                // About
                Section(String(localized: "about")) {
                    Button(String(localized: "rate_app")) {
                        ReviewManager.shared.requestReviewManually()
                    }

                    HStack {
                        Text(String(localized: "version"))
                        Spacer()
                        Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle(String(localized: "settings"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "back")) {
                        onBack()
                    }
                }
            }
        }
    }
}
