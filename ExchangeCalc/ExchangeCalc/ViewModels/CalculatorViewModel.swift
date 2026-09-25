import Foundation
import SwiftData

@MainActor
@Observable
final class CalculatorViewModel: Sendable {
    let inputState = InputState()
    let exchangeRateService = ExchangeRateService.shared
    let settings = AppSettings.shared

    var fromCurrency: Currency {
        didSet { settings.lastFromCurrency = fromCurrency }
    }
    var toCurrency: Currency {
        didSet { settings.lastToCurrency = toCurrency }
    }

    var showSavedFeedback: Bool = false
    var showFromCurrencyPicker: Bool = false
    var showToCurrencyPicker: Bool = false

    // Manual rate
    var manualRateEnabled: Bool = false
    var manualRateText: String = ""

    private var manualRate: Double? {
        guard manualRateEnabled, !manualRateText.isEmpty else { return nil }
        return Double(manualRateText)
    }

    private var effectiveRate: Double? {
        if let manual = manualRate, manual > 0 {
            return manual
        }
        guard let fromRate = exchangeRateService.currentRates[fromCurrency.rawValue],
              let toRate = exchangeRateService.currentRates[toCurrency.rawValue],
              fromRate > 0 else { return nil }
        return toRate / fromRate
    }

    var conversionResult: ConversionResult? {
        let amount = inputState.inputAmount
        guard amount > 0, let rate = effectiveRate else { return nil }

        if manualRateEnabled, manualRate != nil {
            let convertedAmount = amount * Decimal(rate)
            let roundingBehavior = NSDecimalNumberHandler(
                roundingMode: .plain,
                scale: Int16(toCurrency.decimalPlaces),
                raiseOnExactness: false,
                raiseOnOverflow: false,
                raiseOnUnderflow: false,
                raiseOnDivideByZero: false
            )
            let rounded = (convertedAmount as NSDecimalNumber).rounding(accordingToBehavior: roundingBehavior) as Decimal
            return ConversionResult(
                inputAmount: amount,
                convertedAmount: rounded,
                fromCurrency: fromCurrency,
                toCurrency: toCurrency,
                exchangeRate: rate,
                lastUpdated: nil
            )
        }

        return exchangeRateService.convert(amount: amount, from: fromCurrency, to: toCurrency)
    }

    var rateDisplay: String? {
        guard let rate = effectiveRate else { return nil }
        return "1 \(fromCurrency.rawValue) = \(rate.rateFormatted()) \(toCurrency.rawValue)"
    }

    var lastUpdatedDisplay: String? {
        if manualRateEnabled, manualRate != nil {
            return String(localized: "manual_rate")
        }
        guard let date = exchangeRateService.lastUpdated else { return nil }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    init() {
        // Restore last used currency pair
        fromCurrency = settings.lastFromCurrency
        toCurrency = settings.lastToCurrency
    }

    // MARK: - Keypad Actions

    func appendDigit(_ digit: String) {
        inputState.appendDigit(digit, maxDecimalPlaces: fromCurrency.decimalPlaces)
    }

    func appendDoubleZero() {
        inputState.appendDoubleZero(maxDecimalPlaces: fromCurrency.decimalPlaces)
    }

    func appendDecimalPoint() {
        inputState.appendDecimalPoint(maxDecimalPlaces: fromCurrency.decimalPlaces)
    }

    func deleteLastCharacter() {
        inputState.deleteLastCharacter()
    }

    func clear() {
        inputState.clear()
    }

    // MARK: - Currency Actions

    func swapCurrencies() {
        let temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        inputState.clear()
    }

    func selectFromCurrency(_ currency: Currency) {
        guard currency != toCurrency else { return }
        fromCurrency = currency
        inputState.clear()
        showFromCurrencyPicker = false
    }

    func selectToCurrency(_ currency: Currency) {
        guard currency != fromCurrency else { return }
        toCurrency = currency
        showToCurrencyPicker = false
    }

    // MARK: - History

    func saveToHistory(modelContext: ModelContext) {
        guard let result = conversionResult, result.inputAmount > 0 else { return }

        let history = ConversionHistory(
            inputAmount: "\(result.inputAmount)",
            convertedAmount: "\(result.convertedAmount)",
            fromCurrency: result.fromCurrency,
            toCurrency: result.toCurrency,
            exchangeRate: result.exchangeRate
        )
        modelContext.insert(history)
        showSavedFeedback = true
        ReviewManager.shared.recordSave()
    }

    func restoreFromHistory(_ history: ConversionHistory) {
        if let from = history.fromCurrency {
            fromCurrency = from
        }
        if let to = history.toCurrency {
            toCurrency = to
        }
        inputState.clear()
        for char in history.inputAmount {
            if char == "." {
                inputState.appendDecimalPoint(maxDecimalPlaces: fromCurrency.decimalPlaces)
            } else {
                inputState.appendDigit(String(char), maxDecimalPlaces: fromCurrency.decimalPlaces)
            }
        }
    }

    // MARK: - Rate Refresh

    func refreshRates() async {
        await exchangeRateService.fetchRatesIfNeeded()
    }

    func forceRefreshRates() async {
        await exchangeRateService.forceRefresh()
    }

    // MARK: - Manual Rate

    func toggleManualRate(_ enabled: Bool) {
        manualRateEnabled = enabled
        if !enabled {
            manualRateText = ""
        } else {
            if let rate = effectiveRate {
                manualRateText = String(format: "%.4f", rate)
            }
        }
    }

    func setManualRate(_ rate: Double) {
        manualRateText = String(format: "%.4f", rate)
    }

    func dismissSavedFeedback() {
        showSavedFeedback = false
    }
}
