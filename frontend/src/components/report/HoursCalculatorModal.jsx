import React, { useEffect, useState } from 'react';
import { Plus, Trash2, X } from 'lucide-react';
import '../../styles/modal.css';

const HoursCalculatorModal = ({ isOpen, onClose, initialLogs, onSave, cruiseDates }) => {
    const [logs, setLogs] = useState([]);

    const parseDate = (dateStr) => {
        if (!dateStr) return null;
        const parts = dateStr.split('.');
        if (parts.length === 3) {
            return new Date(parts[2], parts[1] - 1, parts[0]);
        }
        return null;
    };

    useEffect(() => {
        if (isOpen) {
            const start = parseDate(cruiseDates.start);
            const end = parseDate(cruiseDates.end);

            if (initialLogs && initialLogs.length > 0) {
                const processedLogs = initialLogs.map((log, i) => {
                    if (log.isBase === undefined) {
                        const isFirst = initialLogs.findIndex(l => l.date === log.date) === i;
                        return { ...log, isBase: isFirst };
                    }
                    return log;
                });
                setLogs(processedLogs);
                return;
            }

            if (start && end && start <= end) {
                const daysArray = [];
                let current = new Date(start);
                let dayNum = 1;

                while (current <= end) {
                    const dateString = current.toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
                    daysArray.push({
                        id: Date.now() + dayNum,
                        date: dateString,
                        startTime: '',
                        endTime: '',
                        total: '',
                        sails: '',
                        engine: '',
                        tidal: '',
                        isBase: true
                    });
                    current.setDate(current.getDate() + 1);
                    dayNum++;
                }
                setLogs(daysArray);
            } else {
                setLogs([{ id: Date.now(), date: 'Brak daty', startTime: '', endTime: '', total: '', sails: '', engine: '', tidal: '' }]);
            }
        }
    }, [isOpen, initialLogs, cruiseDates]);

    if (!isOpen) return null;

    const calculateHours = (startStr, endStr) => {
        if (!startStr || !endStr) return 0;
        const [h1, m1] = startStr.split(':').map(Number);
        const [h2, m2] = endStr.split(':').map(Number);

        let startMins = h1 * 60 + m1;
        let endMins = h2 * 60 + m2;

        if (endMins < startMins) endMins += 24 * 60;

        return (endMins - startMins) / 60;
    };

    const handleLogChange = (index, field, value) => {
        const newLogs = [...logs];
        const log = newLogs[index];

        if (field === 'startTime' || field === 'endTime') {
            log[field] = value;
            const calculatedTotal = calculateHours(log.startTime, log.endTime);
            if (calculatedTotal > 0) {
                log.total = (Math.round(calculatedTotal * 10) / 10).toFixed(1).replace('.', ',');
            } else {
                log.total = '';
            }
            log.sails = '';
            log.engine = '';
            setLogs(newLogs);
            return;
        }

        const totalVal = parseFloat(String(log.total).replace(',', '.')) || 0;
        const floatValue = parseFloat(String(value).replace(',', '.')) || 0;

        if (field === 'sails') {
            if (floatValue > totalVal) {
                log.sails = totalVal.toFixed(1).replace('.', ',');
                log.engine = '0,0';
            } else {
                log.sails = value.replace(/\./g, ',');
                log.engine = (totalVal - floatValue).toFixed(1).replace('.', ',');
            }
        }

        if (field === 'engine') {
            if (floatValue > totalVal) {
                log.engine = totalVal.toFixed(1).replace('.', ',');
                log.sails = '0,0';
            } else {
                log.engine = value.replace(/\./g, ',');
                log.sails = (totalVal - floatValue).toFixed(1).replace('.', ',');
            }
        }

        if (field === 'tidal') {
            log.tidal = value.replace(/\./g, ',');
        }

        setLogs(newLogs);
    };

    const addStage = (index, dateStr) => {
        const newLogs = [...logs];
        newLogs.splice(index + 1, 0, {
            id: Date.now(),
            date: dateStr,
            startTime: '', endTime: '', total: '', sails: '', engine: '', tidal: '',
            isBase: false
        });
        setLogs(newLogs);
    };

    const removeRow = (index) => {
        const newLogs = logs.filter((_, i) => i !== index);
        setLogs(newLogs);
    };

    const handleSave = () => {
        let sumTotal = 0, sumSails = 0, sumEngine = 0, sumTidal = 0;

        logs.forEach(l => {
            sumTotal += parseFloat(String(l.total).replace(',', '.')) || 0;
            sumSails += parseFloat(String(l.sails).replace(',', '.')) || 0;
            sumEngine += parseFloat(String(l.engine).replace(',', '.')) || 0;
            sumTidal += parseFloat(String(l.tidal).replace(',', '.')) || 0;
        });

        onSave({
            total: sumTotal > 0 ? sumTotal.toFixed(1).replace('.', ',') : '',
            sails: sumSails > 0 ? sumSails.toFixed(1).replace('.', ',') : '',
            engine: sumEngine > 0 ? sumEngine.toFixed(1).replace('.', ',') : '',
            tidal: sumTidal > 0 ? sumTidal.toFixed(1).replace('.', ',') : '',
            dailyLogs: logs
        });
        onClose();
    };;

    const handleOverlayClick = (e) => {
        if (e.target.classList.contains('calc-modal-overlay')) {
            onClose();
        }
    };

    return (
        <div className="modal-overlay" onClick={handleOverlayClick}>
            <div className="calc-modal-content">
                <div className="modal-header">
                    <h3>Kalkulator godzin żeglugi</h3>
                    <button onClick={onClose} className="modal-close-btn"><X size={20} /></button>
                </div>

                <div className="calc-table-wrapper">
                    <table className="calc-table">
                        <thead>
                        <tr>
                            <th style={{width: '20%'}}>data</th>
                            <th>od</th>
                            <th>do</th>
                            <th>razem</th>
                            <th>pod żaglami</th>
                            <th>na silniku</th>
                            <th>wody pływowe</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {logs.map((log, idx) => (
                            <tr key={log.id}>
                                <td>
                                    <div style={{display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'bold', color: '#334155'}}>
                                        {log.date}
                                        <button
                                            className="calc-add-stage-btn"
                                            title="Dodaj kolejny etap w tym dniu"
                                            onClick={() => addStage(idx, log.date)}
                                        >
                                            <Plus size={14}/>
                                        </button>
                                    </div>
                                </td>
                                <td>
                                    <input
                                        type="time"
                                        className="calc-input time-input"
                                        value={log.startTime}
                                        onChange={(e) => handleLogChange(idx, 'startTime', e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                    />
                                </td>
                                <td>
                                    <input
                                        type="time"
                                        className="calc-input time-input"
                                        value={log.endTime}
                                        onChange={(e) => handleLogChange(idx, 'endTime', e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                    />
                                </td>
                                <td>
                                    <input className="calc-input center" readOnly value={log.total} style={{backgroundColor: '#f8fafc', fontWeight: 'bold'}} />
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder="0.0" value={log.sails} onChange={(e) => handleLogChange(idx, 'sails', e.target.value)} />
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder="0.0" value={log.engine} onChange={(e) => handleLogChange(idx, 'engine', e.target.value)} />
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder="0.0" value={log.tidal} onChange={(e) => handleLogChange(idx, 'tidal', e.target.value)} />
                                </td>
                                <td>
                                    {!log.isBase ? (
                                        <button className="calc-del-btn" title="Usuń dodatkowy etap" onClick={() => removeRow(idx)}>
                                            <Trash2 size={16}/>
                                        </button>
                                    ) : (
                                        <div style={{width: '28px', height: '28px'}}></div>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <div className="modal-footer">
                    <div style={{flex: 1}}></div>
                    <button className="modal-btn btn-submit" onClick={handleSave}>Zatwierdź i przepisz do karty</button>
                </div>
            </div>
        </div>
    );
};

export default HoursCalculatorModal