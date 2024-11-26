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
            List<Photo> photos
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Photo(
                String photo_reference,
                Integer height,
                Integer width
        ) {}
    }
}
