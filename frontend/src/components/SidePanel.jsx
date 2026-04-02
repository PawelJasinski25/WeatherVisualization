import React from 'react';


const COLORS = {
    PRIMARY: ['#3b82f6', '#f97316'],
    SECONDARY: ['#109346', '#a11ecf'],
    INACTIVE: '#000'
};

const primaryParams = [
    { id: 'wind', label: 'Siła wiatru', icon: '/icons/wind.png', scale: 1.2 },
    { id: 'gusts', label: 'Porywy wiatru', icon: '/icons/wind.png', scale: 1.2 },
    { id: 'temp', label: 'Temperatura', icon: '/icons/temperature.png' },
    { id: 'dew', label: 'Punkt rosy', icon: '/icons/dewpoint.png' },
    { id: 'rain', label: 'Deszcz', icon: '/icons/rain.png', scale: 1.1 },
    { id: 'snow', label: 'Śnieg', icon: '/icons/snowflake.png', scale: 1.0 },
    { id: 'humidity', label: 'Wilgotność', icon: '/icons/humidity.png' },
    { id: 'pressure', label: 'Ciśnienie', icon: '/icons/air_pressure.png', scale: 1.6 },
    { id: 'clouds', label: 'Zachmurzenie', icon: '/icons/cloud.png', scale: 1.4 },
    { id: 'clouds_low', label: 'Chmury niskie', icon: '/icons/cloud_low.png', scale: 1.4 },
    { id: 'clouds_mid', label: 'Chmury średnie', icon: '/icons/cloud_mid.png', scale: 1.4 },
    { id: 'clouds_high', label: 'Chmury wysokie', icon: '/icons/cloud_high.png', scale: 1.4 },
    { id: 'wave_h', label: 'Wysokość fal', icon: '/icons/wave.png' },
    { id: 'wave_p', label: 'Okres fal', icon: '/icons/wave.png' },
    { id: 'wind_wave_h', label: 'Wysokość fal wiatrowych', icon: '/icons/wind_wave.png', scale:1.2 },
    { id: 'wind_wave_p', label: 'Okres fal wiatrowych', icon: '/icons/wind_wave.png', scale:1.2 },
    { id: 'swell_wave_h', label: 'Wysokość fal martwych', icon: '/icons/swell_wave.png' },
    { id: 'swell_wave_p', label: 'Okres fal martwych', icon: '/icons/swell_wave.png' },
    { id: 'ocean_current_velocity', label: 'Prędkość prądów', icon: '/icons/ocean_current.png' },
    { id: 'sea_temperature', label: 'Temperatura morza', icon: '/icons/sea.png', scale:1.8 }

];

const secondaryParams = [
    { id: 'wind_dir', label: 'Kierunek wiatru', icon: '/icons/direction.png' },
    { id: 'wind', label: 'Siła wiatru', icon: '/icons/wind.png', scale: 1.2 },
    { id: 'gusts', label: 'Porywy wiatru', icon: '/icons/wind.png', scale: 1.2 },
    { id: 'temp', label: 'Temperatura', icon: '/icons/temperature.png' },
    { id: 'dew', label: 'Punkt rosy', icon: '/icons/dewpoint.png' },
    { id: 'rain', label: 'Deszcz', icon: '/icons/rain.png', scale: 1.1 },
    { id: 'snow', label: 'Śnieg', icon: '/icons/snowflake.png', scale: 1.0 },
    { id: 'humidity', label: 'Wilgotność', icon: '/icons/humidity.png' },
    { id: 'pressure', label: 'Ciśnienie', icon: '/icons/air_pressure.png', scale: 1.6 },
    { id: 'clouds', label: 'Zachmurzenie', icon: '/icons/cloud.png', scale: 1.4 },
    { id: 'clouds_low', label: 'Chmury niskie', icon: '/icons/cloud_low.png', scale: 1.4 },
    { id: 'clouds_mid', label: 'Chmury średnie', icon: '/icons/cloud_mid.png', scale: 1.4 },
    { id: 'clouds_high', label: 'Chmury wysokie', icon: '/icons/cloud_high.png', scale: 1.4 },
    { id: 'wave_h', label: 'Wysokość fal', icon: '/icons/wave.png' },
    { id: 'wave_p', label: 'Okres fal', icon: '/icons/wave.png' },
    { id: 'wind_wave_h', label: 'Wysokość fal wiatrowych', icon: '/icons/wind_wave.png', scale:1.2 },
    { id: 'wind_wave_p', label: 'Okres fal wiatrowych', icon: '/icons/wind_wave.png', scale:1.2 },
    { id: 'swell_wave_h', label: 'Wysokość fal martwych', icon: '/icons/swell_wave.png' },
    { id: 'swell_wave_p', label: 'Okres fal martwych', icon: '/icons/swell_wave.png' },
    { id: 'ocean_current_velocity', label: 'Prędkość prądów', icon: '/icons/ocean_current.png' },
    { id: 'sea_temperature', label: 'Temperatura morza', icon: '/icons/sea.png', scale:1.8 },
    { id: 'ocean_current_direction', label: 'Kierunek prądów', icon: '/icons/direction.png' },
    { id: 'wave_dir', label: 'Kierunek fal', icon: '/icons/direction.png' }
];

