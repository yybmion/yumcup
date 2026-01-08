package mioneF.yumCup.external.kakao.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import mioneF.yumCup.domain.Meta;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoSearchResponse(
        Meta meta,
        List<KakaoDocument> documents
) {
}
