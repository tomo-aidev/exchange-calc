import Foundation

@MainActor
@Observable
final class InputState: Sendable {
    private(set) var inputText: String = "0"
    private(set) var hasDecimalPoint: Bool = false

    var inputAmount: Decimal {
        Decimal(string: inputText) ?? 0
    }

    var displayText: String {
        if hasDecimalPoint && !inputText.contains(".") {
            return inputText + "."
        }
        return inputText
    }

    func appendDigit(_ digit: String, maxDecimalPlaces: Int) {
        if inputText.length >= 12 { return }

        if let dotIndex = inputText.firstIndex(of: ".") {
            let decimalCount = inputText.distance(from: inputText.index(after: dotIndex), to: inputText.endIndex)
            if decimalCount >= maxDecimalPlaces { return }
        }

        if inputText == "0" && digit != "0" && !hasDecimalPoint {
            inputText = digit
        } else if inputText == "0" && digit == "0" && !hasDecimalPoint {
            return
        } else {
            inputText += digit
        }
    }

    func appendDoubleZero(maxDecimalPlaces: Int) {
        if inputText == "0" && !hasDecimalPoint { return }
        if inputText.length >= 11 { return }

        if let dotIndex = inputText.firstIndex(of: ".") {
            let decimalCount = inputText.distance(from: inputText.index(after: dotIndex), to: inputText.endIndex)
            if decimalCount >= maxDecimalPlaces { return }
            let canAdd = maxDecimalPlaces - decimalCount
            if canAdd >= 2 {
                inputText += "00"
            } else if canAdd == 1 {
                inputText += "0"
            }
        } else {
            inputText += "00"
        }
    }

    func appendDecimalPoint(maxDecimalPlaces: Int) {
        guard maxDecimalPlaces > 0 else { return }
        guard !hasDecimalPoint else { return }
        hasDecimalPoint = true
        inputText += "."
    }

    func deleteLastCharacter() {
        if inputText.length <= 1 || (inputText.length == 2 && inputText.first == "-") {
            clear()
            return
        }
        if inputText.last == "." {
            hasDecimalPoint = false
        }
        inputText = String(inputText.dropLast())
    }

    func clear() {
        inputText = "0"
        hasDecimalPoint = false
    }
}

private extension String {
    var length: Int { count }
}
