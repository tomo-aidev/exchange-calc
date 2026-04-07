import Foundation
import SwiftData

@Model
final class ConversionHistory {
    var inputAmount: String
    var convertedAmount: String
    var fromCurrencyRaw: String
    var toCurrencyRaw: String
    var exchangeRate: Double
    var createdAt: Date
    var note: String

    var fromCurrency: Currency? {
        Currency(rawValue: fromCurrencyRaw)
    }

    var toCurrency: Currency? {
        Currency(rawValue: toCurrencyRaw)
    }

    init(
        inputAmount: String,
        convertedAmount: String,
        fromCurrency: Currency,
        toCurrency: Currency,
        exchangeRate: Double,
        createdAt: Date = Date(),
        note: String = ""
    ) {
        self.inputAmount = inputAmount
        self.convertedAmount = convertedAmount
        self.fromCurrencyRaw = fromCurrency.rawValue
        self.toCurrencyRaw = toCurrency.rawValue
        self.exchangeRate = exchangeRate
        self.createdAt = createdAt
        self.note = note
    }
}
