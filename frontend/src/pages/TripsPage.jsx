import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import Navbar from '../components/Navbar.jsx';
import FileUploadModal from '../components/FileUploadModal.jsx';

import "../styles/trips.css";

const TripsPage = () => {
    const [trips, setTrips] = useState([]);
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState("");
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const navigate = useNavigate();

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

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: '#f9fafb' }}>
            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={() => {
                    fetchTrips();
                    setIsUploadModalOpen(false);
                }}
            />

            <div className="trips-container">
                <h2 className="trips-title">Moje Trasy</h2>

                {trips.length === 0 ? (
                    <div className="trips-empty">
                        Nie masz jeszcze żadnych tras. Kliknij "Nowa Trasa" aby dodać plik GPX.
                    </div>
                ) : (
                    <div className="trips-list">
                        {trips.map(trip => (
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
                                        <div className="trip-info">
                                            <span>{trip.name}</span>
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