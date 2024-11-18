package mioneF.yumCup.domain;

public record MatchResult(
        boolean gameComplete,
        MatchResponse nextMatch,
        RestaurantResponse winner
) {}
