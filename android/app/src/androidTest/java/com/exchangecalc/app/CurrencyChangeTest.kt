package com.exchangecalc.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Currency change UI test.
 * Verifies currency pair selection, swap, and picker behavior.
 */
@RunWith(AndroidJUnit4::class)
class CurrencyChangeTest {

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

    private fun waitForMainScreen() {
        Thread.sleep(2000)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("from_currency_selector").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun assertFromCurrency(code: String) {
        composeTestRule.onNode(
            hasTestTag("from_currency_code") and hasText(code),
            useUnmergedTree = true
        ).assertExists()
    }

    private fun assertToCurrency(code: String) {
        composeTestRule.onNode(
            hasTestTag("to_currency_code") and hasText(code),
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun testDefaultCurrencyPairIsUsdJpy() {
        waitForMainScreen()

        assertFromCurrency("USD")
        assertToCurrency("JPY")

        println("PASS: Default currency pair USD/JPY verified")
    }

    @Test
    fun testSelectFromCurrency() {
        waitForMainScreen()

        composeTestRule.onNodeWithTag("from_currency_selector").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_EUR", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("currency_item_EUR", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        assertFromCurrency("EUR")

        println("PASS: From currency changed to EUR")
    }

    @Test
    fun testSelectToCurrency() {
        waitForMainScreen()

        composeTestRule.onNodeWithTag("to_currency_selector").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_KRW", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("currency_item_KRW", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        assertToCurrency("KRW")

        println("PASS: To currency changed to KRW")
    }

    @Test
    fun testSwapCurrencies() {
        waitForMainScreen()

        assertFromCurrency("USD")
        assertToCurrency("JPY")

        composeTestRule.onNodeWithTag("swap_button").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        assertFromCurrency("JPY")
        assertToCurrency("USD")

        println("PASS: Currencies swapped JPY/USD")
    }

    @Test
    fun testCurrencyPickerExcludesPairedCurrency() {
        waitForMainScreen()

        composeTestRule.onNodeWithTag("from_currency_selector").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_EUR", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("currency_item_JPY", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("currency_item_EUR", useUnmergedTree = true).assertExists()

        println("PASS: Picker correctly excludes paired currency")
    }

    @Test
    fun testSelectThenSwap() {
        waitForMainScreen()

        // Change from to EUR
        composeTestRule.onNodeWithTag("from_currency_selector").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_EUR", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("currency_item_EUR", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Swap EUR -> JPY becomes JPY -> EUR
        composeTestRule.onNodeWithTag("swap_button").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        assertFromCurrency("JPY")
        assertToCurrency("EUR")

        println("PASS: Select EUR then swap -> JPY/EUR")
    }

    @Test
    fun testMultipleCurrencyChanges() {
        waitForMainScreen()

        // from: USD -> GBP
        composeTestRule.onNodeWithTag("from_currency_selector").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_GBP", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("currency_item_GBP", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        assertFromCurrency("GBP")

        // to: JPY -> THB
        composeTestRule.onNodeWithTag("to_currency_selector").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("currency_item_THB", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("currency_item_THB", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)
        assertToCurrency("THB")

        // Swap: GBP/THB -> THB/GBP
        composeTestRule.onNodeWithTag("swap_button").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        assertFromCurrency("THB")
        assertToCurrency("GBP")

        println("PASS: Multiple changes GBP/THB -> THB/GBP")
    }
}
