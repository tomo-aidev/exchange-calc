import SwiftUI

struct CurrencySelector: View {
    let title: String
    let excludeCurrency: Currency?
    let onSelect: (Currency) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(Currency.allCases, id: \.self) { currency in
                    if currency != excludeCurrency {
                        Button {
                            onSelect(currency)
                            dismiss()
                        } label: {
                            HStack(spacing: 12) {
                                Text(currency.flag)
                                    .font(.title2)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(currency.rawValue)
                                        .font(.headline)
                                        .foregroundStyle(.primary)
                                    Text(LocaleManager.shared.localized(currency.nameKey))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }

                                Spacer()

                                Text(currency.symbol)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(LocaleManager.shared.localized("close")) {
                        dismiss()
                    }
                }
            }
        }
    }
}
