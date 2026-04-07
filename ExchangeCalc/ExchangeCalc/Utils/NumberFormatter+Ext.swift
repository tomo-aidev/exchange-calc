import Foundation

extension Decimal {
    func formatted(for currency: Currency) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = currency.decimalPlaces
        formatter.groupingSeparator = ","
        formatter.groupingSize = 3
        formatter.usesGroupingSeparator = true

        let number = self as NSDecimalNumber
        return "\(currency.symbol)\(formatter.string(from: number) ?? "0")"
    }

    func rateFormatted() -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 4
        formatter.groupingSeparator = ","
        formatter.usesGroupingSeparator = true

        let number = self as NSDecimalNumber
        return formatter.string(from: number) ?? "0"
    }
}

extension Double {
    func rateFormatted() -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 4
        formatter.groupingSeparator = ","
        formatter.usesGroupingSeparator = true
        return formatter.string(from: NSNumber(value: self)) ?? "0"
    }
}
