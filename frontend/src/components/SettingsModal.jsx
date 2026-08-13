import React from 'react';
import { useUnits } from '../contexts/UnitContext';
import '../styles/modal.css';

const SettingsModal = ({ isOpen, onClose }) => {
    const { units, updateUnit } = useUnits();

    if (!isOpen) return null;

    const formatTzLabel = (tz) => {
        if (tz === 'UTC') return 'UTC';
        try {
            const offset = new Intl.DateTimeFormat('en-US', {
                timeZone: tz,
                timeZoneName: 'shortOffset'
            }).formatToParts(new Date()).find(p => p.type === 'timeZoneName')?.value || '';

            return `${tz} (${offset.replace('GMT', 'UTC')})`;
        } catch {
            return tz;
        }
    };

    const timezoneOptions = ['UTC', ...Intl.supportedValuesOf('timeZone').filter(tz => tz !== 'UTC')].map(tz => ({
        value: tz,
        label: formatTzLabel(tz)
    }));

    const rows = [
        { name: 'timezone', label: 'Strefa czasowa', options: timezoneOptions },
        { name: 'wind', label: 'Prędkość wiatru', options: ['km/h', 'm/s', 'mph', 'kt', 'bft'] },
        { name: 'temp', label: 'Temperatura', options: ['°C', '°F'] },
        { name: 'pressure', label: 'Ciśnienie', options: ['hPa', 'inHg', 'mmHg'] },
        { name: 'currents', label: 'Prądy morskie', options: ['m/s', 'mph', 'kt', 'km/h'] },
        { name: 'wave', label: 'Wysokość fal', options: ['m', 'ft'] },
        { name: 'rain', label: 'Opady deszczu', options: ['mm', 'inch'] },
        { name: 'snow', label: 'Opady śniegu', options: ['cm', 'mm', 'inch'] },
        { name: 'speed', label: 'Prędkość jednostki', options: ['km/h', 'kt', 'mph', 'm/s'] },
        { name: 'distance', label: 'Odległość', options: ['km', 'NM', 'mi'] },
    ];

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '35rem' }}>

                <div className="modal-header">
                    <h3>Ustawienia jednostek</h3>
                    <button className="modal-close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="modal-body">
                    {rows.map(row => (
                        <div key={row.name} className="settings-row">
                            <span className="settings-label">{row.label}</span>
                            <select
                                className="settings-select"
                                value={units[row.name]}
                                onChange={(e) => updateUnit(row.name, e.target.value)}
                            >
                                {row.options.map(opt => {
                                    const val = typeof opt === 'object' ? opt.value : opt;
                                    const lbl = typeof opt === 'object' ? opt.label : opt;
                                    return <option key={val} value={val}>{lbl}</option>;
                                })}
                            </select>
                        </div>
                    ))}
                </div>

                <div className="modal-footer">
                    <button className="modal-btn btn-submit" onClick={onClose}>
                        Zapisz
                    </button>
                </div>

            </div>
        </div>
    );
};

export default SettingsModal;