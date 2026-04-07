import Foundation

struct CurrencyApiResponse: Sendable {
    let date: String
    let rates: [String: Double]
}

struct CachedRates: Codable, Sendable {
    let rates: [String: Double]
    let lastUpdated: Date
    let fetchedAt: Date
}
