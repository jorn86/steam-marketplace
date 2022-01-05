package org.hertsig.steam

data class TradingCard(
    val name: String,
    val game: String,
    val gems: Int,
    val itemLink: String,
    val marketLink: String,
) {
    val foil = name.endsWith(" (Foil)")
    val display: String get() = "$game - $name"
}
