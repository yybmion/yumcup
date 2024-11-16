package mioneF.yumCup.domain;

public record RestaurantResponse(
        String name,
        String category,
        Integer distance,
        String imageUrl,
        Integer winCount,
        Integer playCount
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getDistance(),
                restaurant.getImageUrl(),
                restaurant.getWinCount(),
                restaurant.getPlayCount()
        );
    }
}
