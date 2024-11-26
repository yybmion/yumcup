import React from 'react';

const RestaurantCard = ({restaurant, onClick}) => {
    const DEFAULT_IMAGE = '/images/default-restaurant.png';

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
                <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded-full text-xs sm:text-sm">
                    {restaurant.priceRange || "만원-2만원"}
                </span>
                {restaurant.rating && (
                    <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full text-xs sm:text-sm">
                        ⭐ {restaurant.rating.toFixed(1)}
                        {restaurant.ratingCount && ` (${restaurant.ratingCount})`}
                    </span>
                )}
            </div>

            {/* 상세 정보 */}
            <div className="space-y-1 sm:space-y-2 text-xs sm:text-sm text-gray-500">
                <p className="flex items-center gap-2">
                    <span>🕒</span>
                    {restaurant.openingHours || "11:00 - 21:00"}
                </p>
                <p className="flex items-center gap-2">
                    <span>📍</span>
                    <span className="truncate">{restaurant.roadAddress}</span>
                </p>
                <p className="flex items-center gap-2">
                    <span>📞</span>
                    {restaurant.phone}
                </p>
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