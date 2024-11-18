package mioneF.yumCup.domain;

public record KakaoRestaurantResponse(
        String id,           // 카카오 장소 ID
        String name,         // 장소명
        String category,     // 카테고리
        String phone,        // 전화번호
        String address,      // 주소
        String roadAddress,  // 도로명 주소
        Double latitude,     // 위도
        Double longitude,    // 경도
        String placeUrl,     // 장소 상세 페이지 URL
        Double distance      // 중심점으로부터의 거리 (미터)
) {
}
