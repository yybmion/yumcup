import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import RestaurantCard from './RestaurantCard';

/**
 * 🚀 Priority-Based Loading 최적화
 * - Home에서 받은 게임 데이터 재사용 (중복 API 호출 제거)
 * - 백그라운드 enrichment는 진행 중
 */
const WorldCupGame = () => {
    const location = useLocation();
    const navigate = useNavigate();

    const [gameId, setGameId] = useState(null);
    const [currentMatch, setCurrentMatch] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [round, setRound] = useState(16);
    const [currentRound, setCurrentRound] = useState(1);
    const [winner, setWinner] = useState(null);
    const [error, setError] = useState(null);

    const getProgressPercentage = () => {
        const totalMatchesInRound = currentMatch.round / 2;
        return (currentRound / totalMatchesInRound) * 100;
    };

    useEffect(() => {
        // Home에서 전달받은 게임 데이터 확인
        const gameData = location.state?.gameData;

        if (gameData) {
            console.log('✅ Using game data from Home (no API call)');
            setGameId(gameData.gameId);
            setCurrentMatch(gameData.currentMatch);
            setRound(gameData.currentRound);
            setCurrentRound(1);
            setIsLoading(false);
        } else {
            console.log('⚠️ No game data, redirecting to Home');
            // 게임 데이터 없으면 홈으로
            navigate('/');
        }
    }, [location, navigate]);

    const handleSelect = async (selectedRestaurant) => {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/api/yumcup/select`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                                         gameId: gameId,
                                         matchId: currentMatch.id,
                                         winnerId: selectedRestaurant.id
                                     })
            });
            const result = await response.json();

            if (result.gameComplete) {
                setWinner(result.winner);
            } else {
                setCurrentMatch(result.nextMatch);
                if (currentMatch.round === result.nextMatch.round) {
                    setCurrentRound(prev => prev + 1);
                } else {
                    setCurrentRound(1);
                }
            }
        } catch (error) {
            console.error('Failed to process selection:', error);
        }
    };

    if (isLoading || !currentMatch) {
        return (
            <div className="min-h-screen flex flex-col">
                <div className="flex-1 flex justify-center items-center min-h-[calc(100vh-200px)]">
                    <div className="text-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto mb-4"></div>
                        <p className="text-gray-600">게임을 준비하는 중...</p>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen flex flex-col">
                <div className="flex-1 flex flex-col justify-center items-center min-h-[calc(100vh-200px)]">
                    <p className="text-red-500 mb-4">{error}</p>
                    <button
                        onClick={() => navigate('/')}
                        className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors">
                        홈으로 돌아가기
                    </button>
                </div>
            </div>
        );
    }

    if (winner) {
        return (
            <div className="min-h-screen flex flex-col">
                <div className="flex-1 max-w-4xl mx-auto p-8 min-h-[calc(100vh-200px)]">
                    <div className="text-center mb-8">
                        <h1 className="text-2xl font-bold mb-2">🎉 우승 음식점 🎉</h1>
                    </div>
                    <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                        <div className="relative h-48 bg-gray-200 mb-4 rounded-lg overflow-hidden">
                            <img
                                src={winner.photoUrl || '/images/default-restaurant.png'}
                                alt={winner.name}
                                className="w-full h-full object-cover"
                                onError={(e) => {
                                    e.target.src = '/images/default-restaurant.png';
                                }}
                            />
                        </div>
                        <h2 className="text-2xl font-bold mb-2">{winner.name}</h2>
                        <p className="text-gray-600 mb-2">{winner.category}</p>
                        <div className="flex justify-center gap-2 mb-4">
                            <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded-full text-sm">
                                {winner.priceLevel || "가격정보 없음"}
                            </span>
                            {winner.rating && (
                                <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full text-sm">
                                    ⭐ {winner.rating.toFixed(1)}
                                    {winner.ratingCount && ` (${winner.ratingCount})`}
                                </span>
                            )}
                        </div>
                        <p className="text-sm text-gray-500 mb-4">{winner.distance}m</p>
                        <div className="space-y-2 text-sm text-gray-500 mb-4">
                            <p>📍 {winner.roadAddress}</p>
                            <p>📞 {winner.phone || "전화번호 없음"}</p>
                        </div>
                        <div className="flex gap-2 justify-center">
                            <button
                                onClick={() => navigate('/')}
                                className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors"
                            >
                                새 게임 시작
                            </button>
                            <button
                                onClick={() => window.open(winner.placeUrl, '_blank')}
                                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                            >
                                매장안내
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col">
            <div className="flex-1 max-w-4xl mx-auto p-4 sm:p-8 min-h-[calc(100vh-200px)]">
                <div className="text-center mb-4 sm:mb-8">
                    <h1 className="text-xl sm:text-2xl font-bold mb-2">음식점 월드컵</h1>
                    <p className="text-base sm:text-lg text-gray-600">
                        {currentMatch.round}강 - {currentRound}/{currentMatch.round / 2}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                        💡 사진과 평점은 게임 진행 중 자동으로 업데이트됩니다
                    </p>
                </div>

                <div className="flex flex-col sm:flex-row justify-between items-center gap-4 sm:gap-8">
                    <RestaurantCard
                        restaurant={currentMatch.restaurant1}
                        onClick={() => handleSelect(currentMatch.restaurant1)}
                    />
                    <div className="text-2xl sm:text-4xl font-bold text-red-500 py-2 sm:px-4">
                        VS
                    </div>
                    <RestaurantCard
                        restaurant={currentMatch.restaurant2}
                        onClick={() => handleSelect(currentMatch.restaurant2)}
                    />
                </div>

                <div className="mt-4 sm:mt-8 bg-gray-200 rounded-full h-2">
                    <div
                        className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                        style={{width: `${getProgressPercentage()}%`}}
                    />
                </div>
            </div>
        </div>
    );
};

export default WorldCupGame;
