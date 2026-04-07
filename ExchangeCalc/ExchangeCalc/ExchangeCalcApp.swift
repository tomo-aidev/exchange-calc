import SwiftUI
import SwiftData
import GoogleMobileAds

@main
struct ExchangeCalcApp: App {

    init() {
        MobileAds.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .preferredColorScheme(AppSettings.shared.darkModeEnabled ? .dark : .light)
        }
        .modelContainer(for: ConversionHistory.self)
    }
}
