package mioneF.yumCup.domain.dto.request;

public record LocationRequest(
        Double latitude,
        Double longitude,
        Integer radius    // 미터 단위
) {
}
