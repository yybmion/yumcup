import React, {useEffect, useState} from 'react';

const WorldCupGame = () => {
    const [gameId, setGameId] = useState(null);
    const [currentMatch, setCurrentMatch] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [round, setRound] = useState(16);
    const [currentRound, setCurrentRound] = useState(1);
    const [winner, setWinner] = useState(null);

    const startGame = async () => {
        try {
            setIsLoading(true);
            const response = await fetch('/api/yumcup/start');
            const data = await response.json();
            console.log('백엔드 응답:', data);

            setGameId(data.gameId);
            setCurrentMatch(data.currentMatch);
            setRound(data.currentRound);
            setCurrentRound(1);
            setWinner(null);
        } catch (error) {
            console.error('Failed to start game:', error);
        } finally {
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
                setRound(result.nextMatch.round);
                setCurrentRound(prev => prev + 1);
            }
        } catch (error) {
            console.error('Failed to process selection:', error);
        }
    };

    // 로딩 중이거나 데이터가 없을 때
    if (isLoading || !currentMatch) {
        return <div className="flex justify-center items-center h-screen">Loading...</div>;
    }

    // 우승자가 결정됐을 때
    if (winner) {
        return (
            <div className="max-w-4xl mx-auto p-8">
                <div className="text-center mb-8">
                    <h1 className="text-2xl font-bold mb-2">🎉 우승 음식점 🎉</h1>
                </div>
                <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                    <div className="h-48 bg-gray-200 mb-4 rounded-lg flex items-center justify-center">
                        <span className="text-gray-500">이미지</span>
                    </div>
                    <h2 className="text-2xl font-bold mb-2">{winner.name}</h2>
                    <p className="text-gray-600 mb-2">{winner.category}</p>
                    <p className="text-sm text-gray-500 mb-4">{winner.distance}m</p>
                    <button
                        onClick={startGame}
                        className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors"
                    >
                        새 게임 시작
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            {/* 라운드 정보 */}
            <div className="text-center mb-8">
                <h1 className="text-2xl font-bold mb-2">음식점 월드컵</h1>
                <p className="text-lg text-gray-600">{currentMatch.round}강 - {currentRound}/{currentMatch.round / 2}</p>
            </div>

            {/* 대결 구역 */}
            <div className="flex justify-between items-center gap-8">
                {/* 왼쪽 음식점 */}
                <div
                    onClick={() => handleSelect(currentMatch.restaurant1)}
                    className="flex-1 bg-white rounded-lg shadow-lg p-6 cursor-pointer transition-transform hover:scale-105"
                >
                    <div className="h-48 bg-gray-200 mb-4 rounded-lg flex items-center justify-center">
                        <span className="text-gray-500">이미지</span>
                    </div>
                    <h2 className="text-xl font-bold mb-2">{currentMatch.restaurant1.name}</h2>
                    <p className="text-gray-600 mb-2">{currentMatch.restaurant1.category}</p>
                    <p className="text-sm text-gray-500">{currentMatch.restaurant1.distance}m</p>
                </div>

                {/* VS 표시 */}
                <div className="text-4xl font-bold text-red-500 px-4">
                    VS
                </div>

                {/* 오른쪽 음식점 */}
                <div
                    onClick={() => handleSelect(currentMatch.restaurant2)}
                    className="flex-1 bg-white rounded-lg shadow-lg p-6 cursor-pointer transition-transform hover:scale-105"
                >
                    <div className="h-48 bg-gray-200 mb-4 rounded-lg flex items-center justify-center">
                        <span className="text-gray-500">이미지</span>
                    </div>
                    <h2 className="text-xl font-bold mb-2">{currentMatch.restaurant2.name}</h2>
                    <p className="text-gray-600 mb-2">{currentMatch.restaurant2.category}</p>
                    <p className="text-sm text-gray-500">{currentMatch.restaurant2.distance}m</p>
                </div>
            </div>

            {/* 진행 상황 바 */}
            <div className="mt-8 bg-gray-200 rounded-full h-2">
                <div
                    className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                    style={{width: `${(currentRound / (round / 2)) * 100}%`}}
                />
            </div>
        </div>
    );
};

export default WorldCupGame;