import React from 'react';
import '../styles/panel.css';

const allParams = [
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

const AnimationPanel = ({ selectedParams, setSelectedParams, isOpen, setIsOpen }) => {

    const toggleParam = (id) => {
        if (selectedParams.includes(id)) {
            setSelectedParams(selectedParams.filter(p => p !== id));
        } else {
            if (selectedParams.length < 3) {
                setSelectedParams([...selectedParams, id]);
            } else {
                alert("Możesz wybrać maksymalnie 3 parametry do animacji.");
            }
        }
    };

    return (
        <div className="panel-wrapper" style={{ transform: isOpen ? 'translateX(-20px)' : 'translateX(100%)' }}>
            <button onClick={() => setIsOpen(!isOpen)} className="panel-toggle-btn">
                {isOpen ? '▶' : '◀'}
            </button>

            <div className="panel-content">
                {/* DODANY DIV: */}
                <div className="panel-section">
                    <h3 className="panel-title">Wybór parametrów</h3>
                    <p className="panel-subtitle">Wybierz maksymalnie 3 parametry do śledzenia</p>

                    <div className="panel-grid">
                        {allParams.map(param => {
                        const isSelected = selectedParams.includes(param.id);
                        const activeStyle = isSelected ? { borderColor: '#3b82f6', backgroundColor: '#eff6ff', color: '#000' } : {};
                        const themeColor = isSelected ? '#3b82f6' : '#000';

                        return (
                            <button key={param.id} className="panel-btn" style={activeStyle} onClick={() => toggleParam(param.id)}>
                                <div className="panel-icon-container">
                                    <div className="panel-icon" style={{
                                        backgroundColor: themeColor,
                                        WebkitMaskImage: `url(${param.icon})`, WebkitMaskSize: 'contain', WebkitMaskRepeat: 'no-repeat', WebkitMaskPosition: 'center',
                                        transform: `scale(${param.scale || 1})`
                                    }} />
                                </div>
                                <span className="panel-label">{param.label}</span>
                            </button>
                        );
                    })}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AnimationPanel;