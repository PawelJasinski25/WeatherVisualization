import React from 'react';
import '../styles/panel.css';

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
    { id: 'sea_temperature', label: 'Temperatura morza', icon: '/icons/sea.png', scale:1.8 },
    { id: 'astronomy', label: 'Pory dnia', icon: '/icons/time.png', scale: 1.0 },

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
                className="panel-btn"
                style={activeStyle}
                onClick={() => onClick(param.id)}
            >
                <div className="panel-icon-container">
                    <div className="panel-icon" style={{
                        backgroundColor: themeColor,
                        WebkitMaskImage: `url(${param.icon})`, WebkitMaskSize: 'contain', WebkitMaskRepeat: 'no-repeat', WebkitMaskPosition: 'center',
                        maskImage: `url(${param.icon})`, maskSize: 'contain', maskRepeat: 'no-repeat', maskPosition: 'center',
                        transform: `scale(${param.scale || 1})`
                    }} />
                </div>
                <span className="panel-label">{param.label}</span>
            </button>
        );
    };

    return (
        <div className="panel-wrapper" style={{ transform: isOpen ? 'translateX(-20px)' : 'translateX(100%)' }}>

            <button onClick={() => setIsOpen(!isOpen)} className="panel-toggle-btn">
                {isOpen ? '▶' : '◀'}
            </button>

            <div className="panel-content">
                <div className="panel-section">
                    <h3 className="panel-title">Parametry podstawowe</h3>
                    <p className="panel-subtitle">wyświetlane jako gradienty na trasie (max 2)</p>
                    <div className="panel-grid">
                        {primaryParams.map(param =>
                            renderButton(param, selectedPrimary, togglePrimary, COLORS.PRIMARY)
                        )}
                    </div>
                </div>

                <div className="panel-section">
                    <h3 className="panel-title">Parametry dodatkowe</h3>
                    <p className="panel-subtitle">wyświetlane jako etykieta po przybliżeniu (max 2)</p>
                    <div className="panel-grid">
                        {secondaryParams.map(param =>
                            renderButton(param, selectedSecondary, toggleSecondary, COLORS.SECONDARY)
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SidePanel;