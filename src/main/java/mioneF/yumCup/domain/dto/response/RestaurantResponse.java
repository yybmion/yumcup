package mioneF.yumCup.domain.dto.response;

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
        String photoUrl,          // mainPhotoUrl -> photoUrl로 변경
        String openingHours,
        Double rating,
        Integer ratingCount,      // 평점 개수 추가
        String priceRange
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
                restaurant.getOpeningHours(),
                restaurant.getRating(),
                restaurant.getRatingCount(),     // 평점 개수
                restaurant.getPriceRange()
        );
    }
}