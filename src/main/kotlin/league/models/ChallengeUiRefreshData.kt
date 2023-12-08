package league.models

import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import league.models.enums.ChallengeCategory
import league.models.enums.GameMode
import league.models.json.Challenge
import league.models.json.ChallengeSummary

data class ChallengeUiRefreshData(
    val challengesSummary: ChallengeSummary,
    val allChallenges: ObservableMap<ChallengeCategory, List<Challenge>>,
    val filteredChallenges: ObservableMap<ChallengeCategory, List<Challenge>>,
    val allCategories: ObservableList<ChallengeCategory>,
    val categories: ObservableList<ChallengeCategory>,
    val allGameModes: ObservableList<GameMode>,
) {
    override fun toString(): String {
        return "ChallengeUiRefreshData(challengesSummary=$challengesSummary, allChallenges=$allChallenges, filteredChallenges=$filteredChallenges, " +
                "allCategories=$allCategories, categories=$categories, allGameModes=$allGameModes)"
    }
}