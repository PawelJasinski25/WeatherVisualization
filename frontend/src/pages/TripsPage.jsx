import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import Navbar from '../components/Navbar.jsx';
import FileUploadModal from '../components/FileUploadModal.jsx';
import TripMergeModal from '../components/TripMergeModal.jsx';
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.jsx';
import { Pencil, Trash2, Check, Link,  X, Download } from 'lucide-react';
import { useUnits } from '../contexts/UnitContext.jsx';

import "../styles/trips.css";
import ErrorModal from "../components/ErrorModal.jsx";

const TripsPage = () => {
    const [trips, setTrips] = useState([]);
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState("");
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [tripToDelete, setTripToDelete] = useState(null);
    const navigate = useNavigate();
    const [isMergeModalOpen, setIsMergeModalOpen] = useState(false);
    const [filterStartDate, setFilterStartDate] = useState('');
    const [filterEndDate, setFilterEndDate] = useState('');


    const [errorModalOpen, setErrorModalOpen] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    const { units } = useUnits();
    const tz = units?.timezone || 'UTC';

    useEffect(() => {
        fetchTrips();
    }, []);

    const fetchTrips = async () => {
        try {
            const response = await api.get('/trips');
            setTrips(response.data);
        } catch (error) {
            console.error("Błąd podczas pobierania tras:", error);
            showError("Wystąpił błąd podczas pobierania listy tras");
        }
    };

    const showError = (msg) => {
        setErrorMessage(msg);
        setErrorModalOpen(true);
    };

    const filteredAndSortedTrips = useMemo(() => {
        if (!trips) return [];

        const filtered = trips.filter(trip => {
            if (!trip.startTime) return true;

            const tripStart = new Date(trip.startTime).getTime();
            const tripDateStr = tripStart.toLocaleString('sv-SE', { timeZone: tz }).slice(0, 10);

            if (filterStartDate && tripDateStr < filterStartDate) return false;
            if (filterEndDate && tripDateStr > filterEndDate) return false;

            return true;

        });

        // Sortujemy malejąco
        return filtered.sort((a, b) => {
            const timeA = a.startTime ? new Date(a.startTime).getTime() : 0;
            const timeB = b.startTime ? new Date(b.startTime).getTime() : 0;
            return timeB - timeA;
        });
    }, [trips, filterStartDate, filterEndDate, tz]);

    const openDeleteModal = (trip, e) => {
        e.stopPropagation();
        setTripToDelete(trip);
    };

    // Właściwa funkcja usuwania (wywoływana przez modal)
    const confirmDelete = async () => {
        if (!tripToDelete) return;
        try {
            await api.delete(`/trips/${tripToDelete.id}`);
            setTrips(trips.filter(t => t.id !== tripToDelete.id));
        } catch (error) {
            console.error("Błąd usuwania trasy:", error);
            showError("Wystąpił błąd podczas usuwania trasy.");
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
            setTrips(trips.map(t => t.id === id ? { ...t, name: editName } : t));
            setEditingId(null);
        } catch (error) {
            console.error("Błąd podczas zmiany nazwy:", error);
            showError("Nie udało się zmienić nazwy trasy.")
        }
    };

    const handleRowClick = (id) => {
        navigate('/dashboard', { state: { tripId: id } });
    };

    const formatTripDates = (start, end) => {
        if (!start && !end) return '';

        const options = { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: tz };
        const formatter = new Intl.DateTimeFormat('pl-PL', options);

        const sDate = start ? formatter.format(new Date(start)) : '';
        const eDate = end ? formatter.format(new Date(end)) : '';

        if (sDate && eDate && sDate !== eDate) {
            return `${sDate} - ${eDate}`;
        }
        return sDate || eDate;
    };

    const handleExportGpx = async (trip, e) => {
        e.stopPropagation();
        try {
            const response = await api.get(`/trips/${trip.id}/export/gpx`, {
                responseType: 'blob'
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;

            const safeName = trip.name.replace(/\.gpx$/i, '').trim();

            link.setAttribute('download', `${safeName}.gpx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Błąd podczas pobierania pliku GPX:", error);
            showError("Nie udało się pobrać pliku GPX.");
        }
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

            <ConfirmDeleteModal
                isOpen={!!tripToDelete}
                onClose={() => setTripToDelete(null)}
                onConfirm={confirmDelete}
                tripName={tripToDelete?.name}
            />

            <ErrorModal
                isOpen={errorModalOpen}
                onClose={() => setErrorModalOpen(false)}
                errorMessage={errorMessage}
            />

            <div className="trips-container">

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', borderBottom: '2px solid #ddd', paddingBottom: '0.625rem' }}>
                    <h2 className="trips-title" style={{ border: 'none', margin: 0, padding: 0 }}>Moje trasy</h2>
                    <button
                        onClick={() => setIsMergeModalOpen(true)}
                        className="merge-btn"
                        disabled={trips.length < 1}
                    >
                        <Link size={18} />
                        Przytnij / Połącz trasy
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
                                    <>
                                        <div className="trip-info" onClick={e => e.stopPropagation()} style={{ paddingRight: '1rem' }}>
                                            <input
                                                type="text"
                                                value={editName}
                                                onChange={e => setEditName(e.target.value)}
                                                autoFocus
                                                className="edit-input"
                                            />
                                        </div>
                                        <div className="trip-actions" onClick={e => e.stopPropagation()}>
                                            <button onClick={(e) => handleSaveEdit(trip.id, e)} className="icon-btn save-btn">
                                                <Check className="trip-icon" />
                                            </button>
                                            <button onClick={() => setEditingId(null)} className="icon-btn cancel-btn">
                                                <X className="trip-icon" />
                                            </button>
                                        </div>
                                    </>
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

                                        <div className="trip-actions" onClick={e => e.stopPropagation()}>
                                            <button
                                                onClick={(e) => startEditing(trip, e)}
                                                className="icon-btn edit-btn"
                                                title="Zmień nazwę"
                                            >
                                                <Pencil className="trip-icon" />
                                            </button>
                                            <button
                                                onClick={(e) => handleExportGpx(trip, e)}
                                                className="icon-btn"
                                                title="Pobierz GPX"
                                            >
                                                <Download className="trip-icon" />
                                            </button>
                                            <button
                                                onClick={(e) => openDeleteModal(trip, e)}
                                                className="icon-btn delete-btn"
                                                title="Usuń trasę"
                                            >
                                                <Trash2 className="trip-icon" />
                                            </button>
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