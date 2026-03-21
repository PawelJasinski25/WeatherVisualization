import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

const Navbar = ({ onOpenUpload }) => {
    const navigate = useNavigate();

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

                <button onClick={handleLogout} style={styles.logoutBtn}>
                    Wyloguj
                </button>
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
    logoutBtn: {
        background: 'none',
        border: 'none',
        color: '#333',
        cursor: 'pointer',
        fontSize: '0.95rem',
        fontWeight: '600'
    }
};

export default Navbar;