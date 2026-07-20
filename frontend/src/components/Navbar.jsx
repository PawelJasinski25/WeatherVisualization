import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import '../styles/navbar.css';
import SettingsModal from './SettingsModal';
import {CloudSun, Map, PlayCircle, FileText, Plus, User, ChevronDown, Route, Settings, LogOut} from 'lucide-react';

const Navbar = ({ onOpenUpload, activeTab = 'map', currentTripId = null }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
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
                <span
                    className="navbar-logo"
                    style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}
                    onClick={() => navigate('/about')}
                    title="Informacje o aplikacji"
                >
                     <CloudSun size={24} color="#1e40af" /> WeatherVisualization
                </span>
            </div>

            <div className="navbar-center">
                <button
                    className={`navbar-tab ${activeTab === 'map' ? 'active' : ''}`}
                    onClick={() => navigate('/dashboard', { state: { tripId: currentTripId } })}
                >
                    <Map size={20} color="var(--theme-map)" /> Mapa
                </button>

                <button
                    className={`navbar-tab ${activeTab === 'animation' ? 'active' : ''}`}
                    onClick={() => navigate('/animation', { state: { tripId: currentTripId } })}
                >
                    <PlayCircle size={20} color="var(--theme-anim)" /> Animacja
                </button>

                <button
                    className={`navbar-tab ${activeTab === 'report' ? 'active' : ''}`}
                    onClick={() => navigate('/report', { state: { tripId: currentTripId } })}
                >
                    <FileText size={20} color="var(--theme-report)" /> Raport
                </button>
            </div>

            <div className="navbar-right">

                <button onClick={onOpenUpload} className="upload-btn">
                    <Plus size={20} /> Nowa Trasa
                </button>

                <div className="navbar-separator"></div>

                <div className="user-menu-container" ref={menuRef}>

                    <button onClick={() => setIsMenuOpen(!isMenuOpen)} className="user-menu-btn">
                        <User size={20}  /> Konto <ChevronDown size={16}  />
                    </button>

                    {isMenuOpen && (
                        <div className="dropdown">

                            <button
                                className="dropdown-item"
                                onClick={() => { setIsMenuOpen(false); navigate('/trips'); }}
                            >
                                <Route size={18}  /> Moje trasy
                            </button>

                            <button className="dropdown-item" onClick={() => { setIsMenuOpen(false); setIsSettingsOpen(true); }}>
                                <Settings size={18} /> Jednostki
                            </button>

                            <div className="dropdown-divider"></div>
                            <button onClick={handleLogout} className="dropdown-item logout">
                                <LogOut size={18} /> Wyloguj
                            </button>
                        </div>
                    )}
                </div>
            </div>
            <SettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
        </nav>
    );
};

export default Navbar;