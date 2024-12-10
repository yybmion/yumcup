import React, {useEffect, useState} from 'react';
import RestaurantCard from './RestaurantCard';
import GoogleAd from '../components/GoogleAd';  // GoogleAd import 추가

const WorldCupGame = () => {
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

    const startLocationBasedGame = async (position) => {
        try {
            setIsLoading(true);
            const requestData = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude,
                radius: 1000
            };
            console.log('Sending location data:', requestData);

            const response = await fetch(`${process.env.REACT_APP_API_URL}/api/yumcup/start/location`, {
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
            setError(null);

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
                    Loading...
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen flex flex-col">
                <div className="flex-1 flex flex-col justify-center items-center min-h-[calc(100vh-200px)]">
                    <p className="text-red-500 mb-4">{error}</p>
                    <button onClick={startGame}
                            className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors">
                        다시 시도
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
                <div>
                    <div className="w-full max-w-[1024px] mx-auto">
                        <GoogleAd/>
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
            <div>
                <div className="w-full max-w-[1024px] mx-auto">
                    <GoogleAd/>
                </div>
            </div>
        </div>
    );
};

export default WorldCupGame;