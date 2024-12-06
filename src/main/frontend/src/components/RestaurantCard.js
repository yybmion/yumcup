import React from 'react';

const RestaurantCard = ({restaurant, onClick}) => {
    const DEFAULT_IMAGE = '/images/default-restaurant.png';

    const getPriceLevelClass = (priceLevel) => {
        switch(priceLevel) {
            case "무료": return "bg-gray-100 text-gray-800";
            case "저렴": return "bg-green-100 text-green-800";
            case "보통": return "bg-blue-100 text-blue-800";
            case "비싼": return "bg-yellow-100 text-yellow-800";
            case "매우 비싼": return "bg-red-100 text-red-800";
            default: return "bg-gray-100 text-gray-800";
        }
    };

    return (
        <div
            className="w-full sm:flex-1 bg-white rounded-lg shadow-lg p-4 sm:p-6
                     cursor-pointer transition-transform hover:scale-105"
        >
            <div className="relative h-32 sm:h-48 bg-gray-200 mb-3 sm:mb-4 rounded-lg overflow-hidden">
                <img
                    src={restaurant.photoUrl || DEFAULT_IMAGE}
                    alt={restaurant.name}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                        console.error('Image load error:', e.target.src);
                        e.target.src = DEFAULT_IMAGE;
                    }}
                />
                <div className="absolute bottom-2 right-2 bg-white/90 px-2 py-1 rounded-full text-xs sm:text-sm">
                    {restaurant.distance}m
                </div>
            </div>

            {/* 상단 정보 */}
            <h2 className="text-lg sm:text-xl font-bold mb-1 sm:mb-2">{restaurant.name}</h2>
            <p className="text-sm sm:text-base text-gray-600 mb-2">{restaurant.category}</p>

            {/* 태그들 */}
            <div className="flex flex-wrap gap-1 sm:gap-2 mb-2 sm:mb-3">
              <span className={`px-2 py-1 rounded-full text-xs sm:text-sm ${getPriceLevelClass(restaurant.priceLevel)}`}>
                  💰 {restaurant.priceLevel || "가격정보 없음"}
              </span>

                {restaurant.rating && (
                    <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full text-xs sm:text-sm">
                      ⭐ {restaurant.rating.toFixed(1)}
                        {restaurant.ratingCount && ` (${restaurant.ratingCount})`}
                  </span>
                )}

                {restaurant.isOpenNow !== null && (
                    <span className={`px-2 py-1 rounded-full text-xs sm:text-sm ${
                        restaurant.isOpenNow
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-red-100 text-red-800'
                    }`}>
                      {restaurant.isOpenNow ? '🟢 영업중' : '🔴 영업종료'}
                  </span>
                )}
            </div>

            {/* 상세 정보 */}
            <div className="space-y-1 sm:space-y-2 text-xs sm:text-sm text-gray-500">
                <p className="flex items-center gap-2">
                    <span>🕒</span>
                    {restaurant.weekdayText || "영업시간 정보 없음"}
                </p>
                <p className="flex items-center gap-2">
                    <span>📍</span>
                    <span className="truncate">{restaurant.roadAddress}</span>
                </p>
                <p className="flex items-center gap-2">
                    <span>📞</span>
                    {restaurant.phone || "전화번호 없음"}
                </p>
            </div>

            {/* 데이터 출처 범례 */}
            <div className="mt-3 pt-2 border-t border-gray-100">
                <div className="flex gap-4 text-xs text-gray-400">
                    <span>🗺️ 위치/연락처: Kakao Maps</span>
                    <span>⭐ 평점/영업: Google</span>
                </div>
            </div>

            {/* 버튼들 */}
            <div className="mt-3 sm:mt-4 flex gap-2">
                <button
                    onClick={onClick}
                    className="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600
                           transition-colors text-sm sm:text-base"
                >
                    선택하기
                </button>
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        window.open(restaurant.placeUrl, '_blank');
                    }}
                    className="px-3 sm:px-4 py-2 border border-gray-300 rounded-lg
                           hover:bg-gray-50 transition-colors text-sm sm:text-base"
                >
                    정보보기
                </button>
            </div>
        </div>
    );
};

export default RestaurantCard;