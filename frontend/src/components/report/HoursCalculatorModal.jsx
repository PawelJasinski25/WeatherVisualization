import React, { useEffect, useState } from 'react';
import { Plus, Trash2, X } from 'lucide-react';
import '../../styles/modal.css';
import { useUnits } from '../../contexts/UnitContext';

const HoursCalculatorModal = ({ isOpen, onClose, initialLogs, onSave, cruiseDates, dailySummaries }) => {
    const [logs, setLogs] = useState([]);
    const [showStopsAndGaps, setShowStopsAndGaps] = useState(false);
    const { units } = useUnits();
    const tz = units.timezone || 'UTC';

    const parseDate = (dateStr) => {
        if (!dateStr) return null;
        const parts = dateStr.split('.');
        if (parts.length === 3) {
            return new Date(parts[2], parts[1] - 1, parts[0]);
        }
        return null;
    };

    const calculateHours = (startStr, endStr) => {
        if (!startStr || !endStr) return 0;

        if (startStr === '00:00' && endStr === '00:00') {
            return 24;
        }

        const [h1, m1] = startStr.split(':').map(Number);
        const [h2, m2] = endStr.split(':').map(Number);

        let startMins = h1 * 60 + m1;
        let endMins = h2 * 60 + m2;

        if (endStr === '00:00') {
            endMins = 24 * 60;
        }

        if (endMins < startMins)
            return 0;

        return (endMins - startMins) / 60;
    };


    const sortAndFormatLogs = (logsArray) => {
        const sorted = [...logsArray].sort((a, b) => {
            const dateA = parseDate(a.date);
            const dateB = parseDate(b.date);

            if (dateA && dateB && dateA.getTime() !== dateB.getTime()) {
                return dateA.getTime() - dateB.getTime();
            }

            if (!a.startTime && b.startTime) return -1;
            if (a.startTime && !b.startTime) return 1;
            if (a.startTime && b.startTime && a.startTime !== b.startTime) {
                return a.startTime.localeCompare(b.startTime);
            }

            if (!a.endTime && b.endTime) return -1;
            if (a.endTime && !b.endTime) return 1;
            if (a.endTime && b.endTime) {
                return a.endTime.localeCompare(b.endTime);
            }

            return 0;
        });

        return sorted.map((log, idx, arr) => ({
            ...log,
            isBase: idx === 0 || arr[idx - 1].date !== log.date
        }));
    };


    useEffect(() => {
        if (!isOpen) return;

        let loadedLogs = [];

        if (initialLogs && initialLogs.length > 0) {
            loadedLogs = [...initialLogs];
        } else if (dailySummaries && dailySummaries.length > 0) {
            dailySummaries.forEach(day => {
                let dateString = day.date;
                if (day.date && day.date.includes('-')) {
                    const [y, m, d] = day.date.split('-');
                    dateString = `${d}.${m}.${y}`;
                }
                const events = day.timelineEvents || [];

                if (events.length > 0) {
                    events.forEach((event) => {
                        const formatter = new Intl.DateTimeFormat('pl-PL', {
                            hour: '2-digit', minute: '2-digit', timeZone: tz
                        });

                        const sTime = formatter.format(new Date(event.start));
                        const eTime = formatter.format(new Date(event.end));

                        const total = calculateHours(sTime, eTime);

                        loadedLogs.push({
                            id: Date.now() + Math.random(),
                            date: dateString,
                            type: event.type || 'RUCH',
                            startTime: sTime,
                            endTime: eTime,
                            total: total > 0 ? (Math.round(total * 10) / 10).toFixed(1).replace('.', ',') : '',
                            sails: '', engine: '', tidal: '',
                            isBase: false
                        });
                    });
                } else {
                    loadedLogs.push({ id: Date.now() + Math.random(),
                        date: dateString, type: 'RUCH',
                        startTime: '',
                        endTime: '',
                        total: '',
                        sails: '',
                        engine: '',
                        tidal: '',
                        isBase: false
                    });
                }
            });
        }

        const start = parseDate(cruiseDates.start);
        const end = parseDate(cruiseDates.end);

        if (start && end && start <= end) {
            let current = new Date(start);
            while (current <= end) {
                const dateStr = current.toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });

                if (!loadedLogs.some(l => l.date === dateStr)) {
                    loadedLogs.push({ id: Date.now() + Math.random(), date: dateStr, type: 'RUCH', startTime: '', endTime: '', total: '', sails: '', engine: '', tidal: '', isBase: false });
                }
                current.setDate(current.getDate() + 1);
            }
        }

        const finalLogs = loadedLogs.length > 0
            ? loadedLogs
            : [{ id: Date.now(), date: 'Brak daty', type: 'RUCH', startTime: '', endTime: '', total: '', sails: '', engine: '', tidal: '', isBase: true }];

        setLogs(sortAndFormatLogs(finalLogs));

    }, [isOpen, initialLogs, cruiseDates, dailySummaries]);

    if (!isOpen) return null;


    const handleLogChange = (id, field, value) => {
        const newLogs = [...logs];
        const index = newLogs.findIndex(l => l.id === id);
        if (index === -1) return;

        const log = newLogs[index];

        if (field === 'type') {
            log.type = value;
            if (value !== 'RUCH') {
                log.sails = '';
                log.engine = '';
                log.tidal = '';
            }
            setLogs(newLogs);
            return;
        }

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
            setLogs(sortAndFormatLogs(newLogs));
            return;
        }

        const sanitizedValue = value.replace(/[^\d.,]/g, '');
        const totalVal = parseFloat(String(log.total).replace(',', '.')) || 0;
        const floatValue = parseFloat(String(sanitizedValue).replace(',', '.')) || 0;

        if (field === 'sails') {
            if (floatValue > totalVal) {
                log.sails = totalVal.toFixed(1).replace('.', ',');
                log.engine = '0,0';
            } else {
                log.sails = sanitizedValue.replace(/\./g, ',');
                log.engine = (totalVal - floatValue).toFixed(1).replace('.', ',');
            }
        }

        if (field === 'engine') {
            if (floatValue > totalVal) {
                log.engine = totalVal.toFixed(1).replace('.', ',');
                log.sails = '0,0';
            } else {
                log.engine = sanitizedValue.replace(/\./g, ',');
                log.sails = (totalVal - floatValue).toFixed(1).replace('.', ',');
            }
        }

        if (field === 'tidal') {
            if (floatValue > totalVal) {
                log.tidal = totalVal.toFixed(1).replace('.', ',');
            } else {
                log.tidal = sanitizedValue.replace(/\./g, ',');
            }
        }

        setLogs(newLogs);
    };

    const addStage = (id, dateStr) => {
        const newLogs = [...logs, {
            id: Date.now() + Math.random(),
            date: dateStr,
            type: 'RUCH',
            startTime: '', endTime: '', total: '', sails: '', engine: '', tidal: '',
            isBase: false
        }];
        setLogs(sortAndFormatLogs(newLogs));
    };

    const removeRow = (id) => {
        const filteredLogs = logs.filter(l => l.id !== id);
        setLogs(sortAndFormatLogs(filteredLogs));
    };

    const handleSave = () => {
        let sumTotal = 0, sumSails = 0, sumEngine = 0, sumTidal = 0, sumStopped = 0, sumGap = 0;

        logs.forEach(l => {
            const t = parseFloat(String(l.total).replace(',', '.')) || 0;

            if (l.type === 'POSTÓJ') {
                sumStopped += t;
            } else if (l.type === 'BRAK DANYCH') {
                sumGap += t;
            } else {
                sumTotal += t;
                sumSails += parseFloat(String(l.sails).replace(',', '.')) || 0;
                sumEngine += parseFloat(String(l.engine).replace(',', '.')) || 0;
                sumTidal += parseFloat(String(l.tidal).replace(',', '.')) || 0;
            }
        });

        onSave({
            total: sumTotal > 0 ? sumTotal.toFixed(1).replace('.', ',') : '',
            sails: sumSails > 0 ? sumSails.toFixed(1).replace('.', ',') : '',
            engine: sumEngine > 0 ? sumEngine.toFixed(1).replace('.', ',') : '',
            tidal: sumTidal > 0 ? sumTidal.toFixed(1).replace('.', ',') : '',
            stopped: sumStopped > 0 ? sumStopped.toFixed(1).replace('.', ',') : '',
            gap: sumGap > 0 ? sumGap.toFixed(1).replace('.', ',') : '',
            dailyLogs: logs
        });
        onClose();
    };

    const handleOverlayClick = (e) => {
        if (e.target.classList.contains('calc-modal-overlay')) {
            onClose();
        }
    };

    const visibleLogs = logs.filter(log => {
        if (showStopsAndGaps) return true;
        return log.type === 'RUCH';
    });

    return (
        <div className="modal-overlay" onClick={handleOverlayClick}>
            <div className="calc-modal-content">
                <div className="modal-header">
                    <h3>Kalkulator godzin żeglugi</h3>
                    <div className="modal-header-actions">
                        <label className="empty-days-label">
                            <input
                                type="checkbox"
                                className="empty-days-checkbox"
                                checked={showStopsAndGaps}
                                onChange={(e) => setShowStopsAndGaps(e.target.checked)}
                            />
                            Pokaż postoje i brak danych
                        </label>

                        <button onClick={onClose} className="modal-close-btn">
                            <X size={20} />
                        </button>
                    </div>
                </div>



                <div className="calc-table-wrapper">
                    <table className="calc-table">
                        <thead>
                        <tr>
                            <th style={{width: '14%'}}>data</th>
                            <th style={{width: '18%'}}>typ</th>
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
                        {visibleLogs.map((log, index) => {
                            const showDate = index === 0 || visibleLogs[index - 1].date !== log.date;

                            return (
                                <tr key={log.id}>
                                    <td>
                                        {showDate ? (
                                            <div style={{display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'bold', color: '#334155'}}>
                                            {log.date}
                                            <button
                                                className="calc-add-stage-btn"
                                                title="Dodaj kolejny etap w tym dniu"
                                                onClick={() => addStage(log.id, log.date)}
                                            >
                                                <Plus size={14}/>
                                            </button>
                                        </div>
                                    ) : null}
                                </td>
                                <td>
                                    <select
                                        className="calc-input calc-select"
                                        value={log.type || 'RUCH'}
                                        onChange={(e) => handleLogChange(log.id, 'type', e.target.value)}
                                    >
                                        <option value="RUCH">Żegluga</option>
                                        <option value="POSTÓJ">Postój</option>
                                        <option value="BRAK DANYCH">Brak danych</option>
                                    </select>
                                </td>
                                <td>
                                    <input
                                        type="time"
                                        className="calc-input time-input"
                                        value={log.startTime}
                                        onChange={(e) => handleLogChange(log.id, 'startTime', e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                    />
                                </td>
                                <td>
                                    <input
                                        type="time"
                                        className="calc-input time-input"
                                        value={log.endTime}
                                        onChange={(e) => handleLogChange(log.id, 'endTime', e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                    />
                                </td>
                                <td>
                                    <input className="calc-input center" readOnly value={log.total} style={{backgroundColor: '#f8fafc', fontWeight: 'bold'}} />
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder={log.type === 'RUCH' ? "0,0" : "-"} value={log.sails} onChange={(e) => handleLogChange(log.id, 'sails', e.target.value)} disabled={log.type !== 'RUCH'}
                                           style={{ opacity: log.type !== 'RUCH' ? 0.3 : 1 }}/>
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder={log.type === 'RUCH' ? "0,0" : "-"} value={log.engine} onChange={(e) => handleLogChange(log.id, 'engine', e.target.value)} disabled={log.type !== 'RUCH'}
                                           style={{ opacity: log.type !== 'RUCH' ? 0.3 : 1 }} />
                                </td>
                                <td>
                                    <input className="calc-input center" placeholder={log.type === 'RUCH' ? "0,0" : "-"} value={log.tidal} onChange={(e) => handleLogChange(log.id, 'tidal', e.target.value)} disabled={log.type !== 'RUCH'}
                                           style={{ opacity: log.type !== 'RUCH' ? 0.3 : 1 }} />
                                </td>
                                <td>
                                    <button className="calc-del-btn" title="Usuń etap" onClick={() => removeRow(log.id)}>
                                        <Trash2 size={18}/>
                                    </button>
                                </td>
                                </tr>
                            );
                        })}
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