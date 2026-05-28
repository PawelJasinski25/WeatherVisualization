import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import Navbar from '../components/Navbar.jsx';
import FileUploadModal from '../components/FileUploadModal.jsx';
import TripMergeModal from '../components/TripMergeModal.jsx';

import "../styles/trips.css";

const TripsPage = () => {
    const [trips, setTrips] = useState([]);
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState("");
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const navigate = useNavigate();
    const [isMergeModalOpen, setIsMergeModalOpen] = useState(false);
    const [filterStartDate, setFilterStartDate] = useState('');
    const [filterEndDate, setFilterEndDate] = useState('');

    useEffect(() => {
        fetchTrips();
    }, []);

    const fetchTrips = async () => {
        try {
            const response = await api.get('/trips');
            setTrips(response.data);
        } catch (error) {
            console.error("Błąd podczas pobierania tras:", error);
        }
    };

    const filteredAndSortedTrips = useMemo(() => {
        if (!trips) return [];

        const filtered = trips.filter(trip => {
            if (!trip.startTime) return true;

            const tripStart = new Date(trip.startTime).getTime();

            if (filterStartDate) {
                const filterStart = new Date(filterStartDate).getTime();
                if (tripStart < filterStart) return false;
            }

            if (filterEndDate) {
                const filterEnd = new Date(filterEndDate);
                filterEnd.setHours(23, 59, 59, 999);
                if (tripStart > filterEnd.getTime()) return false;
            }

            return true;
        });

        // Sortujemy malejąco
        return filtered.sort((a, b) => {
            const timeA = a.startTime ? new Date(a.startTime).getTime() : 0;
            const timeB = b.startTime ? new Date(b.startTime).getTime() : 0;
            return timeB - timeA;
        });
    }, [trips, filterStartDate, filterEndDate]);

    const handleDelete = async (id, e) => {
        e.stopPropagation();
        if (window.confirm("Czy na pewno chcesz usunąć tę trasę? Tej operacji nie można cofnąć.")) {
            try {
                await api.delete(`/trips/${id}`);
                setTrips(trips.filter(t => t.id !== id));
            } catch (error) {
                console.error("Błąd usuwania trasy:", error);
            }
        }
    };

    const startEditing = (trip, e) => {
        e.stopPropagation();
        setEditingId(trip.id);
        setEditName(trip.name);
    };

    const handleSaveEdit = async (id, e) => {
        e.stopPropagation();
        try {
            const response = await api.put(`/trips/${id}`, { name: editName });
            setTrips(trips.map(t => t.id === id ? { ...t, name: response.data.name } : t));
            setEditingId(null);
        } catch (error) {
            console.error("Błąd podczas zmiany nazwy:", error);
        }
    };

    const handleRowClick = (id) => {
        navigate('/dashboard', { state: { tripId: id } });
    };

    const formatTripDates = (start, end) => {
        if (!start && !end) return '';
        const sDate = start ? new Date(start).toLocaleDateString('pl-PL') : '';
        const eDate = end ? new Date(end).toLocaleDateString('pl-PL') : '';

        if (sDate && eDate && sDate !== eDate) {
            return `${sDate} - ${eDate}`;
        }
        return sDate || eDate;
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: '#f9fafb' }}>
            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} activeTab="none" />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={() => {
                    fetchTrips();
                    setIsUploadModalOpen(false);
                }}
            />

            <TripMergeModal
                isOpen={isMergeModalOpen}
                onClose={() => setIsMergeModalOpen(false)}
                availableTrips={trips}
                onMergeSuccess={() => {
                    fetchTrips();
                    setIsMergeModalOpen(false);
                }}
            />

            <div className="trips-container">

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', borderBottom: '2px solid #ddd', paddingBottom: '0.625rem' }}>
                    <h2 className="trips-title" style={{ border: 'none', margin: 0, padding: 0 }}>Moje Trasy</h2>
                    <button
                        onClick={() => setIsMergeModalOpen(true)}
                        className="upload-btn"
                        style={{ backgroundColor: '#eff6ff', color: '#1e40af', borderColor: '#bfdbfe' }}
                        disabled={trips.length < 2}
                    >
                        🔗 Połącz trasy
                    </button>
                </div>

                {trips.length > 0 && (
                    <div className="filter-container" style={{ marginBottom: '1.5rem', backgroundColor: 'white', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                        <label className="form-label filter-label" style={{ marginBottom: '0.5rem', display: 'block' }}>
                            Filtruj trasy po dacie:
                        </label>
                        <div className="filter-inputs-wrapper" style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
                            <div className="filter-input-group" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <span className="filter-date-label">Od:</span>
                                <input
                                    type="date"
                                    value={filterStartDate}
                                    onChange={(e) => setFilterStartDate(e.target.value)}
                                    className="merge-date-picker picker-enabled"
                                />
                            </div>
                            <div className="filter-input-group" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <span className="filter-date-label">Do:</span>
                                <input
                                    type="date"
                                    value={filterEndDate}
                                    onChange={(e) => setFilterEndDate(e.target.value)}
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
                )}

                {trips.length === 0 ? (
                    <div className="trips-empty">
                        Nie masz jeszcze żadnych tras. Kliknij "Nowa Trasa" aby dodać plik GPX.
                    </div>
                ) : filteredAndSortedTrips.length === 0 ? (
                    <div className="trips-empty" style={{ color: '#64748b' }}>
                        Brak tras w wybranym przedziale czasowym.
                    </div>
                ) : (
                    <div className="trips-list">
                        {filteredAndSortedTrips.map(trip => (
                            <div key={trip.id} onClick={() => handleRowClick(trip.id)} className="trip-card">
                                {editingId === trip.id ? (
                                    <div className="edit-container" onClick={e => e.stopPropagation()}>
                                        <input
                                            type="text"
                                            value={editName}
                                            onChange={e => setEditName(e.target.value)}
                                            autoFocus
                                            className="edit-input"
                                        />
                                        <button onClick={(e) => handleSaveEdit(trip.id, e)} className="icon-btn">💾</button>
                                        <button onClick={() => setEditingId(null)} className="icon-btn">❌</button>
                                    </div>
                                ) : (
                                    <>
                                        <div className="trip-info" style={{ display: 'flex', flexDirection: 'column' }}>
                                            <span style={{ fontWeight: 'bold' }}>{trip.name}</span>
                                            {(trip.startTime || trip.endTime) && (
                                                <span style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '2px' }}>
                                                        {formatTripDates(trip.startTime, trip.endTime)}
                                                </span>
                                            )}
                                        </div>

                                        <div className="trip-actions">
                                            <button onClick={(e) => startEditing(trip, e)} className="icon-btn" title="Zmień nazwę">✏️</button>
                                            <button onClick={(e) => handleDelete(trip.id, e)} className="icon-btn" title="Usuń trasę">🗑️</button>
                                        </div>
                                    </>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default TripsPage;