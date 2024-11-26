package mioneF.yumCup.domain;

public record SelectWinnerRequest(
        Long gameId,
        Long matchId,
        Long winnerId
) {
}
