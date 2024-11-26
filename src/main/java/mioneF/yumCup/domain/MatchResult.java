package mioneF.yumCup.domain;

import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;

public record MatchResult(
        boolean gameComplete,
        MatchResponse nextMatch,
        RestaurantResponse winner
) {
}
