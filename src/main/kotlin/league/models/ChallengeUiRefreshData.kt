package league.models

import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import league.models.enums.ChallengeCategory
import league.models.json.ChallengeInfo
import league.models.json.ChallengeSummary

data class ChallengeUiRefreshData(
    val challengesSummary: ChallengeSummary,
    val allChallenges: ObservableMap<ChallengeCategory, List<ChallengeInfo>>,
    val filteredChallenges: ObservableMap<ChallengeCategory, List<ChallengeInfo>>,
    val allCategories: ObservableList<ChallengeCategory>,
    val categories: ObservableList<ChallengeCategory>
) {
    override fun toString(): String {
        return "ChallengeUiRefreshData(challengesSummary=$challengesSummary, allChallenges=$allChallenges, filteredChallenges=$filteredChallenges, " +
                "allCategories=$allCategories, categories=$categories)"
    }
}