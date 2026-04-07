import SwiftUI
import StoreKit

struct PaywallView: View {
    let onDismiss: () -> Void
    let onPurchased: () -> Void

    @State private var isPurchasing = false
    @State private var isRestoring = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            // Icon
            Image(systemName: "arrow.left.arrow.right.circle.fill")
                .font(.system(size: 60))
                .foregroundStyle(AppTheme.primary)

            // Title
            Text(String(localized: "paywall_title"))
                .font(.title2.weight(.bold))
                .multilineTextAlignment(.center)

            // Feature
            VStack(spacing: 8) {
                FeatureRow(icon: "infinity", text: String(localized: "paywall_feature_unlimited"))
                FeatureRow(icon: "bolt.fill", text: String(localized: "paywall_feature_fast"))
            }
            .padding(.horizontal, 32)

            // Price
            if let product = PurchaseManager.shared.product {
                Text(product.displayPrice)
                    .font(.title.weight(.bold))
                    .foregroundStyle(AppTheme.primary)

                Text(String(localized: "paywall_one_time"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            // Purchase Button
            Button {
                isPurchasing = true
                Task {
                    let success = await PurchaseManager.shared.purchase()
                    isPurchasing = false
                    if success { onPurchased() }
                }
            } label: {
                HStack {
                    if isPurchasing {
                        ProgressView()
                            .tint(.white)
                    }
                    Text(String(localized: "paywall_purchase"))
                        .font(.headline)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(AppTheme.primary)
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .disabled(isPurchasing || isRestoring)
            .padding(.horizontal, 32)

            // Restore
            Button {
                isRestoring = true
                Task {
                    let success = await PurchaseManager.shared.restorePurchases()
                    isRestoring = false
                    if success { onPurchased() }
                    else { errorMessage = String(localized: "restore_failed") }
                }
            } label: {
                Text(String(localized: "restore_purchase"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .disabled(isPurchasing || isRestoring)

            Spacer()

            // Close
            Button(String(localized: "close")) {
                onDismiss()
            }
            .padding(.bottom, 20)
        }
        .task {
            await PurchaseManager.shared.loadProduct()
        }
        .alert("Error", isPresented: .init(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.primary)
                .frame(width: 24)
            Text(text)
                .font(.subheadline)
            Spacer()
        }
    }
}
