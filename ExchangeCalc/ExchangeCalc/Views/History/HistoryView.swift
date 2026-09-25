import SwiftUI
import SwiftData

struct HistoryView: View {
    @Query(sort: \ConversionHistory.createdAt, order: .reverse)
    private var histories: [ConversionHistory]
    @Environment(\.modelContext) private var modelContext

    let onBack: () -> Void
    let onRestore: (ConversionHistory) -> Void

    var body: some View {
        NavigationStack {
            Group {
                if histories.isEmpty {
                    ContentUnavailableView(
                        String(localized: "no_history"),
                        systemImage: "clock.arrow.circlepath",
                        description: Text(String(localized: "no_history_description"))
                    )
                } else {
                    List {
                        ForEach(histories) { history in
                            HistoryItemRow(history: history)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    onRestore(history)
                                }
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                modelContext.delete(histories[index])
                            }
                        }
                    }
                }
            }
            .navigationTitle(String(localized: "history"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "back")) {
                        onBack()
                    }
                }
                if !histories.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(String(localized: "delete_all"), role: .destructive) {
                            for history in histories {
                                modelContext.delete(history)
                            }
                        }
                    }
                }
            }
        }
    }
}

struct HistoryItemRow: View {
    let history: ConversionHistory

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                if let from = history.fromCurrency, let to = history.toCurrency {
                    Text("\(from.flag) \(from.rawValue) \u{2192} \(to.flag) \(to.rawValue)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(history.createdAt, style: .relative)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }

            HStack(alignment: .firstTextBaseline) {
                if let from = history.fromCurrency {
                    Text("\(from.symbol)\(history.inputAmount)")
                        .font(.headline)
                }
                Image(systemName: "arrow.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let to = history.toCurrency {
                    Text("\(to.symbol)\(history.convertedAmount)")
                        .font(.headline)
                        .foregroundStyle(AppTheme.primary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
