import React from 'react';
import { Link } from 'react-router-dom';

const About = () => {
    return (
        <div className="min-h-screen flex flex-col bg-white">
            {/* 네비게이션 */}
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

            {/* 메인 콘텐츠 */}
            <main className="flex-1 container mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
                <div className="max-w-3xl mx-auto">
                    <h1 className="text-4xl font-bold mb-8 text-center">맛있는 선택의 즐거움, YUMCUP</h1>

                    <div className="prose prose-lg max-w-none">
                        <div className="mb-12">
                            <p className="text-lg text-gray-600 mb-6">
                                '오늘 뭐 먹지?'
                            </p>
                            <p className="text-gray-600 mb-6">
                                하루에도 몇 번씩 마주하는 이 질문 앞에서 우리는 종종 망설입니다.
                                특히 새로운 동네에서, 혹은 익숙한 곳에서조차 선택의 어려움을 겪곤 하죠.
                                저도 그랬어요. 맛집 리스트를 하나하나 살피고, 리뷰를 읽어가며 고르는
                                시간이 점심시간보다 더 길어질 때가 있었으니까요.
                            </p>
                            <p className="text-gray-600 mb-6">
                                그래서 YUMCUP이 탄생했습니다.
                            </p>
                        </div>

                        <div className="mb-12">
                            <h2 className="text-2xl font-bold mb-4">왜 YUMCUP인가요?</h2>
                            <div className="space-y-6">
                                <div className="bg-gray-50 p-6 rounded-lg">
                                    <h3 className="font-bold mb-2">👉 즐거운 선택</h3>
                                    <p className="text-gray-600">
                                        맛집 고르기가 스트레스가 아닌 즐거움이 되길 바랐습니다.
                                        친구들과 함께 월드컵 게임을 하듯, 웃으며 선택할 수 있도록 만들었어요.
                                    </p>
                                </div>
                                <div className="bg-gray-50 p-6 rounded-lg">
                                    <h3 className="font-bold mb-2">👉 직관적인 결정</h3>
                                    <p className="text-gray-600">
                                        복잡한 리뷰나 평점에 휘둘리지 않고, 순간의 당신 마음이 원하는 것을 고르세요.
                                        때론 그게 가장 맛있는 선택이 됩니다.
                                    </p>
                                </div>
                                <div className="bg-gray-50 p-6 rounded-lg">
                                    <h3 className="font-bold mb-2">👉 위치 기반 추천</h3>
                                    <p className="text-gray-600">
                                        멀리 있는 맛집 때문에 실망하지 않도록, 현재 위치에서 갈 수 있는
                                        진짜 맛집들만 보여드려요.
                                    </p>
                                </div>
                            </div>
                        </div>

                        <div className="text-center mt-12">
                            <p className="text-gray-600 mb-6">
                                지금 이 순간에도 누군가는 YUMCUP으로 새로운 맛집을 발견하고 있을 거예요.<br />
                                혹시 오늘 점심, 아직 정하지 못하셨나요?
                            </p>
                            <Link to="/" className="bg-gray-900 text-white px-8 py-3 rounded-lg hover:bg-gray-800 transition-colors inline-block">
                                맛집 찾으러 가기
                            </Link>
                        </div>
                    </div>
                </div>
            </main>

            <footer className="p-4 sm:p-8 text-center text-sm text-gray-500">
                <p>© 2024 YUMCUP. All rights reserved.</p>
            </footer>
        </div>
    );
};

export default About;