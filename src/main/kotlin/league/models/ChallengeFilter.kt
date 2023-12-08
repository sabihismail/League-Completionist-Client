package league.models

import league.models.json.Challenge

data class ChallengeFilter(val isSet: Boolean, val action: (Challenge) -> Boolean)
