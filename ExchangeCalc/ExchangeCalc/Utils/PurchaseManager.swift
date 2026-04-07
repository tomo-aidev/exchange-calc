import Foundation
import StoreKit

@MainActor
@Observable
final class PurchaseManager: Sendable {
    static let shared = PurchaseManager()

    private let productID = "com.exchangecalc.app.unlock"
    private let purchasedKey = "isProUnlocked"

    private(set) var isProUnlocked: Bool = false
    private(set) var product: Product?
    private(set) var purchaseError: String?

    private var updateTask: Task<Void, Never>?

    private init() {
        isProUnlocked = UserDefaults.standard.bool(forKey: purchasedKey)
    }

    func startObserving() {
        updateTask = Task {
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    if transaction.productID == productID {
                        unlockPro()
                        await transaction.finish()
                    }
                }
            }
        }
    }

    func loadProduct() async {
        do {
            let products = try await Product.products(for: [productID])
            product = products.first
        } catch {
            purchaseError = error.localizedDescription
        }
    }

    func purchase() async -> Bool {
        guard let product else { return false }
        purchaseError = nil

        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    unlockPro()
                    await transaction.finish()
                    return true
                }
            case .userCancelled:
                break
            case .pending:
                break
            @unknown default:
                break
            }
        } catch {
            purchaseError = error.localizedDescription
        }
        return false
    }

    func restorePurchases() async -> Bool {
        do {
            try await AppStore.sync()
            for await result in Transaction.currentEntitlements {
                if case .verified(let transaction) = result {
                    if transaction.productID == productID {
                        unlockPro()
                        return true
                    }
                }
            }
        } catch {
            purchaseError = error.localizedDescription
        }
        return false
    }

    func checkExistingPurchases() async {
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                if transaction.productID == productID {
                    unlockPro()
                }
            }
        }
    }

    private func unlockPro() {
        isProUnlocked = true
        UserDefaults.standard.set(true, forKey: purchasedKey)
    }
}
