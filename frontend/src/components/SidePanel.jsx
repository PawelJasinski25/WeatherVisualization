import React, { useState } from 'react';

const primaryParams = [
    { id: 'wind', label: 'Siła wiatru', icon: '/icons/wind.png', color: '#3b82f6' },
    { id: 'gusts', label: 'Porywy wiatru', icon: '🍃', color: '#6366f1' },
    { id: 'temp', label: 'Temperatura', icon: '/icons/temperature.png', color: '#f97316' },
    { id: 'dew', label: 'Punkt rosy', icon: '💧', color: '#0ea5e9' },
    { id: 'wave_h', label: 'Wysokość fal', icon: '🌊', color: '#0284c7' },
    { id: 'wave_p', label: 'Okres fal', icon: '⏱️', color: '#0369a1' }
];

const secondaryParams = [
    { id: 'wind_dir', label: 'Kierunek wiatru', icon: '🌬️', color: '#3b82f6' },
    { id: 'rain', label: 'Deszcz', icon: '🌧️', color: '#3b82f6' },
    { id: 'humidity', label: 'Wilgotność', icon: '💦', color: '#3b82f6' },
    { id: 'pressure', label: 'Ciśnienie', icon: '📉', color: '#3b82f6' },
    { id: 'wave_dir', label: 'Kierunek fal', icon: '🌊', color: '#3b82f6' },
    { id: 'clouds', label: 'Zachmurzenie', icon: '☁️', color: '#3b82f6' }
];

const SidePanel = ({ selectedPrimary, setSelectedPrimary, selectedSecondary, setSelectedSecondary }) => {
    const [isOpen, setIsOpen] = useState(true);

    const togglePrimary = (id) => {
        if (selectedPrimary.includes(id)) {
            setSelectedPrimary(selectedPrimary.filter(item => item !== id));
        } else {
            setSelectedPrimary([...selectedPrimary, id]);
        }
    };

    const toggleSecondary = (id) => {
        if (selectedSecondary.includes(id)) {
            setSelectedSecondary(selectedSecondary.filter(item => item !== id));
        } else {
            setSelectedSecondary([...selectedSecondary, id]);
        }
    };

    const renderButton = (param, isSelected, onClick) => {
        const activeStyle = isSelected ? {
            borderColor: param.color,
            backgroundColor: `${param.color}15`,
            color: '#000',
            fontWeight: '600'
        } : {};

        return (
            <button
                key={param.id}
                style={{ ...styles.button, ...activeStyle }}
                onClick={() => onClick(param.id)}
            >
                <span style={styles.icon}>
                    {param.icon.includes('.png') || param.icon.includes('.svg') || param.icon.includes('http') ? (
                        <img src={param.icon} alt={param.label} style={styles.imgIcon} />
                    ) : (
                        param.icon
                    )}
                </span>
                <span style={styles.label}>{param.label}</span>
            </button>
        );
    };

    return (
        <div style={{ ...styles.panelContainer, right: isOpen ? '20px' : '-320px' }}>

            <button onClick={() => setIsOpen(!isOpen)} style={styles.toggleBtn}>
                {isOpen ? '▶' : '◀'}
            </button>

            <div style={styles.section}>
                <h3 style={styles.title}>Parametry podstawowe</h3>
                <p style={styles.subtitle}>wyświetlane jako gradienty na trasie</p>
                <div style={styles.grid}>
                    {primaryParams.map(param =>
                        renderButton(param, selectedPrimary.includes(param.id), togglePrimary)
                    )}
                </div>
            </div>

            <div style={styles.section}>
                <h3 style={styles.title}>Parametry dodatkowe</h3>
                <p style={styles.subtitle}>wyświetlane jako etykieta po przybliżeniu</p>
                <div style={styles.grid}>
                    {secondaryParams.map(param =>
                        renderButton(param, selectedSecondary.includes(param.id), toggleSecondary)
                    )}
                </div>
            </div>

        </div>
    );
};

const styles = {
    panelContainer: {
        position: 'absolute',
        top: '20px',
        width: '320px',
        boxSizing: 'border-box',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(5px)',
        borderRadius: '8px',
        boxShadow: '0 4px 15px rgba(0,0,0,0.15)',
        padding: '20px',
        zIndex: 1000,
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
    },
    toggleBtn: {
        position: 'absolute',
        left: '-32px',
        top: '20px',
        width: '32px',
        height: '48px',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        border: '1px solid #ddd',
        borderRight: 'none',
        borderRadius: '8px 0 0 8px',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#555',
        fontSize: '1rem',
        boxShadow: '-4px 4px 10px rgba(0,0,0,0.05)',
        backdropFilter: 'blur(5px)'
    },

    imgIcon: {
        width: '28px',
        height: '28px',
        objectFit: 'contain',
        marginBottom: '4px'
    },

    section: {
        display: 'flex',
        flexDirection: 'column'
    },
    title: {
        margin: '0 0 2px 0',
        fontSize: '1.05rem',
        color: '#222'
    },
    subtitle: {
        margin: '0 0 12px 0',
        fontSize: '0.75rem',
        fontStyle: 'italic',
        color: '#666'
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '8px'
    },
    button: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '10px 5px',
        backgroundColor: '#fff',
        border: '1px solid #ddd',
        borderRadius: '6px',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        color: '#444'
    },
    icon: {
        fontSize: '1.4rem',
        marginBottom: '4px'
    },
    label: {
        fontSize: '0.75rem',
        textAlign: 'center',
        fontWeight: '500'
    }
};

export default SidePanel;