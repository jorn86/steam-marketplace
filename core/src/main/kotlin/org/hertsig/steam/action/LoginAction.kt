package org.hertsig.steam.action

import org.hertsig.steam.service.SeleniumContext
import org.openqa.selenium.By
import org.slf4j.LoggerFactory

class LoginAction(private val context: SeleniumContext) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun invoke(): String {
        context.driver.get("https://steamcommunity.com/")
        return context.waitForLogin()
    }

    private tailrec fun SeleniumContext.waitForLogin(): String {
        val accountSpan = driver.findElements(By.id("account_pulldown"))
        if (accountSpan.isEmpty()) {
            log.info("Please log in")
            pause(10)
            return waitForLogin()
        }
        val profileLink = driver.findElement(By.className("user_avatar")).getAttribute("href")
        val accountId = profileLink.substringAfter("/profiles/", "").substringBefore('/')
        log.info("Found account name ${accountSpan[0].text} and id $accountId")
        return accountId
    }
}
