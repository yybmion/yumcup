package mioneF.yumCup.external.kakao.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.exception.RestaurantProcessingException;
import mioneF.yumCup.exception.RestaurantProcessingTimeoutException;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import mioneF.yumCup.external.kakao.dto.KakaoSearchResponse;
import mioneF.yumCup.performance.Monitored;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoMapRestaurantService {
	private static final String CATEGORY_GROUP_CODE = "FD6";
	private static final int PAGE_SIZE = 15;
	private static final int REQUIRED_RESTAURANTS = 16;
	private static final int TIMEOUT_SECONDS = 5;

	private final ExecutorService executorService;
	private final WebClient kakaoWebClient;
	private final GooglePlaceService googlePlaceService;
	private final RestaurantRepository restaurantRepository;

	public KakaoMapRestaurantService(
			@Qualifier("kakaoWebClient") WebClient kakaoWebClient,
			GooglePlaceService googlePlaceService,
			RestaurantRepository restaurantRepository) {
		this.kakaoWebClient = kakaoWebClient;
		this.googlePlaceService = googlePlaceService;
		this.restaurantRepository = restaurantRepository;
		this.executorService = Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() * 2
		);
	}

	@Monitored
	@Transactional
	public List<Restaurant> searchNearbyRestaurants(Double latitude, Double longitude, Integer radius) {
		List<Restaurant> allRestaurants = new ArrayList<>();
		int page = 1;

		while ( allRestaurants.size() < REQUIRED_RESTAURANTS ) {
			KakaoSearchResponse response = fetchRestaurantsPage( latitude, longitude, radius, page );

			if ( response == null || response.documents().isEmpty() ) {
				throw new NoNearbyRestaurantsException(
						String.format( "Can't found any around restaurant" )
				);
			}

			log.info( "Processing {} restaurants in parallel", response.documents().size() );

			List<CompletableFuture<Restaurant>> futures = response.documents().stream()
					.map( doc -> CompletableFuture.supplyAsync(
							() -> {
								try {
									return createRestaurantWithGoogleInfo( doc );
								}
								catch (Exception e) {
									log.error( "Error processing restaurant {}: {}", doc.place_name(), e.getMessage() );
									throw new RestaurantProcessingException( "Error processing restaurant" );
								}
							}, executorService
					) )
					.collect( Collectors.toList() );

			List<Restaurant> pageRestaurants = collectRestaurantResults( futures );
			log.info( "Completed processing {} restaurants", pageRestaurants.size() );

			List<String> kakaoIds = pageRestaurants.stream()
					.map( Restaurant::getKakaoId )
					.collect( Collectors.toList() );

			List<Restaurant> existingRestaurants = restaurantRepository.findByKakaoIdIn( kakaoIds );

			Map<String, Restaurant> existingMap = existingRestaurants.stream()
					.collect( Collectors.toMap( Restaurant::getKakaoId, Function.identity() ) );

			List<Restaurant> newRestaurants = new ArrayList<>();
			List<Restaurant> restaurantsToUpdate = new ArrayList<>();

			for ( Restaurant restaurant : pageRestaurants ) {
				Restaurant existing = existingMap.get( restaurant.getKakaoId() );

				if ( existing == null ) {
					newRestaurants.add( restaurant );
				}
				else {
					if ( existing.getUpdatedAt().isBefore( LocalDateTime.now().minusDays( 14 ) ) ) {
						existing.updateWithNewInfo( restaurant );
						restaurantsToUpdate.add( existing );
					}
					allRestaurants.add( existing );
				}
			}

			if ( !newRestaurants.isEmpty() ) {
				log.info( "Batch inserting {} new restaurants", newRestaurants.size() );
				List<Restaurant> savedRestaurants = restaurantRepository.saveAll( newRestaurants );
				allRestaurants.addAll( savedRestaurants );
			}

			if ( !restaurantsToUpdate.isEmpty() ) {
				log.info( "Batch updating {} restaurants", restaurantsToUpdate.size() );
				restaurantRepository.saveAll( restaurantsToUpdate );
			}

			if ( response.meta().is_end() ) {
				break;
			}
			page++;
		}

		if ( allRestaurants.size() < REQUIRED_RESTAURANTS ) {
			throw new InsufficientRestaurantsException(
					String.format( "Need at least 16 restaurants, but found only %d", allRestaurants.size() )
			);
		}

		Collections.shuffle( allRestaurants );
		return allRestaurants;
	}

	private KakaoSearchResponse fetchRestaurantsPage(Double latitude, Double longitude, Integer radius, int page) {
		return kakaoWebClient.get()
				.uri( uriBuilder -> uriBuilder
						.path( "/v2/local/search/category.json" )
						.queryParam( "category_group_code", CATEGORY_GROUP_CODE )
						.queryParam( "x", longitude )
						.queryParam( "y", latitude )
						.queryParam( "radius", radius )
						.queryParam( "size", PAGE_SIZE )
						.queryParam( "page", page )
						.build() )
				.retrieve()
				.bodyToMono( KakaoSearchResponse.class )
				.block();
	}

	private Restaurant createRestaurantWithGoogleInfo(KakaoDocument doc) {
		Restaurant restaurant = createBaseRestaurant( doc );

		try {
			GooglePlaceResponse googleResponse = googlePlaceService.findPlace(
					doc.id(),
					doc.place_name(),
					Double.parseDouble( doc.y() ),
					Double.parseDouble( doc.x() )
			);

			if ( isValidGoogleResponse( googleResponse ) ) {
				GooglePlaceResponse.GooglePlace place = googleResponse.candidates().get( 0 );
				restaurant = updateRestaurantWithGoogleInfo( restaurant, place );
			}
		}
		catch (Exception e) {
			log.warn( "Google API failed for {}: {}", doc.place_name(), e.getMessage() );
		}

		return restaurant;
	}

	private Restaurant createBaseRestaurant(KakaoDocument doc) {
		String category = extractMainCategory( doc.category_name() );

		return Restaurant.builder()
				.name( doc.place_name() )
				.category( category )
				.distance( Integer.parseInt( doc.distance() ) )
				.kakaoId( doc.id() )
				.latitude( Double.parseDouble( doc.y() ) )
				.longitude( Double.parseDouble( doc.x() ) )
				.address( doc.address_name() )
				.roadAddress( doc.road_address_name() )
				.phone( doc.phone() )
				.placeUrl( doc.place_url() )
				.build();
	}

	private boolean isValidGoogleResponse(GooglePlaceResponse googleResponse) {
		return googleResponse != null &&
				googleResponse.candidates() != null &&
				!googleResponse.candidates().isEmpty();
	}

	private Restaurant updateRestaurantWithGoogleInfo(
			Restaurant restaurant,
			GooglePlaceResponse.GooglePlace place) {
		String photoUrl = extractPhotoUrl( place );
		Boolean isOpenNow = extractOpeningStatus( place );

		return restaurant.toBuilder()
				.rating( place.rating() )
				.ratingCount( place.user_ratings_total() )
				.photoUrl( photoUrl )
				.priceLevel( place.price_level() )
				.isOpenNow( isOpenNow )
				.build();
	}

	private String extractPhotoUrl(GooglePlaceResponse.GooglePlace place) {
		if ( place.photos() != null && !place.photos().isEmpty() ) {
			return googlePlaceService.getPhotoUrl(
					place.photos().get( 0 ).photo_reference()
			);
		}
		return null;
	}

	private Boolean extractOpeningStatus(GooglePlaceResponse.GooglePlace place) {
		if ( place.opening_hours() != null ) {
			return place.opening_hours().open_now();
		}
		return null;
	}

	private String extractMainCategory(String categoryName) {
		String[] categories = categoryName.split( " > " );
		return categories.length > 1 ? categories[1] : categories[0];
	}

	private List<Restaurant> collectRestaurantResults(List<CompletableFuture<Restaurant>> futures) {
		return futures.stream()
				.map( this::getRestaurantWithTimeout )
				.collect( Collectors.toList() );
	}

	private Restaurant getRestaurantWithTimeout(CompletableFuture<Restaurant> future) {
		try {
			return future.get( TIMEOUT_SECONDS, TimeUnit.SECONDS );
		}
		catch (TimeoutException e) {
			throw new RestaurantProcessingTimeoutException( "Restaurant information processing timeout" );
		}
		catch (Exception e) {
			throw new RestaurantProcessingException( "Error processing restaurant information" );
		}
	}
}
