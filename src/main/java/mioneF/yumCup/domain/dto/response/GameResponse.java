package mioneF.yumCup.domain.dto.response;

import mioneF.yumCup.domain.entity.GameStatus;

public record GameResponse(
        Long gameId,
        Integer currentRound,
        MatchResponse currentMatch,
        GameStatus status
) {
}
