package org.hertsig.steam.service

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptException
import org.openqa.selenium.WebElement
import org.openqa.selenium.WindowType
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

interface SeleniumContext {
    val driver: RemoteWebDriver
    val actions: Actions

    fun click(element: WebElement) = actions.click(element).perform()
    fun click(by: By) = actions.click(driver.findElement(by)).perform()
    fun pause(amount: Long = 1, unit: TemporalUnit = ChronoUnit.SECONDS) = actions.pause(Duration.of(amount, unit)).perform()
    fun type(by: By, text: String, vararg args: Any) = type(driver.findElement(by), text, *args)
    fun type(element: WebElement, text: String, vararg args: Any) = actions.sendKeys(element, String.format(text, args)).perform()

    val WebElement.safeIsDisplayed get() = try {
        // TODO find out why this can fail
        isDisplayed
    } catch (e: JavascriptException) {
        false
    }

    fun <T> inSeparateTab(url: String, function: SeleniumContext.() -> T): T {
        val currentWindow = driver.windowHandle
        driver.switchTo().newWindow(WindowType.TAB)
        driver.navigate().to(url)
        try {
            return function()
        } finally {
            driver.close()
            driver.switchTo().window(currentWindow)
        }
    }
}
