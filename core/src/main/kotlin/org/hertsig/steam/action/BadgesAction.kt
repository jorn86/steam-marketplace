package org.hertsig.steam.action

import org.hertsig.steam.service.SeleniumContext
import org.slf4j.LoggerFactory

class BadgesAction(
    context: SeleniumContext,
    private val accountId: String,
) : SeleniumContext by context, () -> Unit {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun invoke() {
        log.info("Getting badges")
        driver.get("https://steamcommunity.com/profiles/$accountId/badges")

        TODO()
    }
}
