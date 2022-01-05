package org.hertsig.steam.action

import org.hertsig.steam.TradingCard
import org.hertsig.steam.TradingCardPrice
import org.hertsig.steam.service.SeleniumContext
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

open class InventoryAction(
    context: SeleniumContext,
    private val accountId: String,
    private val pauseBeforeScrap: Long = 0,
    private val pauseBeforeSale: Long = 0,
): SeleniumContext by context {
    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var itemLinks: List<String>
    private var linkElements: List<WebElement> = listOf()

    fun init() {
        driver.get("https://steamcommunity.com/profiles/$accountId/inventory/#753")
        initPageState()
        val links = mutableListOf<String>()
        do {
            linkElements.filter { it.safeIsDisplayed }.forEach { links.add(it.getAttribute("href")) }
            pause()
        } while (nextPage())
        itemLinks = links.toList()
        log.info("Initialized with ${itemLinks.size} cards")
    }

    fun listCards() = itemLinks.mapNotNull { checkCard(it) }

    private fun checkCard(link: String): TradingCard? {
        val attribute = selectCardAndGetAttribute(link) ?: return null

        val nameElement = driver.findElement(By.id("${attribute}_item_name"))
        val typeElement = driver.findElement(By.id("${attribute}_item_type"))
        val scrapElement = driver.findElement(By.id("${attribute}_item_scrap_value"))
        val marketElement = driver.findElement(By.id("${attribute}_item_market_actions"))

        val name = nameElement.text
        val game = typeElement.text.substringBeforeLast(" Trading Card").trim()
        val marketLink = marketElement.findElement(By.tagName("a")).getAttribute("href")
        val gems = scrapElement.text.substringBefore(' ', "-1").toInt()

        return TradingCard(name, game, gems, link, marketLink)
    }

    fun getPriceInfo(card: TradingCard) = inSeparateTab(card.marketLink) {
        val buyElement = driver.findElement(By.id("market_commodity_forsale"))
        val sellElement = driver.findElement(By.id("market_commodity_buyrequests"))
        val lowestOffer = buyElement.parsePrice()
        val highestRequest = sellElement.parsePrice()
        TradingCardPrice(highestRequest, lowestOffer)
    }

    private fun WebElement.parsePrice(): Int {
        val priceElements = findElements(By.className("market_commodity_orders_header_promote"))
        if (priceElements.isEmpty()) return -1
        val text = priceElements.last().text
        val match = Regex("^(\\d+)[.,](\\d{2})").find(text) ?: return 0
        return match.groups[1]!!.value.toInt() * 100 + match.groups[2]!!.value.toInt()
    }

    fun sellCard(card: TradingCard, priceInCents: Int) {
        if (selectCard(card.itemLink)) {
            sellSelectedCard(priceInCents)
        }
    }

    private fun sellSelectedCard(priceInCents: Int) {
        require(priceInCents > 4) { "No point in listing for under 5 cents" }
        val button = driver.findElements(By.cssSelector("a.item_market_action_button"))
            .single { it.isDisplayed }
        click(button)
        type(By.id("market_sell_buyercurrency_input"), "%.2f", priceInCents * 0.01f)
        pause()
        click(By.id("market_sell_dialog_accept_ssa"))
        pause(pauseBeforeSale)
        click(By.id("market_sell_dialog_accept"))
        pause()
        click(By.id("market_sell_dialog_ok"))
        while (driver.findElements(By.id("market_sell_dialog_throbber")).any { it.isDisplayed }) {
            log.debug("Waiting for sale to complete")
            pause()
        }
        pause()
        initPageState()
    }

    fun scrapCard(card: TradingCard) {
        val attribute = selectCardAndGetAttribute(card.itemLink)
        if (attribute != null) {
            click(By.id("${attribute}_item_scrap_link"))
            pause(pauseBeforeScrap)
            click(By.cssSelector("div.btn_green_steamui")) // ok
            pause()
            initPageState()
        }
    }

    private fun initPageState() {
        setFilters()
        pause()
        linkElements = driver.findElement(By.id("inventory_${accountId}_753_0"))
            .findElements(By.cssSelector("a.inventory_item_link"))
            .filter { it.getAttribute("href").contains("inventory/#753_6_") }
        pause()
    }

    private fun setFilters() {
//        click(By.id("contextselect"))
//        pause()
//        click(By.id("context_option_753_6")) // registers as Community but actually selects Gifts (sometimes)
        click(By.id("filter_tag_show"))
        click(By.id("tag_filter_753_0_misc_marketable"))
        click(driver.findElements(By.className("econ_tag_filter_collapsable_tags_showlink")).last())
        pause()
        click(By.id("tag_filter_753_0_item_class_item_class_2"))
    }

    private fun nextPage(): Boolean {
        val element = driver.findElement(By.id("pagebtn_next"))
        if (element.getAttribute("class").contains("disabled")) return false
        click(element)
        return true
    }

    private fun previousPage(): Boolean {
        val element = driver.findElement(By.id("pagebtn_previous"))
        if (element.getAttribute("class").contains("disabled")) return false
        click(element)
        return true
    }

    private fun selectCard(link: String): Boolean {
        val targetIndex = linkElements.indexOfFirst { it.getAttribute("href") == link }
        if (targetIndex == -1) return false
        val itemElement = linkElements[targetIndex]
        var progress = true
        while (progress && !itemElement.isDisplayed) {
            val currentDisplayedIndex = linkElements.indexOfFirst { it.safeIsDisplayed }
            progress = if (currentDisplayedIndex > targetIndex) {
                log.debug("Navigating back from $currentDisplayedIndex to $targetIndex")
                previousPage()
            } else {
                log.debug("Navigating forward from $currentDisplayedIndex to $targetIndex")
                nextPage()
            }
            pause(250, ChronoUnit.MILLIS)
        }

        if (progress) {
            log.debug("Clicking ${itemElement.getAttribute("href")}")
            click(itemElement)
        }
        return progress
    }

    private fun selectCardAndGetAttribute(link: String): String? {
        if (!selectCard(link)) {
            log.warn("Missing element for $link")
            return null
        }
        pause(250, ChronoUnit.MILLIS)

        val parent0 = driver.findElement(By.id("iteminfo0"))
        val parent1 = driver.findElement(By.id("iteminfo1"))
        if (parent0.isDisplayed == parent1.isDisplayed) {
            log.warn("Missed one")
            return null
        }

        return (parent0.takeIf(WebElement::isDisplayed) ?: parent1).getAttribute("id")
    }
}
