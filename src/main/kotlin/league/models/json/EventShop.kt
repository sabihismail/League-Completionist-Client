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

data class LolEventShopInfo(val currentTokenBalance: Int)

data class LolEventShopPurchaseOfferRequest(val offerId: String)

data class LolEventShopCategoriesOfferItem(val inventoryType: LolEventShopCategoriesOfferItemInventoryType, val id: String, val price: Int,
                                           val localizedDescription: String, val localizedTitle: String)

data class LolEventShopCategoriesOffer(val offers: List<LolEventShopCategoriesOfferItem>, val price: Int)

data class LolEventShopUnclaimedRewards(val rewardsCount: Int, val lockedTokensCount: Int)
