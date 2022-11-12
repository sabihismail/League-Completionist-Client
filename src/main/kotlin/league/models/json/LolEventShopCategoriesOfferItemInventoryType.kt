package league.models.json

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class LolEventShopCategoriesOfferItemInventoryType {
    @SerializedName("CHAMPION_SKIN")
    CHAMPION_SKIN,

    @SerializedName("SUMMONER_ICON")
    SUMMONER_ICON,

    @SerializedName("HEXTECH_CRAFTING")
    HEXTECH_CRAFTING,
}