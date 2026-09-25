import XCTest

final class ScreenshotTests: XCTestCase {

    let app = XCUIApplication()
    let screenshotDir = "/Volumes/AI/claude/exchange-calc-app/screenshots"

    override func setUp() {
        continueAfterFailure = true
        app.launch()
        sleep(2) // Wait for rates to load
    }

    func testCaptureEnglish() {
        captureAllScreens(lang: "en")
    }

    func testCaptureJapanese() {
        captureAllScreens(lang: "ja")
    }

    private func captureAllScreens(lang: String) {
        // Screen 1: $100 input (USD > JPY)
        tapKeypadButton("1")
        tapKeypadButton("0")
        tapKeypadButton("0")
        sleep(1)

        saveScreenshot(name: "\(lang)/1_calculator")

        // Screen 2: From currency selector
        // Tap the USD button to open from-currency picker
        let usdButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'USD'")).firstMatch
        if usdButton.exists {
            usdButton.tap()
            sleep(1)
            saveScreenshot(name: "\(lang)/2_currency_selector")

            // Close the sheet
            let closeButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'close' OR label CONTAINS '閉じる'")).firstMatch
            if closeButton.exists { closeButton.tap() }
            else { app.swipeDown() }
            sleep(1)
        }

        // Screen 3: Rate chart
        let menuButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'ellipsis'")).firstMatch
        if menuButton.exists {
            menuButton.tap()
            sleep(0.5)

            let chartButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'chart' OR label CONTAINS 'レート推移'")).firstMatch
            if chartButton.exists {
                chartButton.tap()
                sleep(5) // Wait for chart data
                saveScreenshot(name: "\(lang)/3_rate_chart")
            }
        }
    }

    private func tapKeypadButton(_ label: String) {
        let button = app.buttons[label]
        if button.exists {
            button.tap()
            usleep(200_000)
        }
    }

    private func saveScreenshot(name: String) {
        let screenshot = app.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        // Also save to disk
        let data = screenshot.pngRepresentation
        let path = "\(screenshotDir)/\(name).png"
        let dirPath = (path as NSString).deletingLastPathComponent
        try? FileManager.default.createDirectory(atPath: dirPath, withIntermediateDirectories: true)
        try? data.write(to: URL(fileURLWithPath: path))
    }
}
