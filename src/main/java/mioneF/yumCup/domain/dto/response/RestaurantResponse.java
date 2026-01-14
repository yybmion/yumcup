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
		String photoUrl,
		Double rating,
		Integer ratingCount,
		String priceLevel,
		Boolean isOpenNow
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
				restaurant.getPhotoUrl(),
				restaurant.getRating(),
				restaurant.getRatingCount(),
				PriceLevel.getDescription( restaurant.getPriceLevel() ),
				restaurant.getIsOpenNow()
		);
	}
}
