import React, {useState, useEffect, useRef, useCallback} from 'react';
import {Link, useNavigate} from 'react-router-dom';

// 1. 사전 연결 설정 (백엔드 API URL도 추가)
const preconnectUrls = [
    'https://t1.daumcdn.net',
    'https://dapi.kakao.com',
    process.env.REACT_APP_API_URL // 백엔드 API에도 미리 연결
].filter(Boolean); // null/undefined 제거

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

// 3. 로딩 상태에 따른 메시지를 보여주는 LoadingSpinner 컴포넌트
const LoadingSpinner = React.memo(({ loadingStage }) => {
    // 로딩 단계별 메시지
    const stageMessages = {
        location: '위치 확인 중...',
        api: '맛집 검색 중...',
        default: '준비 중...'
    };

    return (
        <div className="flex flex-col items-center gap-2">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
            <span className="text-sm">{stageMessages[loadingStage] || stageMessages.default}</span>
        </div>
    );
});

const Home = () => {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [loadingStage, setLoadingStage] = useState('default'); // 로딩 단계 표시

    // 🚀 핵심 최적화: 위치 정보를 미리 캐시
    const cachedPositionRef = useRef(null);
    const [locationStatus, setLocationStatus] = useState('idle'); // idle, fetching, ready, error

    // 🚀 위치 정보 미리 가져오기 (백그라운드) - useEffect 전에 정의
    const prefetchLocation = useCallback(() => {
        if (!("geolocation" in navigator)) {
            setLocationStatus('error');
            return;
        }

        setLocationStatus('fetching');

        navigator.geolocation.getCurrentPosition(
            (position) => {
                // 위치 정보를 캐시에 저장
                cachedPositionRef.current = position;
                setLocationStatus('ready');
                console.log('📍 위치 정보 미리 획득 완료!');
            },
            (error) => {
                // 에러가 나도 괜찮음 - 버튼 클릭 시 다시 시도
                console.log('📍 위치 정보 사전 획득 실패 (버튼 클릭 시 재시도)');
                setLocationStatus('error');
            },
            {
                enableHighAccuracy: false, // 빠른 응답을 위해 false
                timeout: 5000,
                maximumAge: 60000 // 1분간 캐시 허용
            }
        );
    }, []);

    // 4. 컴포넌트 마운트 시 사전 연결 설정 + 위치 정보 미리 요청
    useEffect(() => {
        // preconnect 설정
        preconnectUrls.forEach(url => {
            const link = document.createElement('link');
            link.rel = 'preconnect';
            link.href = url;
            document.head.appendChild(link);
        });

        // 🚀 페이지 로드 시 위치 정보 미리 요청 (백그라운드에서)
        prefetchLocation();
    }, [prefetchLocation]);

    // 5. 위치 정보 획득 로직 (캐시 우선 사용)
    const getGeolocation = async () => {
        if (!("geolocation" in navigator)) {
            throw new Error('이 브라우저에서는 위치 기반 서비스를 사용할 수 없습니다.');
        }

        // 🚀 캐시된 위치가 있으면 바로 반환 (시간 절약!)
        if (cachedPositionRef.current) {
            console.log('📍 캐시된 위치 정보 사용 (즉시 응답!)');
            return cachedPositionRef.current;
        }

        // 캐시가 없으면 새로 요청
        console.log('📍 위치 정보 새로 요청...');
        return new Promise((resolve, reject) => {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    cachedPositionRef.current = position; // 캐시 업데이트
                    resolve(position);
                },
                reject,
                {
                    enableHighAccuracy: false,
                    timeout: 10000,
                    maximumAge: 60000
                }
            );
        });
    };

    // 6. API 호출 로직 분리
    const fetchNearbyRestaurants = async (latitude, longitude, radius) => {
        console.log('API URL:', process.env.REACT_APP_API_URL);
        console.log('Request Payload:', { latitude, longitude, radius });
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/api/yumcup/start/location`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({latitude, longitude, radius})
            });
            console.log('Response status:', response.status);
            console.log('Response headers:', Object.fromEntries(response.headers));
            if (!response.ok) {
                const errorText = await response.text();
                console.error('Error response:', errorText);
                throw new Error('주변 음식점을 찾을 수 없습니다.');
            }
            const data = await response.json();
            console.log('Response data:', data);
            return data;
        } catch (error) {
            console.error('Fetch error:', error);
            console.error('Error details:', {
                name: error.name,
                message: error.message,
                stack: error.stack
            });
            throw error;
        }
    };
    const handleStartGame = async () => {
        try {
            setIsLoading(true);

            // 1단계: 위치 정보 획득
            setLoadingStage('location');
            const position = await getGeolocation();

            // 2단계: API 호출 (이 부분이 제일 오래 걸림)
            setLoadingStage('api');
            const data = await fetchNearbyRestaurants(
                position.coords.latitude,
                position.coords.longitude,
                1000
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
            setLoadingStage('default');
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
            <main className="flex-1 flex justify-center items-center p-4 sm:p-8 min-h-[calc(100vh-200px)]">
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
                     transition-colors duration-200 min-w-[140px] ${isLoading ? 'opacity-75 cursor-not-allowed' : ''}`}
                    >
                        {isLoading ? <LoadingSpinner loadingStage={loadingStage} /> : '시작하기'}
                    </button>
                    {/* 위치 준비 상태 표시 (사용자에게 피드백) */}
                    {locationStatus === 'ready' && !isLoading && (
                        <p className="text-xs text-green-600 mt-2">📍 위치 준비 완료</p>
                    )}
                </div>
            </main>

            <div>
                <footer className="p-4 sm:p-8 text-center text-sm text-gray-500">
                    <p>© 2024 YUMCUP. All rights reserved.</p>
                </footer>
            </div>
        </div>
    );
};
export default Home;
