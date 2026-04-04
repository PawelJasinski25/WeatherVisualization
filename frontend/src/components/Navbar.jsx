import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import '../styles/navbar.css';

const Navbar = ({ onOpenUpload, activeTab = 'map', currentTripId = null }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleLogout = () => {
        authService.logout();
        navigate('/login');
    };

    return (
        <nav className="navbar">
            <div className="navbar-left">
                <span className="navbar-logo">🌍 WeatherVisualization</span>
            </div>

            <div className="navbar-center">
                <button
                    className={`navbar-tab ${activeTab === 'map' ? 'active' : ''}`}
                    onClick={() => navigate('/dashboard', { state: { tripId: currentTripId } })}
                >
                    🗺️ Mapa
                </button>
                <button
                    className={`navbar-tab ${activeTab === 'animation' ? 'active' : ''}`}
                    onClick={() => navigate('/animation', { state: { tripId: currentTripId } })}
                >
                    🎞️ Animacja
                </button>
                <button className="navbar-tab">📊 Wykresy</button>
                <button className="navbar-tab">📄 Raport</button>
            </div>

            <div className="navbar-right">
                <button onClick={onOpenUpload} className="upload-btn">
                    ➕ Nowa Trasa
                </button>

                <div className="navbar-separator"></div>

                <div className="user-menu-container" ref={menuRef}>
                    <button onClick={() => setIsMenuOpen(!isMenuOpen)} className="user-menu-btn">
                        👤 Konto <span style={{ fontSize: '0.7em', marginLeft: '4px' }}>▼</span>
                    </button>

                    {isMenuOpen && (
                        <div className="dropdown">
                            <button
                                className="dropdown-item"
                                onClick={() => { setIsMenuOpen(false); navigate('/trips'); }}
                            >
                                🗺️ Wgrane trasy
                            </button>
                            <button className="dropdown-item" onClick={() => setIsMenuOpen(false)}>
                                ⚙️ Ustawienia
                            </button>
                            <div className="dropdown-divider"></div>
                            <button onClick={handleLogout} className="dropdown-item logout">
                                🚪 Wyloguj
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
};

export default Navbar;