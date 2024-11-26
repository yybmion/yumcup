package mioneF.yumCup.domain;

public record GameResponse(
        Long gameId,
        Integer currentRound,
        MatchResponse currentMatch,
        GameStatus status
) {}
