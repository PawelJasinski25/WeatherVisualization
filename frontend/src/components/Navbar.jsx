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
        backgroundColor: '#fff',
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
        fontSize: '1.4rem',
        fontWeight: 'bold',
        color: '#333'
    },
    left: { display: 'flex', alignItems: 'center' },
    right: { display: 'flex', alignItems: 'center', gap: '15px' },

    uploadBtn: {
        padding: '8px 16px',
        backgroundColor: '#10b981',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: '600',
        fontSize: '0.9rem',
        display: 'flex',
        alignItems: 'center',
        gap: '5px'
    },
    logoutBtn: {
        background: 'none',
        border: 'none',
        color: '#666',
        cursor: 'pointer',
        fontSize: '0.9rem',
        fontWeight: '500'
    },
    separator: {
        width: '1px',
        height: '20px',
        backgroundColor: '#ddd'
    }
};

export default Navbar;