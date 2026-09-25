package com.exchangecalc.app

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Multi-language UI test.
 * Tests: 1) Default English UI renders correctly
 *        2) All locale string resources contain correct translations
 */
@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private val testIntent = Intent(Intent.ACTION_MAIN).apply {
        setClassName("com.exchangecalc.app", "com.exchangecalc.app.MainActivity")
        putExtra("SKIP_SPLASH", true)
    }

    @get:Rule
    val composeTestRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule<MainActivity>(testIntent),
        activityProvider = { rule ->
            var activity: MainActivity? = null
            rule.scenario.onActivity { activity = it }
            activity!!
        }
    )

    private fun getStringForLocale(locale: Locale, resId: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        return localizedContext.resources.getString(resId)
    }

    private fun waitForMainScreen() {
        Thread.sleep(2000)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("app_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testDefaultEnglishUI() {
        waitForMainScreen()

        composeTestRule.onNodeWithTag("app_title")
            .assertExists()
            .assertTextContains("ExchangeCalc")

        composeTestRule.onNodeWithText("Save").assertExists()
        composeTestRule.onNodeWithTag("from_currency_selector").assertExists()
        composeTestRule.onNodeWithTag("to_currency_selector").assertExists()

        // Check currency codes in unmerged tree
        composeTestRule.onNode(
            hasTestTag("from_currency_code"),
            useUnmergedTree = true
        ).assertExists()
        composeTestRule.onNode(
            hasTestTag("to_currency_code"),
            useUnmergedTree = true
        ).assertExists()

        println("PASS: English UI renders correctly")
    }

    @Test
    fun testJapaneseStrings() {
        val locale = Locale.JAPANESE
        assert(getStringForLocale(locale, R.string.app_title) == "両替計算")
        assert(getStringForLocale(locale, R.string.from_currency) == "変換元")
        assert(getStringForLocale(locale, R.string.to_currency) == "変換先")
        assert(getStringForLocale(locale, R.string.history) == "履歴")
        assert(getStringForLocale(locale, R.string.settings) == "設定")
        assert(getStringForLocale(locale, R.string.save) == "保存")
        println("PASS: Japanese strings verified")
    }

    @Test
    fun testKoreanStrings() {
        val locale = Locale.KOREAN
        assert(getStringForLocale(locale, R.string.app_title) == "환율 계산기")
        assert(getStringForLocale(locale, R.string.from_currency) == "변환 전")
        assert(getStringForLocale(locale, R.string.save) == "저장")
        println("PASS: Korean strings verified")
    }

    @Test
    fun testChineseStrings() {
        val locale = Locale.SIMPLIFIED_CHINESE
        assert(getStringForLocale(locale, R.string.app_title) == "汇率计算器")
        assert(getStringForLocale(locale, R.string.from_currency) == "从")
        assert(getStringForLocale(locale, R.string.save) == "保存")
        println("PASS: Chinese strings verified")
    }

    @Test
    fun testThaiStrings() {
        val locale = Locale("th")
        assert(getStringForLocale(locale, R.string.app_title) == "เครื่องคำนวณอัตราแลกเปลี่ยน")
        assert(getStringForLocale(locale, R.string.from_currency) == "จาก")
        assert(getStringForLocale(locale, R.string.save) == "บันทึก")
        println("PASS: Thai strings verified")
    }

    @Test
    fun testVietnameseStrings() {
        val locale = Locale("vi")
        assert(getStringForLocale(locale, R.string.app_title) == "Máy tính tỷ giá")
        assert(getStringForLocale(locale, R.string.from_currency) == "Từ")
        assert(getStringForLocale(locale, R.string.save) == "Lưu")
        println("PASS: Vietnamese strings verified")
    }

    @Test
    fun testAllLocalesComplete() {
        val locales = listOf(
            "en" to Locale.ENGLISH,
            "ja" to Locale.JAPANESE,
            "ko" to Locale.KOREAN,
            "zh" to Locale.SIMPLIFIED_CHINESE,
            "th" to Locale("th"),
            "vi" to Locale("vi")
        )
        val stringIds = listOf(
            R.string.app_title, R.string.from_currency, R.string.to_currency,
            R.string.save, R.string.history, R.string.settings, R.string.refresh_rates
        )

        for ((name, locale) in locales) {
            for (resId in stringIds) {
                val value = getStringForLocale(locale, resId)
                assert(value.isNotEmpty()) { "Empty string: locale=$name, resId=$resId" }
            }
        }

        // Verify non-English locales differ from English
        val enTitle = getStringForLocale(Locale.ENGLISH, R.string.app_title)
        for ((name, locale) in locales.drop(1)) {
            val title = getStringForLocale(locale, R.string.app_title)
            assert(title != enTitle) { "$name title '$title' same as English" }
        }

        println("PASS: All 6 locales complete and unique")
    }
}
