package mioneF.yumCup.external.kakao.service;

import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.infrastructure.api.GooglePlacesApiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GooglePlaceService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GooglePlaceService 단위 테스트")
class GooglePlaceServiceTest {

	@Mock
	private GooglePlacesApiClient googleApiClient;

	@InjectMocks
	private GooglePlaceService googlePlaceService;

	private GooglePlaceResponse.GooglePlace mockGooglePlace;
	private GooglePlaceResponse mockGoogleResponse;

	@BeforeEach
	void setUp() {
		GooglePlaceResponse.GooglePlace.Photo mockPhoto =
				new GooglePlaceResponse.GooglePlace.Photo( "test-photo-ref", 400, 400 );

		GooglePlaceResponse.GooglePlace.OpeningHours mockOpeningHours =
				new GooglePlaceResponse.GooglePlace.OpeningHours( true );

		mockGooglePlace = new GooglePlaceResponse.GooglePlace(
				"ChIJ123",
				"Test Restaurant",
				4.5,
				100,
				List.of( mockPhoto ),
				2,
				mockOpeningHours
		);

		mockGoogleResponse = new GooglePlaceResponse(
				List.of( mockGooglePlace ),
				"OK"
		);
	}

	@Test
	@DisplayName("findPlace() - 성공: API 클라이언트를 올바르게 호출")
	void findPlace_Success() {
		// Given
		when( googleApiClient.findPlace( anyString(), anyDouble(), anyDouble(), eq( GooglePlaceResponse.class ) ) )
				.thenReturn( mockGoogleResponse );

		// When
		GooglePlaceResponse response = googlePlaceService.findPlace(
				"kakao-123",
				"Test Restaurant",
				37.5665,
				126.9780
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.status() ).isEqualTo( "OK" );
		assertThat( response.candidates() ).hasSize( 1 );
		assertThat( response.candidates().get( 0 ).name() ).isEqualTo( "Test Restaurant" );

		// API 클라이언트 호출 검증
		verify( googleApiClient, times( 1 ) ).findPlace(
				eq( "Test Restaurant" ),
				eq( 37.5665 ),
				eq( 126.9780 ),
				eq( GooglePlaceResponse.class )
		);
	}

	@Test
	@DisplayName("findPlace() - 빈 결과: candidates가 비어있어도 정상 처리")
	void findPlace_EmptyCandidates() {
		// Given
		GooglePlaceResponse emptyResponse = new GooglePlaceResponse(
				Collections.emptyList(),
				"ZERO_RESULTS"
		);

		when( googleApiClient.findPlace( anyString(), anyDouble(), anyDouble(), any() ) )
				.thenReturn( emptyResponse );

		// When
		GooglePlaceResponse response = googlePlaceService.findPlace(
				"kakao-456",
				"Nonexistent Place",
				37.5665,
				126.9780
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.status() ).isEqualTo( "ZERO_RESULTS" );
		assertThat( response.candidates() ).isEmpty();
	}

	@Test
	@DisplayName("findPlace() - 파라미터 전달: 올바른 값으로 API 호출")
	void findPlace_ParameterPassing() {
		// Given
		when( googleApiClient.findPlace( anyString(), anyDouble(), anyDouble(), any() ) )
				.thenReturn( mockGoogleResponse );

		// When
		googlePlaceService.findPlace( "kakao-789", "스타벅스", 37.5, 127.0 );

		// Then
		verify( googleApiClient ).findPlace(
				eq( "스타벅스" ),
				eq( 37.5 ),
				eq( 127.0 ),
				eq( GooglePlaceResponse.class )
		);
	}

	@Test
	@DisplayName("getPhotoUrl() - 정상: API 클라이언트에게 위임")
	void getPhotoUrl_Success() {
		// Given
		String expectedUrl = "https://maps.googleapis.com/maps/api/place/photo?photo_reference=test-ref";
		when( googleApiClient.generatePhotoUrl( "test-ref" ) )
				.thenReturn( expectedUrl );

		// When
		String photoUrl = googlePlaceService.getPhotoUrl( "test-ref" );

		// Then
		assertThat( photoUrl ).isEqualTo( expectedUrl );
		verify( googleApiClient, times( 1 ) ).generatePhotoUrl( "test-ref" );
	}

	@Test
	@DisplayName("getPhotoUrl() - null 입력: API 클라이언트 호출 안 함")
	void getPhotoUrl_NullInput() {
		// When
		String photoUrl = googlePlaceService.getPhotoUrl( null );

		// Then
		assertThat( photoUrl ).isNull();

		verify( googleApiClient, never() ).generatePhotoUrl( anyString() );
	}

	@Test
	@DisplayName("getPhotoUrl() - null 반환: API 클라이언트가 null 반환 시")
	void getPhotoUrl_NullReturn() {
		// Given
		when( googleApiClient.generatePhotoUrl( "invalid-ref" ) ).thenReturn( null );

		// When
		String photoUrl = googlePlaceService.getPhotoUrl( "invalid-ref" );

		// Then
		assertThat( photoUrl ).isNull();
	}
}
