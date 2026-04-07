import SwiftUI
import Charts

struct RateChartView: View {
    let fromCurrency: Currency
    let toCurrency: Currency
    let onBack: () -> Void

    @State private var dataPoints: [ChartPoint] = []
    @State private var isLoading = false
    @State private var selectedPoint: ChartPoint?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                pairHeader

                if isLoading {
                    Spacer()
                    ProgressView(LocaleManager.shared.localized("loading_chart"))
                    Spacer()
                } else if dataPoints.isEmpty {
                    Spacer()
                    Text(LocaleManager.shared.localized("no_chart_data"))
                        .foregroundStyle(.secondary)
                    Spacer()
                } else {
                    summarySection
                        .padding(.horizontal, 16)
                        .padding(.top, 12)

                    chartSection
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                    if let point = selectedPoint {
                        selectedDetail(point)
                            .padding(.horizontal, 16)
                            .padding(.top, 8)
                    }

                    Spacer()
                }
            }
            .navigationTitle(LocaleManager.shared.localized("rate_chart"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(LocaleManager.shared.localized("back")) { onBack() }
                }
            }
        }
        .task {
            await loadData()
        }
    }

    // MARK: - Pair Header

    private var pairHeader: some View {
        HStack(spacing: 8) {
            Text(fromCurrency.flag).font(.title2)
            Text(fromCurrency.rawValue).font(.headline)
            Image(systemName: "arrow.right").foregroundStyle(.secondary)
            Text(toCurrency.flag).font(.title2)
            Text(toCurrency.rawValue).font(.headline)
            Spacer()
            Text(LocaleManager.shared.localized("days_90"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color(.systemGray6))
                .clipShape(Capsule())
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    // MARK: - Summary

    private var summarySection: some View {
        let rates = dataPoints.map(\.rate)
        let current = rates.last ?? 0
        let min = rates.min() ?? 0
        let max = rates.max() ?? 0
        let first = rates.first ?? 0
        let change = first > 0 ? ((current - first) / first) * 100 : 0

        return HStack(spacing: 16) {
            summaryItem(label: LocaleManager.shared.localized("current"), value: String(format: "%.3f", current))
            summaryItem(label: LocaleManager.shared.localized("high"), value: String(format: "%.3f", max))
            summaryItem(label: LocaleManager.shared.localized("low"), value: String(format: "%.3f", min))
            summaryItem(
                label: LocaleManager.shared.localized("change"),
                value: String(format: "%+.1f%%", change),
                color: change >= 0 ? .green : .red
            )
        }
    }

    private func summaryItem(label: String, value: String, color: Color = .primary) -> some View {
        VStack(spacing: 2) {
            Text(label).font(.system(size: 10)).foregroundStyle(.secondary)
            Text(value).font(.caption.weight(.bold)).foregroundStyle(color)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Chart (no bleed below axis)

    private var chartSection: some View {
        Chart {
            ForEach(dataPoints) { point in
                LineMark(
                    x: .value("Date", point.dateObject),
                    y: .value("Rate", point.rate)
                )
                .foregroundStyle(AppTheme.primary)
                .lineStyle(StrokeStyle(lineWidth: 2))

                AreaMark(
                    x: .value("Date", point.dateObject),
                    yStart: .value("Min", chartYMin),
                    yEnd: .value("Rate", point.rate)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [AppTheme.primary.opacity(0.15), AppTheme.primary.opacity(0.0)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
            }

            if let selected = selectedPoint {
                RuleMark(x: .value("Date", selected.dateObject))
                    .foregroundStyle(.secondary.opacity(0.5))
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [4]))

                PointMark(
                    x: .value("Date", selected.dateObject),
                    y: .value("Rate", selected.rate)
                )
                .foregroundStyle(AppTheme.swapAccent)
                .symbolSize(60)
            }
        }
        .chartYScale(domain: chartYMin...chartYMax)
        .chartXAxis {
            AxisMarks(values: .stride(by: .day, count: 15)) { _ in
                AxisGridLine()
                AxisValueLabel(format: .dateTime.month(.abbreviated).day())
            }
        }
        .chartYAxis {
            AxisMarks(position: .trailing)
        }
        .chartOverlay { proxy in
            GeometryReader { geo in
                Rectangle()
                    .fill(Color.clear)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                guard let plotFrame = proxy.plotFrame else { return }
                                let x = value.location.x - geo[plotFrame].origin.x
                                if let date: Date = proxy.value(atX: x) {
                                    selectedPoint = closestPoint(to: date)
                                }
                            }
                    )
            }
        }
        .frame(height: 220)
    }

    private var chartYMin: Double {
        let rates = dataPoints.map(\.rate)
        return (rates.min() ?? 0) * 0.998
    }

    private var chartYMax: Double {
        let rates = dataPoints.map(\.rate)
        return (rates.max() ?? 1) * 1.002
    }

    private func closestPoint(to date: Date) -> ChartPoint? {
        dataPoints.min(by: { abs($0.dateObject.timeIntervalSince(date)) < abs($1.dateObject.timeIntervalSince(date)) })
    }

    // MARK: - Selected Detail

    private func selectedDetail(_ point: ChartPoint) -> some View {
        HStack {
            Spacer()
            Text(point.date)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text("1 \(fromCurrency.rawValue) = \(String(format: "%.4f", point.rate)) \(toCurrency.rawValue)")
                .font(.subheadline.weight(.semibold))
        }
    }

    // MARK: - Data Loading (lightweight pair endpoint)

    private func loadData() async {
        isLoading = true
        let points = await ExchangeRateService.shared.fetchPairHistory(
            from: fromCurrency, to: toCurrency, days: 90
        )

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")

        dataPoints = points.compactMap { point in
            guard let date = formatter.date(from: point.date) else { return nil as ChartPoint? }
            return ChartPoint(date: point.date, rate: point.rate, dateObject: date)
        }
        isLoading = false
    }
}

struct ChartPoint: Identifiable {
    let id = UUID()
    let date: String
    let rate: Double
    let dateObject: Date
}
