package mioneF.yumCup.domain;

public record LocationRequest(
        Double latitude,
        Double longitude,
        Integer radius    // 미터 단위
) {
}
