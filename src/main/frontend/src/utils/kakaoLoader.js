export const loadKakaoAPI = () => {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = '//dapi.kakao.com/v2/maps/sdk.js';
        script.async = true;
        script.crossOrigin = 'anonymous';
        script.setAttribute('data-appkey', process.env.REACT_APP_KAKAO_API_KEY);

        // 로드 완료 후 초기화 옵션 설정
        script.onload = () => {
            window.Kakao.init({
                appkey: process.env.REACT_APP_KAKAO_API_KEY,
                // 쿠키 관련 옵션 추가
                apiConfig: {
                    webStorageType: 'sessionStorage', // localStorage 대신 sessionStorage 사용
                    restrictedServiceDomain: window.location.hostname // 현재 도메인으로 제한
                }
            });
            resolve();
        };

        script.onerror = reject;
        document.head.appendChild(script);
    });
};