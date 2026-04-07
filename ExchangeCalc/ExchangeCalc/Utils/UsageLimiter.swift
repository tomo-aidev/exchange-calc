import Foundation

@MainActor
@Observable
final class UsageLimiter: Sendable {
    static let shared = UsageLimiter()
    static let dailyLimit = 10

    private let countKey = "dailyUsageCount"
    private let dateKey = "dailyUsageDate"

    private(set) var todayUsageCount: Int = 0
    var showPaywall: Bool = false

    var remainingCount: Int {
        if PurchaseManager.shared.isProUnlocked { return 999 }
        loadTodayUsage()
        return max(0, Self.dailyLimit - todayUsageCount)
    }

    var isLimitReached: Bool {
        if PurchaseManager.shared.isProUnlocked { return false }
        loadTodayUsage()
        return todayUsageCount >= Self.dailyLimit
    }

    private init() {
        loadTodayUsage()
    }

    func recordUsage() {
        if PurchaseManager.shared.isProUnlocked { return }
        loadTodayUsage()
        todayUsageCount += 1
        saveTodayUsage()
    }

    func canUse() -> Bool {
        if PurchaseManager.shared.isProUnlocked { return true }
        loadTodayUsage()
        return todayUsageCount < Self.dailyLimit
    }

    private func loadTodayUsage() {
        let savedDate = UserDefaults.standard.string(forKey: dateKey) ?? ""
        let today = todayString()

        if savedDate == today {
            todayUsageCount = UserDefaults.standard.integer(forKey: countKey)
        } else {
            todayUsageCount = 0
            UserDefaults.standard.set(today, forKey: dateKey)
            UserDefaults.standard.set(0, forKey: countKey)
        }
    }

    private func saveTodayUsage() {
        UserDefaults.standard.set(todayString(), forKey: dateKey)
        UserDefaults.standard.set(todayUsageCount, forKey: countKey)
    }

    private func todayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }
}
