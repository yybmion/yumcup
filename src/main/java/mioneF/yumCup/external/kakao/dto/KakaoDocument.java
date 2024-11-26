package mioneF.yumCup.external.kakao.dto;

public record KakaoDocument(
        String id,                // 장소 ID
        String place_name,        // 장소명
        String category_name,     // 카테고리 이름
        String category_group_code,// 중요 카테고리만 그룹핑한 카테고리 그룹 코드
        String category_group_name,// 중요 카테고리만 그룹핑한 카테고리 그룹명
        String phone,            // 전화번호
        String address_name,     // 전체 지번 주소
        String road_address_name,// 전체 도로명 주소
        String x,               // X 좌표값 (longitude)
        String y,               // Y 좌표값 (latitude)
        String place_url,       // 장소 상세페이지 URL
        String distance         // 중심좌표까지의 거리
) {
}
