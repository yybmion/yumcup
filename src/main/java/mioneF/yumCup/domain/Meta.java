package mioneF.yumCup.domain;

public record Meta(
        Integer total_count,        // 검색어에 검색된 문서 수
        Integer pageable_count,     // total_count 중 노출 가능 문서 수
        Boolean is_end             // 현재 페이지가 마지막 페이지인지 여부
) {
}
