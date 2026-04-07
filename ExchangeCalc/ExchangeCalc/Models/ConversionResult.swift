import Foundation

struct ConversionResult: Sendable {
    let inputAmount: Decimal
    let convertedAmount: Decimal
    let fromCurrency: Currency
    let toCurrency: Currency
    let exchangeRate: Double
    let lastUpdated: Date?
}
