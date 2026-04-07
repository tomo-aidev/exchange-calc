import SwiftUI

struct DisplaySection: View {
    let result: ConversionResult?
    let rateDisplay: String?
    let lastUpdatedDisplay: String?
    let isLoading: Bool
    let hasRates: Bool
    let manualRateEnabled: Bool
    let manualRateText: String
    let fromCurrency: Currency
    let toCurrency: Currency
    let onToggleManualRate: (Bool) -> Void
    let onManualRateChanged: (Double) -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            // Converted amount
            if let result {
                Text(result.convertedAmount.formatted(for: result.toCurrency))
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(AppTheme.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            } else {
                Text(isLoading ? LocaleManager.shared.localized("loading_rates") : (hasRates ? "-" : LocaleManager.shared.localized("no_rates")))
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(.secondary)
            }

            // Exchange rate
            if let rateDisplay {
                Text(rateDisplay)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            // Last updated
            if let lastUpdatedDisplay {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.caption2)
                    Text(lastUpdatedDisplay)
                        .font(.caption2)
                }
                .foregroundStyle(.tertiary)
            }

            // Disclaimer
            Text(LocaleManager.shared.localized("rate_disclaimer"))
                .font(.system(size: 9))
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.trailing)

            // Manual Rate section
            Divider()

            HStack {
                Text(LocaleManager.shared.localized("manual_rate"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Toggle("", isOn: Binding(
                    get: { manualRateEnabled },
                    set: { onToggleManualRate($0) }
                ))
                .labelsHidden()
                .tint(AppTheme.swapAccent)
            }

            if manualRateEnabled {
                RateDrumRollPicker(
                    fromCurrency: fromCurrency,
                    toCurrency: toCurrency,
                    initialRate: manualRateText,
                    onRateChanged: onManualRateChanged
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .padding(.vertical, 16)
        .padding(.horizontal, 20)
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
        }
    }
}

// MARK: - Rate Drum Roll Picker (XXX.XXX format)

struct RateDrumRollPicker: View {
    let fromCurrency: Currency
    let toCurrency: Currency
    let initialRate: String
    let onRateChanged: (Double) -> Void

    // 6 digits: d0 d1 d2 . d3 d4 d5
    @State private var d0: Int = 0  // hundreds
    @State private var d1: Int = 0  // tens
    @State private var d2: Int = 0  // ones
    @State private var d3: Int = 0  // tenths
    @State private var d4: Int = 0  // hundredths
    @State private var d5: Int = 0  // thousandths

    private let pickerHeight: CGFloat = 100

    var body: some View {
        VStack(spacing: 4) {
            Text("1 \(fromCurrency.rawValue) =")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .trailing)

            HStack(spacing: 0) {
                Spacer()

                // Integer part: 3 digits
                digitWheel($d0)
                digitWheel($d1)
                digitWheel($d2)

                // Decimal point
                Text(".")
                    .font(.title2.weight(.bold))
                    .frame(width: 12)

                // Decimal part: 3 digits
                digitWheel($d3)
                digitWheel($d4)
                digitWheel($d5)

                Text(" \(toCurrency.rawValue)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)
            }
        }
        .onAppear { parseRate() }
        .onChange(of: d0) { emitRate() }
        .onChange(of: d1) { emitRate() }
        .onChange(of: d2) { emitRate() }
        .onChange(of: d3) { emitRate() }
        .onChange(of: d4) { emitRate() }
        .onChange(of: d5) { emitRate() }
    }

    private func digitWheel(_ selection: Binding<Int>) -> some View {
        Picker("", selection: selection) {
            ForEach(0..<10, id: \.self) { i in
                Text("\(i)")
                    .font(.system(size: 20, weight: .medium, design: .monospaced))
                    .tag(i)
            }
        }
        .pickerStyle(.wheel)
        .frame(width: 36, height: pickerHeight)
        .clipped()
    }

    private func parseRate() {
        guard let value = Double(initialRate), value >= 0 else { return }
        let intPart = Int(value) % 1000  // max 999
        d0 = (intPart / 100) % 10
        d1 = (intPart / 10) % 10
        d2 = intPart % 10

        let decPart = value - Double(Int(value))
        let decDigits = Int(round(decPart * 1000))
        d3 = (decDigits / 100) % 10
        d4 = (decDigits / 10) % 10
        d5 = decDigits % 10
    }

    private func emitRate() {
        let intValue = Double(d0 * 100 + d1 * 10 + d2)
        let decValue = Double(d3) * 0.1 + Double(d4) * 0.01 + Double(d5) * 0.001
        onRateChanged(intValue + decValue)
    }
}
