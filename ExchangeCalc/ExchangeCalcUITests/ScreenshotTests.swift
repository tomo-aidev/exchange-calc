import XCTest

@MainActor
final class ScreenshotTests: XCTestCase {

    private var app: XCUIApplication!
    private let baseDir = "/Volumes/AI/claude/exchange-calc-app/screenshots"

    override func setUp() {
        continueAfterFailure = true
    }

    private func launchWithLanguage(_ lang: String? = nil) {
        app = XCUIApplication()
        if let lang {
            app.launchArguments = ["-AppleLanguages", "(\"\(lang)\")", "-AppleLocale", lang]
        }
        app.launch()
        sleep(3)
    }

    func testCaptureZhHans() {
        app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\"zh-Hans\")", "-AppleLocale", "zh_CN"]
        app.launch()
        sleep(3)
        captureScreens(lang: "zh")
    }

    func testCaptureZhHant() {
        app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\"zh-Hant\")", "-AppleLocale", "zh_TW"]
        app.launch()
        sleep(3)
        captureScreens(lang: "zh-Hant")
    }

    func testCaptureAll() {
        launchWithLanguage()
        let lang = detectLanguage()
        captureScreens(lang: lang)
    }

    private func captureScreens(lang: String) {
        print("Detected language: \(lang)")

        // --- Screen 1: Type $100 ---
        tapButton(label: "1")
        tapButton(label: "0")
        tapButton(label: "00")
        sleep(1)
        saveScreenshot("\(lang)/1_calculator")

        // --- Clear for next screen ---
        tapButton(label: "C")
        usleep(500_000)

        // --- Screen 2: Open From currency picker ---
        let usdLabel = app.staticTexts["USD"].firstMatch
        if usdLabel.waitForExistence(timeout: 3) {
            usdLabel.tap()
            sleep(1)
            saveScreenshot("\(lang)/2_currency_selector")

            // Close the sheet: tap Close/閉じる button
            let closeBtn = app.buttons.allElementsBoundByIndex.first { btn in
                let l = btn.label
                return l.contains("Close") || l.contains("閉じる") || l.contains("닫기") ||
                       l.contains("关闭") || l.contains("關閉") || l.contains("ปิด") ||
                       l.contains("Đóng") || l.contains("Закрыть")
            }
            if let closeBtn, closeBtn.isHittable {
                closeBtn.tap()
            } else {
                app.swipeDown()
            }
            sleep(2)
        }

        // Ensure main screen is visible
        _ = app.buttons["C"].firstMatch.waitForExistence(timeout: 5)

        // --- Screen 3: Open Rate Chart ---
        openMenuAndTapChart()

        // Wait for chart to load (check for chart content or just wait longer)
        sleep(10)
        saveScreenshot("\(lang)/3_rate_chart")
    }

    private func detectLanguage() -> String {
        // Collect all visible text
        var allLabels: [String] = []
        for btn in app.buttons.allElementsBoundByIndex {
            allLabels.append(btn.label)
        }
        for t in app.staticTexts.allElementsBoundByIndex {
            allLabels.append(t.label)
        }
        let combined = allLabels.joined(separator: " ")

        // Check in specific order (most unique first)
        if combined.contains("Калькулятор") || combined.contains("валют") { return "ru" }
        if combined.contains("환전") || combined.contains("여행") { return "ko" }
        if combined.contains("แลกเงิน") || combined.contains("ท่องเที่ยว") { return "th" }
        if combined.contains("Đổi") || combined.contains("đổi tiền") || combined.contains("Du lịch") { return "vi" }
        if combined.contains("貨幣") || combined.contains("兌換") { return "zh-Hant" }
        if combined.contains("货币") || combined.contains("兑换") || combined.contains("旅行汇率") { return "zh" }
        if combined.contains("通貨") || combined.contains("両替") || combined.contains("さらに表示") { return "ja" }
        if combined.contains("TripRate") || combined.contains("More") { return "en" }

        // Last resort: use Locale
        let lang = Locale.current.language.languageCode?.identifier ?? "en"
        if lang.hasPrefix("zh") { return "zh" }
        return lang
    }

    private func openMenuAndTapChart() {
        // Find and tap menu button
        let menuButton = app.buttons.allElementsBoundByIndex.first {
            $0.label.contains("さらに表示") || $0.label == "More" || $0.label.contains("더보기") ||
            $0.label.contains("更多") || $0.label.contains("เพิ่มเติม") || $0.label.contains("Thêm") ||
            $0.label.contains("Ещё")
        }
        menuButton?.tap()
        sleep(1)

        // Tap the first menu item that looks like "Rate Chart"
        let chartItem = app.buttons.allElementsBoundByIndex.first { btn in
            guard btn.isHittable else { return false }
            let l = btn.label.lowercased()
            return l.contains("レート") || l.contains("rate") || l.contains("chart") ||
                   l.contains("차트") || l.contains("환율") || l.contains("走势") ||
                   l.contains("匯率") || l.contains("กราฟ") || l.contains("อัตรา") ||
                   l.contains("biểu") || l.contains("tỷ giá") || l.contains("график") || l.contains("курс")
        }
        chartItem?.tap()
    }

    private func tapButton(label: String) {
        let btn = app.buttons[label].firstMatch
        if btn.waitForExistence(timeout: 2) {
            btn.tap()
            usleep(300_000)
        }
    }

    private func saveScreenshot(_ name: String) {
        let screenshot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        let data = screenshot.pngRepresentation
        let path = "\(baseDir)/\(name).png"
        let dir = (path as NSString).deletingLastPathComponent
        try? FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true, attributes: nil)
        try? data.write(to: URL(fileURLWithPath: path))
    }
}
