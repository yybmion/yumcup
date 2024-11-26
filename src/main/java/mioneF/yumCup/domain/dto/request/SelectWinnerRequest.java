package mioneF.yumCup.domain.dto.request;

public record SelectWinnerRequest(
        Long gameId,
        Long matchId,
        Long winnerId
) {
}
