package league.models.json

data class LolEventShopCategoriesOfferItem(val inventoryType: LolEventShopCategoriesOfferItemInventoryType, val id: String, val price: Int,
                                           val localizedDescription: String, val localizedTitle: String)
