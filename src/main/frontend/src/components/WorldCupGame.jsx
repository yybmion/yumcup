import React, {useState, useEffect} from 'react';

const WorldCupGame = () => {
    const [restaurants, setRestaurants] = useState([]);
    const [currentPair, setCurrentPair] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

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

    // 로딩 중이거나 데이터가 없을 때 보여줄 화면
    if (isLoading || !currentPair) {
        return <div>Loading...</div>;
    }

    return (
        <div className="p-4">
            {currentPair && currentPair[0] && currentPair[1] && (  // null 체크 추가
                <div className="flex justify-center items-center gap-8">
                    <div
                        className="p-4 border rounded cursor-pointer hover:bg-gray-100"
                        onClick={() => console.log(currentPair[0])}
                    >
                        <h3 className="text-xl font-bold">{currentPair[0].name}</h3>
                        <p className="text-gray-600">{currentPair[0].category}</p>
                        <p className="text-sm">{currentPair[0].distance}m</p>
                    </div>
                    <div className="text-2xl font-bold">VS</div>
                    <div
                        className="p-4 border rounded cursor-pointer hover:bg-gray-100"
                        onClick={() => console.log(currentPair[1])}
                    >
                        <h3 className="text-xl font-bold">{currentPair[1].name}</h3>
                        <p className="text-gray-600">{currentPair[1].category}</p>
                        <p className="text-sm">{currentPair[1].distance}m</p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default WorldCupGame;