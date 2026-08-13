import React, { useState, useEffect, useMemo, useCallback } from 'react';
import api from '../api/axios.js';
import '../styles/modal.css';
import {AlertCircle, Loader2} from "lucide-react";
import { useUnits } from '../contexts/UnitContext.jsx';

const TripMergeModal = ({ isOpen, onClose, onMergeSuccess, availableTrips, getLocalYMD }) => {
    const [selectedIds, setSelectedIds] = useState([]);
    const [timeWindows, setTimeWindows] = useState({});
    const [newTripName, setNewTripName] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const [filterStartDate, setFilterStartDate] = useState('');
    const [filterEndDate, setFilterEndDate] = useState('');
    const [error, setError] = useState('');
    const { units } = useUnits();
    const tz = units?.timezone || 'UTC';

    const formatToInputDateTime = useCallback((dateStr) => {
        if (!dateStr) return '';
        try {
            const d = new Date(dateStr);
            if (!isNaN(d.getTime())) {
                const formatter = new Intl.DateTimeFormat('en-GB', {
                    timeZone: tz,
                    year: 'numeric', month: '2-digit', day: '2-digit',
                    hour: '2-digit', minute: '2-digit',
                    hour12: false
                });
                const parts = formatter.formatToParts(d);
                const p = {};
                parts.forEach(({ type, value }) => { p[type] = value; });

                let hour = p.hour;
                if (hour === '24') hour = '00';

                return `${p.year}-${p.month}-${p.day}T${hour}:${p.minute}`;
            }
        } catch (e) { console.error(e); }
        return '';
    }, [tz]);

    useEffect(() => {
        if (isOpen && availableTrips) {
            setSelectedIds([]);
            setNewTripName('');
            setIsProcessing(false);
            setFilterStartDate('');
            setFilterEndDate('');
            setError('');

            const initialWindows = {};
            availableTrips.forEach(trip => {
                initialWindows[trip.id] = {
                    start: formatToInputDateTime(trip.startTime),
                    end: formatToInputDateTime(trip.endTime),
                    minOriginal: formatToInputDateTime(trip.startTime),
                    maxOriginal: formatToInputDateTime(trip.endTime)
                };
            });
            setTimeWindows(initialWindows);
        }
    }, [isOpen, availableTrips, formatToInputDateTime]);


    const filteredAndSortedTrips = useMemo(() => {
        if (!availableTrips) return [];

        const filtered = availableTrips.filter(trip => {
            if (!trip.startTime) return false;

            const tripDateStr = getLocalYMD(trip.startTime);

            if (filterStartDate && tripDateStr < filterStartDate) return false;
            if (filterEndDate && tripDateStr > filterEndDate) return false;

            return true;
        });

        return filtered.sort((a, b) => {
            const timeA = a.startTime ? new Date(a.startTime).getTime() : 0;
            const timeB = b.startTime ? new Date(b.startTime).getTime() : 0;
            return timeA - timeB;
        });
    }, [availableTrips, filterStartDate, filterEndDate, getLocalYMD]);

    if (!isOpen) return null;

    const handleCheckboxChange = (id) => {
        setError('');
        setSelectedIds(prev => prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]);
    };

    const handleTimeChange = (id, field, value) => {
        setError('');
        setTimeWindows(prev => ({
            ...prev,
            [id]: { ...prev[id], [field]: value }
        }));
    };

    const handleTimeBlur = (id, field) => {
        setTimeWindows(prev => {
            const current = prev[id];
            let newValue = current[field];

            if (field === 'start') {
                if (newValue < current.minOriginal) newValue = current.minOriginal;
                if (newValue > current.end) newValue = current.end;
            } else if (field === 'end') {
                if (newValue > current.maxOriginal) newValue = current.maxOriginal;
                if (newValue < current.start) newValue = current.start;
            }

            return { ...prev, [id]: { ...current, [field]: newValue } };
        });
    };

    const handleMergeSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (selectedIds.length < 1) {
            setError("Wybierz co najmniej 1 trasę.");
            return;
        }
        if (!newTripName.trim()) {
            setError("Proszę podać nazwę dla nowej trasy.");
            return;
        }

        for (const id of selectedIds) {
            const tw = timeWindows[id];
            if (tw.start < tw.minOriginal || tw.end > tw.maxOriginal) {
                setError("Daty wykraczają poza oryginalne ramy czasowe!");
                return;
            }
        }

        setIsProcessing(true);

        const segments = selectedIds.map(id => {
            const trip = availableTrips.find(t => t.id === id);
            const tw = timeWindows[id];

            const originalStartInputMs = new Date(tw.minOriginal + 'Z').getTime();
            const currentStartInputMs = new Date(tw.start + 'Z').getTime();
            const diffStart = currentStartInputMs - originalStartInputMs;

            const originalEndInputMs = new Date(tw.maxOriginal + 'Z').getTime();
            const currentEndInputMs = new Date(tw.end + 'Z').getTime();
            const diffEnd = currentEndInputMs - originalEndInputMs;

            const originalUtcStart = new Date(trip.startTime).getTime();
            const originalUtcEnd = new Date(trip.endTime).getTime();

            return {
                tripId: id,
                trimStartTime: new Date(originalUtcStart + diffStart).toISOString(),
                trimEndTime: new Date(originalUtcEnd + diffEnd).toISOString()
            };
        });

        try {
            await api.post('/trips/merge', { newTripName: newTripName.trim(), segments });
            onMergeSuccess();
            onClose();
        } catch (error) {
            console.error("Błąd podczas łączenia tras:", error);
            setError("Wystąpił błąd podczas scalania tras.");
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-merge-width">
                <div className="modal-header">
                    <h3>Przytnij lub połącz trasy GPX</h3>
                    <button type="button" onClick={onClose} className="modal-close-btn">&times;</button>
                </div>

                <form onSubmit={handleMergeSubmit}>
                    <div className="modal-body custom-gap">
                        <div className="form-group filter-container">
                            <label className="form-label filter-label">
                                Filtruj dostępne trasy po dacie:
                            </label>
                            <div className="filter-inputs-wrapper">
                                <div className="filter-input-group">
                                    <span className="filter-date-label">Od:</span>
                                    <input
                                        type="date"
                                        value={filterStartDate}
                                        onChange={(e) => setFilterStartDate(e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                        className="merge-date-picker picker-enabled"
                                    />
                                </div>
                                <div className="filter-input-group">
                                    <span className="filter-date-label">Do:</span>
                                    <input
                                        type="date"
                                        value={filterEndDate}
                                        onChange={(e) => setFilterEndDate(e.target.value)}
                                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                                        className="merge-date-picker picker-enabled"
                                    />
                                </div>
                                {(filterStartDate || filterEndDate) && (
                                    <button
                                        type="button"
                                        onClick={() => { setFilterStartDate(''); setFilterEndDate(''); }}
                                        className="filter-clear-btn"
                                    >
                                        Wyczyść
                                    </button>
                                )}
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Wybierz pliki do połączenia i dostosuj ich zakresy czasowe:</label>
                            <div className="merge-trips-list">
                                {filteredAndSortedTrips.length === 0 ? (
                                    <div style={{ padding: '1rem', textAlign: 'center', color: '#64748b', fontSize: '0.9rem' }}>
                                        Brak tras w podanym zakresie czasowym.
                                    </div>
                                ) : (
                                    filteredAndSortedTrips.map(trip => {
                                        const isChecked = selectedIds.includes(trip.id);
                                        const tw = timeWindows[trip.id];
                                        return (
                                            <div key={trip.id} className={`merge-trip-row ${isChecked ? 'active-row' : ''}`}>

                                                <div className="merge-trip-info">
                                                    <input
                                                        type="checkbox"
                                                        id={`merge-trip-${trip.id}`}
                                                        checked={isChecked}
                                                        onChange={() => handleCheckboxChange(trip.id)}
                                                        className="merge-checkbox"
                                                    />
                                                    <label htmlFor={`merge-trip-${trip.id}`} className="merge-trip-name">
                                                        {trip.name}
                                                    </label>
                                                </div>

                                            <div className="merge-trip-dates">
                                                <div className="date-input-group">
                                                    <span className="date-label">OD:</span>
                                                    <input
                                                        type="datetime-local"
                                                        value={tw?.start || ''}
                                                        min={tw?.minOriginal}
                                                        max={tw?.end}
                                                        onChange={(e) => handleTimeChange(trip.id, 'start', e.target.value)}
                                                        onBlur={() => handleTimeBlur(trip.id, 'start')}
                                                        disabled={!isChecked}
                                                        className={`merge-date-picker ${isChecked ? 'picker-enabled' : ''}`}
                                                    />
                                                </div>
                                                <div className="date-input-group">
                                                    <span className="date-label">DO:</span>
                                                    <input
                                                        type="datetime-local"
                                                        value={tw?.end || ''}
                                                        min={tw?.start}
                                                        max={tw?.maxOriginal}
                                                        onChange={(e) => handleTimeChange(trip.id, 'end', e.target.value)}
                                                        onBlur={() => handleTimeBlur(trip.id, 'end')}
                                                        disabled={!isChecked}
                                                        className={`merge-date-picker ${isChecked ? 'picker-enabled' : ''}`}
                                                    />
                                                </div>
                                            </div>

                                            </div>
                                        );
                                    })
                                )}
                            </div>
                        </div>
                        <div className="form-group">
                            <label className="form-label">Nazwa nowej, połączonej trasy:</label>
                            <input
                                type="text"
                                className="interactive-input merge-name-input"
                                placeholder="Nazwa"
                                value={newTripName}
                                onChange={e => {
                                    setNewTripName(e.target.value);
                                    setError('');
                                }}
                                required
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="modal-error-message" style={{ margin: '0.5rem 1.25rem 0' }}>
                            <AlertCircle size={18} />
                            <span>{error}</span>
                        </div>
                    )}

                    <div className="modal-footer">
                        <button type="button" onClick={onClose} disabled={isProcessing} className="modal-btn btn-cancel">
                            Anuluj
                        </button>
                        <button type="submit" disabled={isProcessing || selectedIds.length === 0 || !newTripName} className="modal-btn btn-submit">
                            {isProcessing ? (
                                <span className="btn-loading-content">
                                    <Loader2 size={16} className="anim-spin" /> Przetwarzanie...
                                </span>
                            ) : (
                                selectedIds.length === 1 ? "Zapisz przyciętą trasę" : "Połącz wybrane trasy"
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default TripMergeModal;