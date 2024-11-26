import React, {useEffect, useState} from 'react';
import RestaurantCard from './RestaurantCard';

const WorldCupGame = () => {
    const [gameId, setGameId] = useState(null);
    const [currentMatch, setCurrentMatch] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [round, setRound] = useState(16);
    const [currentRound, setCurrentRound] = useState(1);
    const [winner, setWinner] = useState(null);
    const [error, setError] = useState(null);  // 에러 상태 추가

    // 위치 기반 게임 시작
    const startLocationBasedGame = async (position) => {
        try {
            setIsLoading(true);
            const requestData = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude,
                radius: 500
            };
            console.log('Sending location data:', requestData);  // 요청 데이터 로깅

            const response = await fetch('/api/yumcup/start/location', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestData)
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Server response:', response.status, errorText);
                throw new Error(errorText || '주변 음식점을 찾을 수 없습니다.');
            }

            const data = await response.json();
            console.log('Server response data:', JSON.stringify(data, null, 2));

            // restaurant 객체 내부 확인
            if (data.currentMatch) {
                console.log('Restaurant 1:', data.currentMatch.restaurant1);
                console.log('Restaurant 2:', data.currentMatch.restaurant2);
            }

            setGameId(data.gameId);
            setCurrentMatch(data.currentMatch);
            setRound(data.currentRound);
            setCurrentRound(1);
            setWinner(null);
            setError(null);
        } catch (error) {
            console.error('Failed to start location-based game:', error);
            setError(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const startGame = () => {
        try {
            setIsLoading(true);
            setError(null);  // 에러 상태 초기화

            if (!navigator.geolocation) {
                throw new Error('이 브라우저는 위치 기반 서비스를 지원하지 않습니다.');
            }

            navigator.geolocation.getCurrentPosition(
                startLocationBasedGame,
                (error) => {
                    console.error('Geolocation error:', error);
                    setError('위치 정보를 가져올 수 없습니다. 위치 권한을 허용해주세요.');
                    setIsLoading(false);
                },
                {
                    enableHighAccuracy: true,
                    timeout: 5000,
                    maximumAge: 0
                }
            );
        } catch (error) {
            console.error('Failed to start game:', error);
            setError(error.message);
            setIsLoading(false);
        }
    };

    useEffect(() => {
        startGame();
    }, []);

    const handleSelect = async (selectedRestaurant) => {
        try {
            const response = await fetch('/api/yumcup/select', {
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
                // 같은 라운드의 다음 매치인 경우 currentRound 증가
                if (currentMatch.round === result.nextMatch.round) {
                    setCurrentRound(prev => prev + 1);
                } else {
                    // 다음 라운드의 첫 매치인 경우 currentRound 초기화
                    setCurrentRound(1);
                }
            }
        } catch (error) {
            console.error('Failed to process selection:', error);
        }
    };

    // 로딩 중이거나 데이터가 없을 때
    if (isLoading || !currentMatch) {
        return <div className="flex justify-center items-center h-screen">Loading...</div>;
    }

    if (error) {
        return (
            <div className="flex flex-col justify-center items-center h-screen">
                <p className="text-red-500 mb-4">{error}</p>
                <button
                    onClick={startGame}
                    className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors"
                >
                    다시 시도
                </button>
            </div>
        );
    }

    // 우승자가 결정됐을 때
    if (winner) {
        return (
            <div className="max-w-4xl mx-auto p-8">
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-bold mb-2">🎉 우승 음식점 🎉</h1>
                </div>
                <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                    <div className="relative h-48 bg-gray-200 mb-4 rounded-lg overflow-hidden">
                        <img
                            src={winner.photoUrl || '/static/images/default-restaurant.png'}
                            alt={winner.name}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                                e.target.src = '/static/images/default-restaurant.png';
                            }}
                        />
                    </div>
                    <h2 className="text-2xl font-bold mb-2">{winner.name}</h2>
                    <p className="text-gray-600 mb-2">{winner.category}</p>
                    <div className="flex justify-center gap-2 mb-4">
                    <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded-full text-sm">
                        {winner.priceRange || "만원-2만원"}
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
                        <p>📞 {winner.phone}</p>
                    </div>
                    <div className="flex gap-2 justify-center">
                        <button
                            onClick={startGame}
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
        );
    }

    return (
        <div className="max-w-4xl mx-auto p-4 sm:p-8">
            {/* 라운드 정보 */}
            <div className="text-center mb-4 sm:mb-8">
                <h1 className="text-xl sm:text-2xl font-bold mb-2">음식점 월드컵</h1>
                <p className="text-base sm:text-lg text-gray-600">
                    {currentMatch.round}강 - {currentRound}/{currentMatch.round / 2}
                </p>
            </div>

            {/* 대결 구역 - 모바일에서는 세로로 배치 */}
            <div className="flex flex-col sm:flex-row justify-between items-center gap-4 sm:gap-8">
                {/* 왼쪽 음식점 */}
                <RestaurantCard
                    restaurant={currentMatch.restaurant1}
                    onClick={() => handleSelect(currentMatch.restaurant1)}
                />

                {/* VS 표시 - 모바일에서는 작게 표시 */}
                <div className="text-2xl sm:text-4xl font-bold text-red-500 py-2 sm:px-4">
                    VS
                </div>

                {/* 오른쪽 음식점 */}
                <RestaurantCard
                    restaurant={currentMatch.restaurant2}
                    onClick={() => handleSelect(currentMatch.restaurant2)}
                />
            </div>

            {/* 진행 상황 바 */}
            <div className="mt-4 sm:mt-8 bg-gray-200 rounded-full h-2">
                <div
                    className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                    style={{width: `${(currentRound / (round / 2)) * 100}%`}}
                />
            </div>
        </div>
    );
};

export default WorldCupGame;