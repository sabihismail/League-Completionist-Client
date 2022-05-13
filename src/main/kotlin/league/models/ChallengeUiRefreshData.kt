package league.models

import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import league.models.enums.ChallengeCategory
import league.models.json.ChallengeInfo

data class ChallengeUiRefreshData(val allChallengeInfo: ObservableMap<ChallengeCategory, List<ChallengeInfo>>,
                                  val sortedChallengeInfo: ObservableMap<ChallengeCategory, List<ChallengeInfo>>,
                                  val categories: ObservableList<ChallengeCategory>)