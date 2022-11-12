package league.models.json

data class LolEventShopCategoriesOffer(val offers: List<LolEventShopCategoriesOfferItem>, val localizedDescription: String, val localizedTitle: String,
                                       val price: Int)
