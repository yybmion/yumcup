package mioneF.yumCup.external.kakao.dto;

import java.util.List;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import mioneF.yumCup.domain.Meta;

public record KakaoSearchResponse(
        Meta meta,
        List<KakaoDocument> documents
) {
}
