import SwiftUI
import SwiftData

struct CalculatorView: View {
    @Bindable var viewModel: CalculatorViewModel
    @Environment(\.modelContext) private var modelContext
    let onShowHistory: () -> Void
    let onShowSettings: () -> Void
    let onShowChart: () -> Void

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                headerSection
                currencyPairSection
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                inputDisplaySection
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                DisplaySection(
                    result: viewModel.conversionResult,
                    rateDisplay: viewModel.rateDisplay,
                    lastUpdatedDisplay: viewModel.lastUpdatedDisplay,
                    isLoading: viewModel.exchangeRateService.isLoading,
                    hasRates: viewModel.exchangeRateService.hasRates,
                    manualRateEnabled: viewModel.manualRateEnabled,
                    manualRateText: viewModel.manualRateText,
                    fromCurrency: viewModel.fromCurrency,
                    toCurrency: viewModel.toCurrency,
                    onToggleManualRate: viewModel.toggleManualRate,
                    onManualRateChanged: viewModel.setManualRate
                )
                .padding(.horizontal, 16)
                .padding(.top, 8)

                Spacer(minLength: 4)

                AdBannerView()
                    .frame(height: 50)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 4)

                NumericKeypad(
                    onDigit: viewModel.appendDigit,
                    onDoubleZero: viewModel.appendDoubleZero,
                    onDecimalPoint: viewModel.appendDecimalPoint,
                    onBackspace: viewModel.deleteLastCharacter,
                    onClear: viewModel.clear,
                    onSaveHistory: { viewModel.saveToHistory(modelContext: modelContext) },
                    decimalEnabled: viewModel.fromCurrency.decimalPlaces > 0
                )
                .frame(height: geometry.size.height * 0.44)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
        .task { await viewModel.refreshRates() }
        .overlay {
            if viewModel.showSavedFeedback { savedFeedbackToast }
        }
        .sheet(isPresented: $viewModel.showFromCurrencyPicker) {
            CurrencySelector(
                title: String(localized: "from_currency"),
                excludeCurrency: viewModel.toCurrency,
                onSelect: viewModel.selectFromCurrency
            )
        }
        .sheet(isPresented: $viewModel.showToCurrencyPicker) {
            CurrencySelector(
                title: String(localized: "to_currency"),
                excludeCurrency: viewModel.fromCurrency,
                onSelect: viewModel.selectToCurrency
            )
        }
    }

    private var headerSection: some View {
        HStack {
            Text(String(localized: "app_title"))
                .font(.headline)
            Spacer()
            Menu {
                Button { onShowChart() } label: {
                    Label(String(localized: "rate_chart"), systemImage: "chart.line.uptrend.xyaxis")
                }
                Button { onShowHistory() } label: {
                    Label(String(localized: "history"), systemImage: "clock.arrow.circlepath")
                }
                Button { onShowSettings() } label: {
                    Label(String(localized: "settings"), systemImage: "gearshape")
                }
                Button { Task { await viewModel.forceRefreshRates() } } label: {
                    Label(String(localized: "refresh_rates"), systemImage: "arrow.clockwise")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.title3)
                    .foregroundStyle(AppTheme.primary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private var currencyPairSection: some View {
        HStack(spacing: 12) {
            Button { viewModel.showFromCurrencyPicker = true } label: {
                HStack(spacing: 6) {
                    Text(viewModel.fromCurrency.flag).font(.title2)
                    Text(viewModel.fromCurrency.rawValue).font(.headline)
                    Image(systemName: "chevron.down").font(.caption)
                }
                .padding(.horizontal, 12).padding(.vertical, 10)
                .background(AppTheme.primary.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .foregroundStyle(.primary)

            Button(action: viewModel.swapCurrencies) {
                Image(systemName: "arrow.left.arrow.right")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(AppTheme.swapAccent)
                    .clipShape(Circle())
            }

            Button { viewModel.showToCurrencyPicker = true } label: {
                HStack(spacing: 6) {
                    Text(viewModel.toCurrency.flag).font(.title2)
                    Text(viewModel.toCurrency.rawValue).font(.headline)
                    Image(systemName: "chevron.down").font(.caption)
                }
                .padding(.horizontal, 12).padding(.vertical, 10)
                .background(AppTheme.primary.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .foregroundStyle(.primary)

            Spacer()
        }
    }

    private var inputDisplaySection: some View {
        HStack(alignment: .bottom, spacing: 4) {
            Spacer()
            Text("\(viewModel.fromCurrency.symbol)\(viewModel.inputState.displayText)")
                .font(.system(size: 42, weight: .bold, design: .rounded))
                .lineLimit(1)
                .minimumScaleFactor(0.4)
        }
    }

    private var savedFeedbackToast: some View {
        VStack {
            Spacer()
            Text(String(localized: "saved"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(AppTheme.primary)
                .clipShape(Capsule())
                .padding(.bottom, 80)
        }
        .transition(.move(edge: .bottom).combined(with: .opacity))
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                withAnimation { viewModel.dismissSavedFeedback() }
            }
        }
    }
}
