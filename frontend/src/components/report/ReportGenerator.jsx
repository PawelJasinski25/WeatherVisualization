import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import CruiseCardForm from './CruiseCardForm';
import ReportPreview from './ReportPreview';
import '../../styles/panel.css';
import '../../styles/report-elements.css';
import '../../styles/report-generator.css';

const ReportGenerator = ({ tripId }) => {
    const [includeCruiseCard, setIncludeCruiseCard] = useState(true);

    const [isGeneratingPdf, setIsGeneratingPdf] = useState(false);
    const [isGeneratingCsv, setIsGeneratingCsv] = useState(false);
    const [isFetchingData, setIsFetchingData] = useState(true);

    const [formData, setFormData] = useState({
        tripName: '',
        captain: { name: '', patent: '', phone: '', email: '' },
        yacht: { regNumber: '', name: '', length: '', homePort: '', enginePower: '' },
        cruise: {
            logbookNumber: '',
            startDate: '', endDate: '',
            embarkPort: '', embarkDate: '', embarkTidal: '',
            disembarkPort: '', disembarkDate: '', disembarkTidal: '',
            visitedPorts: '',
            tidalPortsCount: '', daysCount: ''
        },
        hours: { total: '', sails: '', engine: '', tidal: '', stopped: '' },
        distance: { nauticalMiles: '' },
        crew: Array(16).fill({ name: '', patent: '', function: '' })
    });

    useEffect(() => {
        const fetchTripData = async () => {
            setIsFetchingData(true);
            try {
                const response = await api.get('/trips');
                const currentTrip = response.data.find(t => t.id === parseInt(tripId) || t.id === tripId);

                if (currentTrip) {
                    const formatDate = (isoString) => {
                        if (!isoString) return '';
                        const d = new Date(isoString);
                        if (isNaN(d.getTime())) return '';
                        const day = String(d.getDate()).padStart(2, '0');
                        const month = String(d.getMonth() + 1).padStart(2, '0');
                        const year = d.getFullYear();
                        return `${day}.${month}.${year}`;
                    };

                    const sDate = formatDate(currentTrip.startTime || currentTrip.startDate);
                    const eDate = formatDate(currentTrip.endTime || currentTrip.endDate);
                    const tName = currentTrip.name || '';

                    setFormData(prev => ({
                        ...prev,
                        tripName: tName,
                        cruise: {
                            ...prev.cruise,
                            startDate: sDate,
                            endDate: eDate,
                            embarkDate: sDate,
                            disembarkDate: eDate
                        }
                    }));
                }
            } catch (error) {
                console.error("Nie udało się pobrać danych trasy:", error);
            } finally {
                setIsFetchingData(false);
            }
        };

        if (tripId) {
            fetchTripData();
        } else {
            setIsFetchingData(false);
        }
    }, [tripId]);

    const handleNestedChange = (section, field, value) => {
        setFormData(prev => ({ ...prev, [section]: { ...prev[section], [field]: value } }));
    };

    const handleFieldChange = (field, value) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleCrewChange = (index, field, value) => {
        const newCrew = [...formData.crew];
        newCrew[index] = { ...newCrew[index], [field]: value };
        setFormData(prev => ({ ...prev, crew: newCrew }));
    };

    const handleGeneratePdf = async () => {
        setIsGeneratingPdf(true);
        try {
            const modulesToExport = ['summary', 'timeline', 'weather', 'maps', 'meteogram'];
            if (includeCruiseCard) modulesToExport.unshift('cruiseCard');

            const response = await api.post(`/trips/${tripId}/download-pdf`, {
                modules: modulesToExport,
                reportData: formData
            }, {
                responseType: 'blob',
                params: { t: new Date().getTime() }
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `raport_trasy_${tripId}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            alert("Błąd podczas pobierania PDF: " + err.message);
        } finally {
            setIsGeneratingPdf(false);
        }
    };

    const handleDownloadCsv = async () => {
        setIsGeneratingCsv(true);
        try {
            const response = await api.get(`/trips/${tripId}/report/csv`, {
                responseType: 'blob',
                params: { t: new Date().getTime() }
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `raport_pogodowy_${tripId}.csv`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            alert("Błąd podczas pobierania CSV: " + err.message);
        } finally {
            setIsGeneratingCsv(false);
        }
    };

    return (
        <div className="report-page-wrapper">
            <div className="report-main">
                <div className="report-scroll-area">

                    {isFetchingData ? (
                        <div className="report-loading-container">
                            <span className="spinner spinner-icon"></span>
                            <h3>Wczytywanie makiety...</h3>
                            <p className="report-loading-text">Pobieranie dat i struktury trasy z serwera</p>
                        </div>
                    ) : (
                        <div className={`report-form-wrapper ${isGeneratingPdf ? 'generating' : ''}`}>
                            {includeCruiseCard && (
                                <CruiseCardForm
                                    formData={formData}
                                    handleFieldChange={handleFieldChange}
                                    handleNestedChange={handleNestedChange}
                                    handleCrewChange={handleCrewChange}
                                />
                            )}
                            <ReportPreview />
                        </div>
                    )}
                </div>
            </div>

            {/* SIDEBAR */}
            <div className="report-sidebar">
                <div className="report-sidebar-content">
                    <h3 className="panel-title" style={{ fontSize: '1.25rem' }}>Generowanie Raportu</h3>
                    <p className="panel-subtitle">
                        <b>Opcjonalnie dodaj i wypełnij Kartę Rejsu.</b>
                    </p>

                    <div className="sidebar-actions-group">
                        <button
                            className={`panel-btn toggle-card-btn ${includeCruiseCard ? 'active' : ''}`}
                            disabled={isFetchingData}
                            onClick={() => setIncludeCruiseCard(!includeCruiseCard)}
                        >
                            <span style={{ fontSize: '1.4rem' }}>📝</span>
                            <span className="panel-label">Karta Rejsu</span>
                            <span className={`toggle-card-status ${includeCruiseCard ? 'active' : ''}`}>
                                {includeCruiseCard ? 'WŁ' : 'WYŁ'}
                            </span>
                        </button>
                    </div>
                </div>

                <div className="sidebar-actions-group">
                    <button
                        onClick={handleGeneratePdf}
                        disabled={isGeneratingPdf || isGeneratingCsv || isFetchingData || !tripId}
                        className="download-action-btn pdf"
                    >
                        {isGeneratingPdf ? (
                            <>
                                <span className="spinner spinner-icon"></span>
                                TWORZENIE PDF...
                            </>
                        ) : 'POBIERZ RAPORT (PDF)'}
                    </button>

                    <button
                        onClick={handleDownloadCsv}
                        disabled={isGeneratingPdf || isGeneratingCsv || isFetchingData || !tripId}
                        className="download-action-btn csv"
                    >
                        {isGeneratingCsv ? (
                            <>
                                <span className="spinner spinner-icon"></span>
                                TWORZENIE CSV...
                            </>
                        ) : 'POBIERZ DANE (CSV)'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ReportGenerator;