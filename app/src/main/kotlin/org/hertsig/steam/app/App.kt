package org.hertsig.steam.app

import org.hertsig.steam.TradingCard
import org.hertsig.steam.TradingCardPrice
import org.hertsig.steam.action.InventoryAction
import org.hertsig.steam.action.LoginAction
import org.hertsig.steam.service.SeleniumService
import org.slf4j.LoggerFactory

object App {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run() {
        val selenium = SeleniumService("./chrome-profiles/steam-marketplace").apply { start() }
        Runtime.getRuntime().addShutdownHook(Thread { selenium.stop() })

        val accountId = LoginAction(selenium).invoke()
        val action = InventoryAction(selenium, accountId)
        action.init()
        action.listCards()
            .groupBy { it.marketLink }
            .forEach { (_, cards) -> action.checkDistinctCard(cards) }
    }

    private fun InventoryAction.checkDistinctCard(cards: List<TradingCard>) {
        val card = cards.first()
        if (card.foil) {
            val price = getPriceInfo(card)
            if (price.lowestOffer > 14) {
                sell(cards, price)
            } else {
                log.info("Keeping ${cards.size}x ${card.display}: Too cheap to sell (${price.lowestOffer - 1} cents)")
            }
        } else if (cards.size > 1) {
            val price = getPriceInfo(card)
            if (price.lowestOffer > 7 || price.highestRequest > 6) {
                sell(cards.subList(1, cards.size - 1), price)
            } else if (card.gems >= 50) {
                log.info("Scrapping ${cards.size - 1} ${card.display} for ${card.gems} gems")
                cards.subList(1, cards.size - 1).forEach { scrapCard(it) }
            } else {
                log.info("Keeping ${cards.size}x ${card.display}: Too cheap to sell (${price.lowestOffer - 1} cents)")
            }
        } else {
            log.info("Keeping 1x ${card.display}: Not a foil or duplicate")
        }
    }

    private fun InventoryAction.sell(cards: List<TradingCard>, price: TradingCardPrice) {
        val card = cards.first()
        log.info("Selling ${cards.size} ${card.display} for ${price.lowestOffer - 1} cents")
        cards.forEach {
            sellCard(it, price.lowestOffer - 1)
        }
    }
}

fun main() = App.run()
