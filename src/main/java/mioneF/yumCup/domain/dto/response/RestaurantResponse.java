package mioneF.yumCup.domain.dto.response;

import mioneF.yumCup.domain.entity.PriceLevel;
import mioneF.yumCup.domain.entity.Restaurant;

public record RestaurantResponse(
        Long id,
        String name,
        String category,
        Integer distance,
        String address,
        String roadAddress,
        String phone,
        String placeUrl,
        String priceLevelText,     // price_level을 텍스트로 변환한 값
        String isOpenNow,         // 현재 영업 여부
        Boolean weekdayText,        // 요일별 영업시간
        String photoUrl,          // mainPhotoUrl -> photoUrl로 변경
        Double rating,
        Integer ratingCount      // 평점 개수 추가
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getDistance(),
                restaurant.getAddress(),
                restaurant.getRoadAddress(),
                restaurant.getPhone(),
                restaurant.getPlaceUrl(),
                restaurant.getPhotoUrl(),        // mainPhotoUrl -> photoUrl
                PriceLevel.getDescription(restaurant.getPriceLevel()),
                restaurant.getIsOpenNow(),
                restaurant.getWeekdayText(),
                restaurant.getRating(),
                restaurant.getRatingCount()     // 평점 개수

        );
    }
}