import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import Navbar from '../components/Navbar.jsx';
import FileUploadModal from '../components/FileUploadModal.jsx';

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
        e.stopPropagation(); // Zapobiega kliknięciu w "przejdź do mapy"
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

    const saveEdit = async (id, e) => {
        e.stopPropagation();
        if (!editName.trim()) return;

        try {
            await api.put(`/trips/${id}/name`, editName, {
                headers: { 'Content-Type': 'text/plain' }
            });
            setTrips(trips.map(t => t.id === id ? { ...t, name: editName } : t));
            setEditingId(null);
        } catch (error) {
            console.error("Błąd edycji nazwy:", error);
        }
    };

    const goToMap = (id) => {
        // Przechodzimy do dashboardu i przekazujemy ID trasy w "state"
        navigate('/dashboard', { state: { tripId: id } });
    };

    return (
        <div style={{ display: "flex", flexDirection: "column", height: "100vh", backgroundColor: "#f0f2f5" }}>
            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => {
                    // Po udanym wgraniu przerzucamy od razu na mapę z nowym ID!
                    navigate('/dashboard', { state: { tripId: id } });
                }}
            />

            {/* DODANE: Ten div zajmuje resztę ekranu i dodaje pasek przewijania (scroll) */}
            <div style={{ flex: 1, overflowY: "auto", paddingBottom: "40px" }}>
                <div style={styles.container}>
                    <h2 style={styles.title}>🗺️ Twoje wgrane trasy</h2>

                    {trips.length === 0 ? (
                        <div style={styles.emptyState}>Nie masz jeszcze żadnych wgranych tras.</div>
                    ) : (
                        <div style={styles.list}>
                            {trips.map(trip => (
                                <div key={trip.id} style={styles.card} onClick={() => goToMap(trip.id)}>

                                    {editingId === trip.id ? (
                                        <div style={styles.editContainer}>
                                            <input
                                                value={editName}
                                                onChange={(e) => setEditName(e.target.value)}
                                                style={styles.input}
                                                autoFocus
                                                onClick={(e) => e.stopPropagation()}
                                            />
                                            <button style={styles.saveBtn} onClick={(e) => saveEdit(trip.id, e)}>💾 Zapisz</button>
                                            <button style={styles.cancelBtn} onClick={(e) => { e.stopPropagation(); setEditingId(null); }}>❌</button>
                                        </div>
                                    ) : (
                                        <div style={styles.tripInfo}>
                                            <span style={styles.tripName}>{trip.name}</span>
                                        </div>
                                    )}

                                    <div style={styles.actions}>
                                        {editingId !== trip.id && (
                                            <button style={styles.iconBtn} onClick={(e) => startEditing(trip, e)} title="Edytuj nazwę">
                                                ✏️
                                            </button>
                                        )}
                                        <button style={styles.iconBtn} onClick={(e) => handleDelete(trip.id, e)} title="Usuń trasę">
                                            🗑️
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

const styles = {
    container: { maxWidth: '800px', margin: '40px auto', padding: '0 20px', width: '100%' },
    title: { color: '#333', marginBottom: '20px', borderBottom: '2px solid #ddd', paddingBottom: '10px' },
    emptyState: { textAlign: 'center', color: '#777', padding: '40px', backgroundColor: '#fff', borderRadius: '8px' },
    list: { display: 'flex', flexDirection: 'column', gap: '15px' },
    card: {
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        backgroundColor: '#fff', padding: '20px', borderRadius: '10px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.05)', cursor: 'pointer', transition: 'transform 0.2s, box-shadow 0.2s'
    },
    tripInfo: { flex: 1, fontSize: '1.1rem', fontWeight: '500', color: '#444' },
    tripName: { cursor: 'pointer' },
    actions: { display: 'flex', gap: '10px' },
    iconBtn: { background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', padding: '5px' },
    editContainer: { display: 'flex', gap: '10px', flex: 1, alignItems: 'center' },
    input: { padding: '8px', borderRadius: '6px', border: '1px solid #ccc', fontSize: '1rem', flex: 1 },
    saveBtn: { padding: '8px 12px', backgroundColor: '#28a745', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' },
    cancelBtn: { background: 'none', border: 'none', cursor: 'pointer' }
};

export default TripsPage;