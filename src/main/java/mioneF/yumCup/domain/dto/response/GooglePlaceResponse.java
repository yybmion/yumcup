package mioneF.yumCup.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public record GooglePlaceResponse(
        List<GooglePlace> candidates,
        String status
) {
    public record GooglePlace(
            String place_id,
            String name,
            Double rating,
            Integer user_ratings_total,
            List<Photo> photos,
            Integer price_level,
            OpeningHours opening_hours
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Photo(
                String photo_reference,
                Integer height,
                Integer width
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record OpeningHours(
                Boolean open_now
        ) {}
    }
}
