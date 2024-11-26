package mioneF.yumCup.domain;

import lombok.Getter;

@Getter
public enum GameStatus {
    PROGRESS("진행중"),
    COMPLETED("완료");

    private final String description;

    GameStatus(String description) {
        this.description = description;
    }
}