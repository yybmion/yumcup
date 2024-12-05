package mioneF.yumCup.external.kakao.dto;

import java.util.List;

public record KakaoPlaceResponse(
        String id,
        String place_name,
        String category_name,
        String category_group_code,
        String category_group_name,
        String phone,
        String address_name,
        String road_address_name,
        String place_url,
        String distance,
        String opening_hours,
        Double rating,        // 평점
        String price_range,   // 가격대
        List<String> photos,  // 사진 URL 목록
        List<Menu> menus     // 메뉴 정보
) {
    public record Menu(
            String name,
            String price
    ) {
    }
}
