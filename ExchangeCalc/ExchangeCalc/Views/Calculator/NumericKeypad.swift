import SwiftUI

struct NumericKeypad: View {
    let onDigit: (String) -> Void
    let onDoubleZero: () -> Void
    let onDecimalPoint: () -> Void
    let onBackspace: () -> Void
    let onClear: () -> Void
    let onSaveHistory: () -> Void
    let decimalEnabled: Bool

    var body: some View {
        VStack(spacing: 8) {
            // Row 1: ⌫, C, .
            HStack(spacing: 8) {
                KeypadButton(style: .function, action: onBackspace) {
                    Image(systemName: "delete.backward")
                        .font(.title2.weight(.medium))
                }
                KeypadButton(label: "C", style: .function, action: onClear)
                KeypadButton(label: ".", style: decimalEnabled ? .function : .disabled, action: onDecimalPoint)
            }

            // Row 2: 7, 8, 9
            HStack(spacing: 8) {
                KeypadButton(label: "7", style: .digit) { onDigit("7") }
                KeypadButton(label: "8", style: .digit) { onDigit("8") }
                KeypadButton(label: "9", style: .digit) { onDigit("9") }
            }

            // Row 3: 4, 5, 6
            HStack(spacing: 8) {
                KeypadButton(label: "4", style: .digit) { onDigit("4") }
                KeypadButton(label: "5", style: .digit) { onDigit("5") }
                KeypadButton(label: "6", style: .digit) { onDigit("6") }
            }

            // Row 4: 1, 2, 3
            HStack(spacing: 8) {
                KeypadButton(label: "1", style: .digit) { onDigit("1") }
                KeypadButton(label: "2", style: .digit) { onDigit("2") }
                KeypadButton(label: "3", style: .digit) { onDigit("3") }
            }

            // Row 5: Save, 0, 00
            HStack(spacing: 8) {
                KeypadButton(style: .primary, action: onSaveHistory) {
                    VStack(spacing: 2) {
                        Image(systemName: "tray.and.arrow.down")
                            .font(.body.weight(.medium))
                        Text(LocaleManager.shared.localized("save"))
                            .font(.caption.weight(.bold))
                    }
                }
                KeypadButton(label: "0", style: .digit) { onDigit("0") }
                KeypadButton(label: "00", style: .function, action: onDoubleZero)
            }
        }
    }
}

// MARK: - Keypad Button

struct KeypadButton<Content: View>: View {
    enum Style {
        case digit
        case function
        case primary
        case disabled
    }

    let style: Style
    let action: () -> Void
    let content: Content

    init(label: String, style: Style, action: @escaping () -> Void) where Content == Text {
        self.style = style
        self.action = action
        self.content = Text(label)
    }

    init(style: Style, action: @escaping () -> Void, @ViewBuilder content: () -> Content) {
        self.style = style
        self.action = action
        self.content = content()
    }

    private var backgroundColor: Color {
        switch style {
        case .digit: return AppTheme.keypadDark
        case .function: return AppTheme.keypadGray
        case .primary: return AppTheme.primary
        case .disabled: return AppTheme.keypadGray.opacity(0.4)
        }
    }

    private var foregroundColor: Color {
        switch style {
        case .digit: return .white
        case .function: return .black
        case .primary: return .white
        case .disabled: return .gray
        }
    }

    var body: some View {
        Button(action: {
            if style != .disabled { action() }
        }) {
            content
                .font(.title.weight(.medium))
                .foregroundStyle(foregroundColor)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(backgroundColor)
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(KeypadPressStyle())
        .disabled(style == .disabled)
    }
}

struct KeypadPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1.0)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.08), value: configuration.isPressed)
    }
}
