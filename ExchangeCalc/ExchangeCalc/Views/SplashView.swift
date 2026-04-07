import SwiftUI

struct SplashView: View {
    let onFinished: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "arrow.left.arrow.right.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(AppTheme.primary)

            Text(String(localized: "app_title"))
                .font(.title.weight(.bold))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                withAnimation { onFinished() }
            }
        }
    }
}
