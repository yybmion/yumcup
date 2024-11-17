import React, {useState, useEffect} from 'react';

const WorldCupGame = () => {
    const [restaurants, setRestaurants] = useState([]);
    const [currentPair, setCurrentPair] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [round, setRound] = useState(16); // 16강부터 시작
    const [currentRound, setCurrentRound] = useState(1);
    const [winners, setWinners] = useState([]);

    const startGame = async () => {
        try {
            setIsLoading(true);
            const response = await fetch('/api/yumcup/start', {});
            const data = await response.json();
            console.log('받은 데이터:', data); // 데이터 확인용 로그
            setRestaurants(data);
            if (data && data.length >= 2) {  // 데이터 유효성 검사 추가
                setCurrentPair([data[0], data[1]]);
            }
        } catch (error) {
            console.error('Failed to start game:', error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        startGame();
    }, []);

    const handleSelect = (selected) => {
        setWinners(prev => [...prev, selected]);
        if (currentRound === round / 2) {
            // 현재 라운드의 모든 경기가 끝났을 때
            if (round === 2) {
                // 결승전이 끝났을 때
                console.log('Winner:', selected);
            } else {
                // 다음 라운드 진출
                setRound(round / 2);
                setCurrentRound(1);
                setRestaurants(winners);
                setWinners([]);
                setCurrentPair([winners[0], winners[1]]);
            }
        } else {
            // 다음 경기 진행
            setCurrentRound(prev => prev + 1);
            const nextIndex = currentRound * 2;
            setCurrentPair([restaurants[nextIndex], restaurants[nextIndex + 1]]);
        }
    };

    // 로딩 중이거나 데이터가 없을 때 보여줄 화면
    if (isLoading || !currentPair) {
        return <div className="flex justify-center items-center h-screen">Loading...</div>;
    }

    return (
        <div className="max-w-4xl mx-auto p-8">
            {/* 라운드 정보 */}
            <div className="text-center mb-8">
                <h1 className="text-2xl font-bold mb-2">음식점 월드컵</h1>
                <p className="text-lg text-gray-600">{round}강 - {currentRound}/{round/2}</p>
            </div>

            {/* 대결 구역 */}
            <div className="flex justify-between items-center gap-8">
                {/* 왼쪽 음식점 */}
                <div
                    onClick={() => handleSelect(currentPair[0])}
                    className="flex-1 bg-white rounded-lg shadow-lg p-6 cursor-pointer transition-transform hover:scale-105"
                >
                    <div className="h-48 bg-gray-200 mb-4 rounded-lg flex items-center justify-center">
                        <span className="text-gray-500">이미지</span>
                    </div>
                    <h2 className="text-xl font-bold mb-2">{currentPair[0].name}</h2>
                    <p className="text-gray-600 mb-2">{currentPair[0].category}</p>
                    <p className="text-sm text-gray-500">{currentPair[0].distance}m</p>
                </div>

                {/* VS 표시 */}
                <div className="text-4xl font-bold text-red-500 px-4">
                    VS
                </div>

                {/* 오른쪽 음식점 */}
                <div
                    onClick={() => handleSelect(currentPair[1])}
                    className="flex-1 bg-white rounded-lg shadow-lg p-6 cursor-pointer transition-transform hover:scale-105"
                >
                    <div className="h-48 bg-gray-200 mb-4 rounded-lg flex items-center justify-center">
                        <span className="text-gray-500">이미지</span>
                    </div>
                    <h2 className="text-xl font-bold mb-2">{currentPair[1].name}</h2>
                    <p className="text-gray-600 mb-2">{currentPair[1].category}</p>
                    <p className="text-sm text-gray-500">{currentPair[1].distance}m</p>
                </div>
            </div>

            {/* 진행 상황 바 */}
            <div className="mt-8 bg-gray-200 rounded-full h-2">
                <div
                    className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${(currentRound/(round/2))*100}%` }}
                />
            </div>
        </div>
    );
};

export default WorldCupGame;