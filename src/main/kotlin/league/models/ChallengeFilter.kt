package league.models

import league.models.json.ChallengeInfo

data class ChallengeFilter(val isSet: Boolean, val action: (ChallengeInfo) -> Boolean)
