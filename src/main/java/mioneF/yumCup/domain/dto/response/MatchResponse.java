package mioneF.yumCup.domain.dto.response;

public record MatchResponse(
        Long id,
        RestaurantResponse restaurant1,
        RestaurantResponse restaurant2,
        Integer round,
        Integer matchOrder
) {
}
