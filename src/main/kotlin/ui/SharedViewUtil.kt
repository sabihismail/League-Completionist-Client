package ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import league.models.ChampionInfo
import league.models.enums.ChampionRole
import league.models.enums.GameMode
import league.models.json.Challenge

object SharedViewUtil {
    private const val EMPTY_CHALLENGE_NAME = "ALL CHALLENGES"

    fun addEmptyChallenge(lst: List<Challenge>): ObservableList<Challenge> {
        val challenge = Challenge().apply {
            name = EMPTY_CHALLENGE_NAME
            description = EMPTY_CHALLENGE_NAME
        }

        return FXCollections.observableList(listOf(challenge) + lst)
    }

    fun Challenge.isEmptyChallenge(): Boolean {
        return name == EMPTY_CHALLENGE_NAME
    }

    fun getActiveChallenges(
        allChallengesProperty: Iterable<Challenge>,
        gameMode: GameMode? = null,
        skip: SimpleBooleanProperty? = null,
    ): ObservableList<Challenge>? {
        val skipFilter: (Challenge) -> Boolean = if (skip != null) { c -> skip.value == false || !c.isComplete } else { _ -> true }
        val allFilters = listOf(skipFilter)

        return FXCollections.observableList(
            allChallengesProperty.sortedWith(compareByDescending<Challenge> { it.isEmptyChallenge() }.thenBy { it.description })
                .filter { it.isEmptyChallenge() || gameMode == null || it.gameModeSet.contains(gameMode) }
                .filter { it.isEmptyChallenge() || allFilters.all { filter -> filter(it) } }
        )
    }

    fun getActiveChampions(
        allChampionsProperty: Iterable<ChampionInfo>,
        role: SimpleObjectProperty<ChampionRole>? = null,
        search: SimpleStringProperty? = null,
        eternalsOnly: SimpleBooleanProperty? = null,
        challenges: SimpleObjectProperty<Challenge>? = null,
    ): ObservableList<ChampionInfo>? {
        val roleFilter: (ChampionInfo) -> Boolean = if (role != null) { c -> role.value == ChampionRole.ANY || c.roles?.contains(role.value) == true } else { _ -> true }
        val searchFilter: (ChampionInfo) -> Boolean = if (search != null) { c -> c.nameLower.contains(search.value.lowercase()) } else { _ -> true }
        val eternalsOnlyFilter: (ChampionInfo) -> Boolean = if (eternalsOnly != null) { c -> !eternalsOnly.value || c.eternalInfo.any { eternal -> eternal.value } } else { _ -> true }
        val challengesFilter: (ChampionInfo) -> Boolean = if (challenges != null) {
            { c -> (challenges.value == null || challenges.value.isEmptyChallenge()) ||
                (challenges.value.availableIdsInt.isEmpty() && !c.completedChallenges.contains(challenges.value.id?.toInt())) ||
                (challenges.value.availableIdsInt.isEmpty() && c.availableChallenges.contains(challenges.value.id?.toInt()) && !c.completedChallenges.contains(challenges.value.id?.toInt())) }
        } else { _ -> true }
        val allFilters = listOf(roleFilter, searchFilter, eternalsOnlyFilter, challengesFilter)

        return FXCollections.observableList(
            allChampionsProperty.filter { allFilters.all { filter -> filter(it) } }
        )
    }
}