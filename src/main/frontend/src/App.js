import React from 'react';
import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';
import Home from './components/Home';
import WorldCupGame from './components/WorldCupGame';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Home/>}/>
                <Route path="/worldcup" element={<WorldCupGame/>}/>
            </Routes>
        </Router>
    );
}

export default App;