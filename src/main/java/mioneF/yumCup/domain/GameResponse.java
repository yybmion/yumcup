package mioneF.yumCup.domain;

public record GameResponse(
        Long gameId,
        Integer currentRound,
        Match currentMatch,
        Integer totalMatches,
        GameStatus status
) {
}
