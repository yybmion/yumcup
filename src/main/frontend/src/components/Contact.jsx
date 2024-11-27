import React from 'react';
import { Link } from 'react-router-dom';

const Contact = () => {
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
            <main className="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8">
                <div className="text-center">
                    <h1 className="text-3xl sm:text-4xl font-bold mb-6">Contact</h1>
                    <p className="text-gray-600 mb-8 leading-relaxed">
                        서비스 이용 중 궁금한 점이나 개선사항이 있으시다면<br />
                        아래 이메일로 연락 주시기 바랍니다.
                    </p>
                    <div className="bg-gray-50 p-6 rounded-lg inline-block">
                        <a
                            href="mailto:yyb400@ajou.ac.kr"
                            className="text-blue-600 hover:text-blue-800 flex items-center gap-2"
                        >
                            <span className="text-2xl">📧</span>
                            <span className="font-medium">yyb400@ajou.ac.kr</span>
                        </a>
                    </div>
                </div>
            </main>

            <footer className="p-4 sm:p-8 text-center text-sm text-gray-500">
                <p>© 2024 YUMCUP. All rights reserved.</p>
            </footer>
        </div>
    );
};

export default Contact;