package mioneF.yumCup.domain;

public record RestaurantResponse(
        Long id,
        String name,
        String category,
        Integer distance,
        String imageUrl
) {
    // 정적 팩토리 메서드도 record 안에 정의할 수 있습니다
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getDistance(),
                restaurant.getImageUrl()
        );
    }
}