const SidePanel = ({ selectedPrimary, setSelectedPrimary, selectedSecondary, setSelectedSecondary, isOpen, setIsOpen }) => {

    const togglePrimary = (id) => {
        const index = selectedPrimary.indexOf(id);
        if (index !== -1) {
            const newArr = [...selectedPrimary];
            newArr[index] = null;
            setSelectedPrimary(newArr);
        } else {
            const nullIndex = selectedPrimary.indexOf(null);
            if (nullIndex !== -1) {
                const newArr = [...selectedPrimary];
                newArr[nullIndex] = id;
                setSelectedPrimary(newArr);
            } else if (selectedPrimary.length < 2) {
                setSelectedPrimary([...selectedPrimary, id]);
            }
        }
    };

    const toggleSecondary = (id) => {
        const index = selectedSecondary.indexOf(id);
        if (index !== -1) {
            const newArr = [...selectedSecondary];
            newArr[index] = null;
            setSelectedSecondary(newArr);
        } else {
            const nullIndex = selectedSecondary.indexOf(null);
            if (nullIndex !== -1) {
                const newArr = [...selectedSecondary];
                newArr[nullIndex] = id;
                setSelectedSecondary(newArr);
            } else if (selectedSecondary.length < 2) {
                setSelectedSecondary([...selectedSecondary, id]);
            }
        }
    };

    const renderButton = (param, selectedArray, onClick, colorPalette) => {
        const index = selectedArray.indexOf(param.id);
        const isSelected = index !== -1;

        let themeColor = COLORS.INACTIVE;
        if (isSelected) {
            themeColor = index === 0 ? colorPalette[0] : colorPalette[1];
        }

        const activeStyle = isSelected ? {
            borderColor: themeColor,
            backgroundColor: `${themeColor}16`,
            color: '#000'
        } : {};

        return (
            <button
                key={param.id}
                style={{ ...styles.button, ...activeStyle }}
                onClick={() => onClick(param.id)}
            >
                <div style={styles.iconContainer}>
                    <div style={{
                        ...styles.imgIcon,
                        backgroundColor: themeColor,
                        WebkitMaskImage: `url(${param.icon})`,
                        WebkitMaskSize: 'contain',
                        WebkitMaskRepeat: 'no-repeat',
                        WebkitMaskPosition: 'center',
                        maskImage: `url(${param.icon})`,
                        maskSize: 'contain',
                        maskRepeat: 'no-repeat',
                        maskPosition: 'center',
                        transform: `scale(${param.scale || 1})`
                    }} />
                </div>
                <span style={styles.label}>{param.label}</span>
            </button>
        );
    };

    return (
        <div style={{
            ...styles.wrapper,
            transform: isOpen ? 'translateX(-20px)' : 'translateX(100%)'
        }}>

            <button onClick={() => setIsOpen(!isOpen)} style={styles.toggleBtn}>
                {isOpen ? '▶' : '◀'}
            </button>

            <div style={styles.panelContent}>
                <div style={styles.section}>
                    <h3 style={styles.title}>Parametry podstawowe</h3>
                    <p style={styles.subtitle}>wyświetlane jako gradienty na trasie (max 2)</p>
                    <div style={styles.grid}>
                        {primaryParams.map(param =>
                            renderButton(param, selectedPrimary, togglePrimary, COLORS.PRIMARY)
                        )}
                    </div>
                </div>

                <div style={styles.section}>
                    <h3 style={styles.title}>Parametry dodatkowe</h3>
                    <p style={styles.subtitle}>wyświetlane jako etykieta po przybliżeniu (max 2)</p>
                    <div style={styles.grid}>
                        {secondaryParams.map(param =>
                            renderButton(param, selectedSecondary, toggleSecondary, COLORS.SECONDARY)
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

const styles = {
    wrapper: {
        position: 'absolute',
        top: '20px',
        right: '0',
        zIndex: 1000,
        transition: 'transform 0.3s ease-in-out',
    },
    panelContent: {
        width: 'min(90vw, 450px)',
        maxHeight: 'calc(100vh - 100px)',
        overflowY: 'auto',
        boxSizing: 'border-box',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(5px)',
        borderRadius: '8px',
        boxShadow: '0 4px 15px rgba(0,0,0,0.15)',
        padding: 'clamp(15px, 2vw, 20px)',
        display: 'flex',
        flexDirection: 'column',
        gap: 'clamp(10px, 1.5vw, 20px)',
    },
    toggleBtn: {
        position: 'absolute',
        left: '-32px',
        top: '0px',
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
        display: 'inline-block',
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
    iconContainer: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0
    },
    label: {
        fontSize: '0.75rem',
        textAlign: 'center',
        fontWeight: '600'
    }
};

export default SidePanel;