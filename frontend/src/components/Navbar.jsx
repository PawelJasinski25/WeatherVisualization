import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

const Navbar = ({ onOpenUpload }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = useRef(null);
    const navigate = useNavigate();

    // Zamykanie menu, po kliknieciu poza
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
        <nav style={styles.nav}>
            <div style={styles.left}>
                <span style={styles.logo}>🌍 WeatherVisualization</span>
            </div>

            <div style={styles.center}>
                <button style={{ ...styles.tab, ...styles.activeTab }}>🗺️ Mapa</button>
                <button style={styles.tab}>🎞️ Animacja</button>
                <button style={styles.tab}>📊 Wykresy</button>
                <button style={styles.tab}>📄 Raport</button>
            </div>

            <div style={styles.right}>
                <button onClick={onOpenUpload} style={styles.uploadBtn}>
                    ➕ Nowa Trasa
                </button>

                <div style={styles.separator}></div>

                {/* Nowe Menu Użytkownika */}
                <div style={styles.userMenuContainer} ref={menuRef}>
                    <button
                        onClick={() => setIsMenuOpen(!isMenuOpen)}
                        style={styles.userMenuBtn}
                    >
                        👤 Konto <span style={{ fontSize: '0.7em', marginLeft: '4px' }}>▼</span>
                    </button>

                    {isMenuOpen && (
                        <div style={styles.dropdown}>
                            <button
                                style={styles.dropdownItem}
                                onClick={() => {
                                    setIsMenuOpen(false);
                                    navigate('/trips'); // <-- Podpięte przekierowanie
                                }}
                            >
                                🗺️ Wgrane trasy
                            </button>
                            <button style={styles.dropdownItem} onClick={() => setIsMenuOpen(false)}>
                                ⚙️ Ustawienia
                            </button>
                            <div style={styles.dropdownDivider}></div>
                            <button onClick={handleLogout} style={{ ...styles.dropdownItem, ...styles.logoutText }}>
                                🚪 Wyloguj
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </nav>
    );
};

const styles = {
    nav: {
        height: '60px',
        backgroundColor: '#f8f9fa',
        borderBottom: '1px solid #ddd',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '0 20px',
        boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
        zIndex: 1000,
        position: 'relative'
    },
    logo: {
        fontSize: '1.2rem',
        fontWeight: 'bold',
        color: '#111'
    },
    left: { display: 'flex', alignItems: 'center', flex: 1 },

    center: {
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        flex: 2,
        justifyContent: 'center'
    },
    tab: {
        background: 'none',
        border: 'none',
        padding: '8px 60px',
        fontSize: '1rem',
        fontWeight: '500',
        color: '#444',
        cursor: 'pointer',
        borderRadius: '6px',
        display: 'flex',
        alignItems: 'center',
        gap: '50px',
        transition: 'background 0.2s'
    },
    activeTab: {
        backgroundColor: '#e5e7eb',
        color: '#000',
        fontWeight: '600'
    },

    right: { display: 'flex', alignItems: 'center', gap: '20px', flex: 1, justifyContent: 'flex-end' },
    uploadBtn: {
        padding: '8px 16px',
        backgroundColor: '#c6f6d5',
        color: '#166534',
        border: '1px solid #bbf7d0',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: '600',
        fontSize: '0.95rem',
        display: 'flex',
        alignItems: 'center',
        gap: '5px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
    },
    separator: {
        width: '1px',
        height: '30px',
        backgroundColor: '#ddd'
    },
    userMenuContainer: {
        position: 'relative',
        display: 'flex',
        alignItems: 'center'
    },
    userMenuBtn: {
        background: 'none',
        border: 'none',
        color: '#333',
        cursor: 'pointer',
        fontSize: '0.95rem',
        fontWeight: '600',
        display: 'flex',
        alignItems: 'center',
        padding: '6px 12px',
        borderRadius: '6px',
    },
    dropdown: {
        position: 'absolute',
        top: 'calc(100% + 10px)',
        right: '0',
        backgroundColor: '#fff',
        border: '1px solid #ddd',
        borderRadius: '8px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
        display: 'flex',
        flexDirection: 'column',
        minWidth: '180px',
        overflow: 'hidden',
        zIndex: 1001
    },
    dropdownItem: {
        background: 'none',
        border: 'none',
        padding: '12px 16px',
        textAlign: 'left',
        cursor: 'pointer',
        fontSize: '0.9rem',
        color: '#444',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        width: '100%'
    },
    dropdownDivider: {
        height: '1px',
        backgroundColor: '#eee',
        margin: '4px 0'
    },
    logoutText: {
        color: '#dc2626',
        fontWeight: '600'
    }
};

export default Navbar;