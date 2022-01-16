package org.hertsig.steam.service

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class SeleniumService(
    private val profilePath: String?,
    private val implicitWait: Duration = Duration.of(10, ChronoUnit.SECONDS),
): SeleniumContext {
    private val log = LoggerFactory.getLogger(javaClass)

    override lateinit var driver: ChromeDriver
    override lateinit var actions: Actions

    fun start() {
        requireNotNull(System.getProperty("webdriver.chrome.driver")) {
            "Pass the path to the Chrome web driver as a system property: -Dwebdriver.chrome.driver=/path/to/driver"
        }
        val profile = profilePath?.let { Path(it).absolutePathString() }
        log.info("Storing Chrome user data in $profile")
        driver = ChromeDriver(ChromeOptions().apply { profile?.let { addArguments("user-data-dir=$it") } })
        driver.manage().timeouts().implicitlyWait(implicitWait)
        actions = Actions(driver)
    }

    fun stop() {
        log.info("Closing browser window")
        driver.windowHandles.forEach {
            driver.switchTo().window(it)
            driver.close()
        }
    }
}
