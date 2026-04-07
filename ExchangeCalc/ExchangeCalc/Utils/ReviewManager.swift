import Foundation
import StoreKit

@MainActor
final class ReviewManager: Sendable {
    static let shared = ReviewManager()

    private let totalSavesKey = "totalSaveCount"
    private let lastReviewDateKey = "lastReviewRequestDate"
    private let thresholds = [5, 15, 35]
    private let minimumDaysBetweenRequests = 14

    private init() {}

    func recordSave() {
        let current = UserDefaults.standard.integer(forKey: totalSavesKey)
        let newCount = current + 1
        UserDefaults.standard.set(newCount, forKey: totalSavesKey)

        if shouldRequestReview(saveCount: newCount) {
            requestReview()
        }
    }

    func requestReviewManually() {
        requestReview()
    }

    private func shouldRequestReview(saveCount: Int) -> Bool {
        guard thresholds.contains(saveCount) else { return false }

        if let lastDate = UserDefaults.standard.object(forKey: lastReviewDateKey) as? Date {
            let daysSince = Calendar.current.dateComponents([.day], from: lastDate, to: Date()).day ?? 0
            if daysSince < minimumDaysBetweenRequests {
                return false
            }
        }

        return true
    }

    private func requestReview() {
        UserDefaults.standard.set(Date(), forKey: lastReviewDateKey)
        if let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
            AppStore.requestReview(in: scene)
        }
    }
}
