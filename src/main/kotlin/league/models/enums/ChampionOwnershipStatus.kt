package league.models.enums

enum class ChampionOwnershipStatus {
    NOT_OWNED,
    FREE_TO_PLAY,
    RENTAL,
    OWNED;

    companion object {
        val UNOWNED_SET = setOf(NOT_OWNED, FREE_TO_PLAY, RENTAL)
    }
}