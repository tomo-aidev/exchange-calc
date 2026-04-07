import Foundation

@MainActor
@Observable
final class ExchangeRateService: Sendable {
    static let shared = ExchangeRateService()

    // Workers URL - replace with your deployed Workers URL
    // During development, falls back to direct API
    private let workersBaseURL = "https://quickrate-api.getonnews.workers.dev"
    private let directLatestURL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
    private let directFallbackURL = "https://latest.currency-api.pages.dev/v1/currencies/usd.json"

    private let cacheKey = "cachedExchangeRates"
    private let historyCacheKey = "cachedHistoryDate"
    private let historyDataKey = "cachedHistoryData"
    private let refreshInterval: TimeInterval = 24 * 60 * 60

    private(set) var currentRates: [String: Double] = [:]
    private(set) var lastUpdated: Date?
    private(set) var rateDate: String?
    private(set) var isLoading: Bool = false
    private(set) var lastError: String?

    // History
    private(set) var historyData: [[String: Any]] = []
    private(set) var isLoadingHistory: Bool = false

    var hasRates: Bool { !currentRates.isEmpty }

    private init() {
        loadCachedRates()
    }

    // MARK: - Latest Rates

    func fetchRatesIfNeeded() async {
        if let lastUpdated, Date().timeIntervalSince(lastUpdated) < refreshInterval, hasRates {
            return
        }
        await forceRefresh()
    }

    func forceRefresh() async {
        guard !isLoading else { return }
        isLoading = true
        lastError = nil

        // Try Workers first, then direct API
        if let data = await fetchWorkersLatest() {
            applyRates(data)
            isLoading = false
            return
        }

        // Fallback: direct API
        for urlString in [directLatestURL, directFallbackURL] {
            if let data = await fetchDirectAPI(urlString) {
                applyRates(data)
                isLoading = false
                return
            }
        }

        isLoading = false
    }

    // MARK: - Pair History (lightweight)

    func fetchPairHistory(from: Currency, to: Currency, days: Int = 90) async -> [PairRatePoint] {
        let cacheKey = "pairHistory:\(from.rawValue):\(to.rawValue)"
        let today = todayString()

        // Check cache: same pair + same day
        if let cachedDate = UserDefaults.standard.string(forKey: "\(cacheKey):date"),
           cachedDate == today,
           let data = UserDefaults.standard.data(forKey: "\(cacheKey):data"),
           let cached = try? JSONDecoder().decode([PairRatePoint].self, from: data) {
            return cached
        }

        // Fetch from Workers
        isLoadingHistory = true
        let points = await fetchPairHistoryFromWorkers(from: from, to: to, days: days)
        isLoadingHistory = false

        if !points.isEmpty {
            if let data = try? JSONEncoder().encode(points) {
                UserDefaults.standard.set(data, forKey: "\(cacheKey):data")
                UserDefaults.standard.set(today, forKey: "\(cacheKey):date")
            }
        }

        return points
    }

    private func fetchPairHistoryFromWorkers(from: Currency, to: Currency, days: Int) async -> [PairRatePoint] {
        let urlStr = "\(workersBaseURL)/history?days=\(days)&from=\(from.rawValue)&to=\(to.rawValue)"
        guard let url = URL(string: urlStr) else { return [] }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return [] }
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let dataArray = json["data"] as? [[String: Any]] else { return [] }

            return dataArray.compactMap { item in
                guard let date = item["date"] as? String,
                      let rate = item["rate"] as? Double else { return nil }
                return PairRatePoint(date: date, rate: rate)
            }.sorted { $0.date < $1.date }
        } catch {
            return []
        }
    }

    // MARK: - Conversion

    func convert(amount: Decimal, from: Currency, to: Currency) -> ConversionResult? {
        guard let fromRate = currentRates[from.rawValue],
              let toRate = currentRates[to.rawValue],
              fromRate > 0 else {
            return nil
        }

        let exchangeRate = toRate / fromRate
        let convertedAmount = amount * Decimal(exchangeRate)

        let roundingBehavior = NSDecimalNumberHandler(
            roundingMode: .plain,
            scale: Int16(to.decimalPlaces),
            raiseOnExactness: false,
            raiseOnOverflow: false,
            raiseOnUnderflow: false,
            raiseOnDivideByZero: false
        )
        let rounded = (convertedAmount as NSDecimalNumber).rounding(accordingToBehavior: roundingBehavior) as Decimal

        return ConversionResult(
            inputAmount: amount,
            convertedAmount: rounded,
            fromCurrency: from,
            toCurrency: to,
            exchangeRate: exchangeRate,
            lastUpdated: lastUpdated
        )
    }

    // MARK: - Private: Workers API

    private func fetchWorkersLatest() async -> (rates: [String: Double], date: String)? {
        guard let url = URL(string: "\(workersBaseURL)/latest") else { return nil }
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return nil }
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let date = json["date"] as? String,
                  let rates = json["rates"] as? [String: Double] else { return nil }
            return (rates, date)
        } catch {
            return nil
        }
    }

    // MARK: - Private: Direct API

    private func fetchDirectAPI(_ urlString: String) async -> (rates: [String: Double], date: String)? {
        guard let url = URL(string: urlString) else { return nil }
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return nil }
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let date = json["date"] as? String,
                  let usdRates = json["usd"] as? [String: Double] else { return nil }

            var uppercased: [String: Double] = ["USD": 1]
            for (k, v) in usdRates where k.count == 3 {
                uppercased[k.uppercased()] = v
            }
            return (uppercased, date)
        } catch {
            lastError = error.localizedDescription
            return nil
        }
    }

    // MARK: - Private: Apply & Cache

    private func applyRates(_ data: (rates: [String: Double], date: String)) {
        currentRates = data.rates
        rateDate = data.date
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        lastUpdated = formatter.date(from: data.date) ?? Date()
        saveCachedRates()
    }

    private func loadCachedRates() {
        guard let data = UserDefaults.standard.data(forKey: cacheKey) else { return }
        do {
            let cached = try JSONDecoder().decode(CachedRates.self, from: data)
            currentRates = cached.rates
            lastUpdated = cached.lastUpdated
        } catch {}
    }

    private func saveCachedRates() {
        guard let lastUpdated else { return }
        let cached = CachedRates(rates: currentRates, lastUpdated: lastUpdated, fetchedAt: Date())
        if let data = try? JSONEncoder().encode(cached) {
            UserDefaults.standard.set(data, forKey: cacheKey)
        }
    }

    private func todayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: Date())
    }
}

// MARK: - Pair Rate Point

struct PairRatePoint: Codable, Sendable {
    let date: String
    let rate: Double
}
