package mioneF.yumCup.domain.entity;

import java.util.Arrays;

public enum PriceLevel {
    FREE(0, "무료"),
    INEXPENSIVE(1, "저렴"),
    MODERATE(2, "보통"),
    EXPENSIVE(3, "비싼"),
    VERY_EXPENSIVE(4, "매우 비싼");

    private final int level;
    private final String description;

    PriceLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public static String getDescription(Integer level) {
        if (level == null) return "가격정보 없음";
        return Arrays.stream(values())
                .filter(p -> p.level == level)
                .findFirst()
                .map(p -> p.description)
                .orElse("가격정보 없음");
    }
}
