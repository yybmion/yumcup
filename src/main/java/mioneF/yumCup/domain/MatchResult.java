package mioneF.yumCup.domain;

public record MatchResult(
        boolean gameComplete,
        Match nextMatch,
        Restaurant winner  // 결승전 종료시에만 설정
) {
}
