import React, {useState, lazy, Suspense} from 'react';
import {Link, useNavigate} from 'react-router-dom';

// 1. 사전 연결 설정
const preconnectUrls = [
    'https://t1.daumcdn.net',
    'https://dapi.kakao.com'
];

// 2. Navigation 컴포넌트 분리 및 메모이제이션
const Navigation = React.memo(() => (
    <nav className="p-4 sm:p-8 flex justify-between items-center">
        <Link to="/" className="text-xl sm:text-2xl font-bold tracking-wider">YUMCUP</Link>
        <div className="flex gap-4 sm:gap-8">
            <Link to="/about" className="text-gray-700 text-sm sm:text-base hover:text-gray-900">About</Link>
            <a
                href="https://github.com/yybmion/yumcup"
                target="_blank"
                rel="noopener noreferrer"
                className="text-gray-700 text-sm sm:text-base hover:text-gray-900"
            >
                Github
            </a>
            <Link to="/contact" className="text-gray-700 text-sm sm:text-base hover:text-gray-900">Contact</Link>
        </div>
    </nav>
));

// 3. 지연 로딩을 위한 LoadingSpinner 컴포넌트 분리
const LoadingSpinner = React.memo(() => (
    <>
        <span className="opacity-0">시작하기</span>
        <div className="absolute inset-0 flex items-center justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
        </div>
    </>
));

const Home = () => {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);

    // 4. 컴포넌트 마운트 시 사전 연결 설정
    React.useEffect(() => {
        preconnectUrls.forEach(url => {
            const link = document.createElement('link');
            link.rel = 'preconnect';
            link.href = url;
            document.head.appendChild(link);
        });
    }, []);

    // 5. 위치 정보 획득 로직 분리
    const getGeolocation = async () => {
        if (!("geolocation" in navigator)) {
            throw new Error('이 브라우저에서는 위치 기반 서비스를 사용할 수 없습니다.');
        }
        return new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(resolve, reject);
        });
    };

    // 6. API 호출 로직 분리
    const fetchNearbyRestaurants = async (latitude, longitude, radius) => {
        const response = await fetch(`${process.env.REACT_APP_API_URL}/yumcup/start/location`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({latitude, longitude, radius})
        });

        console.log('API URL:', process.env.REACT_APP_API_URL);

        if (!response.ok) {
            throw new Error('주변 음식점을 찾을 수 없습니다.');
        }

        return response.json();
    };

    const handleStartGame = async () => {
        try {
            setIsLoading(true);
            const position = await getGeolocation();
            const data = await fetchNearbyRestaurants(
                position.coords.latitude,
                position.coords.longitude,
                500
            );
            navigate('/worldcup', {state: {gameData: data}});
        } catch (error) {
            if (error.code === 1) {
                alert('위치 정보 접근을 허용해주세요.');
            } else {
                alert(error.message || '오류가 발생했습니다. 다시 시도해주세요.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    // 7. 중요한 CSS를 인라인으로 포함
    const mainContentStyle = `
    text-center 
    max-w-2xl
  `;

    return (
        <div className="min-h-screen flex flex-col">
            <Navigation/>

            <main className="flex-1 flex justify-center items-center p-4 sm:p-8">
                <div className={mainContentStyle}>
                    <h1 className="text-4xl sm:text-5xl font-bold mb-4 sm:mb-6">YUMCUP</h1>
                    <p className="text-base sm:text-lg text-gray-600 leading-relaxed mb-6 sm:mb-8">
                        내 주변의 맛있는 발견<br/>
                        취향저격 맛집 찾기의 새로운 재미
                    </p>
                    <button
                        onClick={handleStartGame}
                        disabled={isLoading}
                        className={`relative bg-gray-900 text-white px-6 sm:px-8 py-3 sm:py-4 rounded-lg 
                     text-base sm:text-lg font-medium hover:bg-gray-800 
                     transition-colors duration-200 ${isLoading ? 'opacity-75 cursor-not-allowed' : ''}`}
                    >
                        {isLoading ? <LoadingSpinner/> : '시작하기'}
                    </button>
                </div>
            </main>

            <footer className="p-4 sm:p-8 text-center text-sm text-gray-500">
                <p>© 2024 YUMCUP. All rights reserved.</p>
            </footer>
        </div>
    );
};

export default Home;