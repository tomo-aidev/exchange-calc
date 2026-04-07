import SwiftUI

struct MainTabView: View {
    @State private var viewModel = CalculatorViewModel()
    @State private var currentScreen: Screen = .calculator

    enum Screen {
        case calculator, history, settings, chart
    }

    var body: some View {
        Group {
            switch currentScreen {
            case .calculator:
                CalculatorView(
                    viewModel: viewModel,
                    onShowHistory: { currentScreen = .history },
                    onShowSettings: { currentScreen = .settings },
                    onShowChart: { currentScreen = .chart }
                )

            case .history:
                HistoryView(
                    onBack: { currentScreen = .calculator },
                    onRestore: { history in
                        viewModel.restoreFromHistory(history)
                        currentScreen = .calculator
                    }
                )

            case .settings:
                SettingsView(onBack: { currentScreen = .calculator })

            case .chart:
                RateChartView(
                    fromCurrency: viewModel.fromCurrency,
                    toCurrency: viewModel.toCurrency,
                    onBack: { currentScreen = .calculator }
                )
            }
        }
    }
}
