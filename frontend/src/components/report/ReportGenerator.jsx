import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import CruiseCardForm from './CruiseCardForm';
import CruiseOpinionForm from './CruiseOpinionForm';
import ReportPreview from './ReportPreview';
import '../../styles/panel.css';
import '../../styles/report-elements.css';
import '../../styles/report-generator.css';
import {useUnits} from "../../contexts/UnitContext.jsx";
import { FileSignature, ClipboardList } from 'lucide-react';
import ErrorModal from "../ErrorModal.jsx";
import { Loader2} from 'lucide-react';

const ReportGenerator = ({ tripId }) => {
    const [includeCruiseCard, setIncludeCruiseCard] = useState(true);
    const [includeOpinion, setIncludeOpinion] = useState(false);

    const [isGeneratingPdf, setIsGeneratingPdf] = useState(false);
    const [isGeneratingCsv, setIsGeneratingCsv] = useState(false);
    const [isFetchingData, setIsFetchingData] = useState(true);
    const { units } = useUnits();
    const [error, setError] = useState(null);

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
            tidalPortsCount: '', daysCount: '', dailySummaries: []
        },
        hours: { total: '', sails: '', engine: '', tidal: '', stopped: '' },
        distance: { nauticalMiles: '' },
        crew: Array(16).fill({ name: '', patent: '', function: '' }),

        opinions: []
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

                    let reportData = null;
                    try {
                        const reportResponse = await api.get(`/trips/${tripId}/report-data`);
                        reportData = reportResponse.data;
                    } catch (err) {
                        console.error("Nie udało się pobrać szczegółowych danych raportu", err);
                    }

                    let visitedPorts = '';
                    let daysCount = '';
                    let totalDistanceNM = '';
                    let stoppedHours = '';
                    let movingHours = '';
                    let totalHoursRaw = '';

                    if (reportData) {
                        daysCount = reportData.dailySummaries ? reportData.dailySummaries.length.toString() : '';

                        if (reportData.overallSpeed && reportData.overallSpeed.distanceKm) {
                            const distKm = reportData.overallSpeed.distanceKm;
                            const distNm = distKm * 0.539957;
                            totalDistanceNM = distNm.toFixed(1).replace('.', ',');
                        }

                        const formatTime = (seconds) => {
                            if (!seconds) return '00:00';
                            const hours = seconds / 3600;
                            return (Math.round(hours * 10) / 10).toFixed(1).replace('.', ',');
                        };

                        if (reportData.overallMovement) {
                            stoppedHours = formatTime(reportData.overallMovement.stoppedSeconds);
                        }

                        const portsSet = new Set();

                        if (reportData.startPort) {
                            portsSet.add(reportData.startPort);
                        }

                        if (reportData.dailySummaries) {
                            reportData.dailySummaries.forEach(day => {
                                if (day.timelineEvents) {
                                    day.timelineEvents.forEach(event => {
                                        if (event.placeName && event.type === 'POSTÓJ') {
                                            portsSet.add(event.placeName);
                                        }
                                    });
                                }
                            });
                        }

                        if (reportData.endPort) {
                            portsSet.add(reportData.endPort);
                        }

                        visitedPorts = Array.from(portsSet).join(', ');
                    }

                    const endPortStr = reportData.endPort || '';
                    const endDateStr = eDate || '';
                    const combinedLocationDate = [endPortStr, endDateStr].filter(Boolean).join(', ');

                    setFormData(prev => ({
                        ...prev,
                        tripName: tName,
                        cruise: {
                            ...prev.cruise,
                            startDate: sDate,
                            endDate: eDate,
                            embarkDate: sDate,
                            disembarkDate: eDate,
                            visitedPorts: visitedPorts,
                            daysCount: daysCount,
                            embarkPort: reportData.startPort || '',
                            disembarkPort: reportData.endPort || '',
                            dailySummaries: reportData?.dailySummaries || []
                        },
                        distance: {
                            ...prev.distance,
                            nauticalMiles: totalDistanceNM
                        },
                        hours: {
                            ...prev.hours,
                            stopped: stoppedHours
                        },
                    }));
                }
            } catch (error) {
                console.error("Nie udało się pobrać danych trasy:");
                setError("Nie udało się pobrać danych trasy z serwera.");
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
        let processedValue = value;

        const numericFields = ['total', 'sails', 'engine', 'tidal', 'nauticalMiles', 'length'];
        if (numericFields.includes(field) && typeof value === 'string') {
            processedValue = processedValue.replace(/\./g, ',');
        }

        setFormData(prev => ({ ...prev, [section]: { ...prev[section], [field]: processedValue } }));
    };

    const handleFieldChange = (field, value) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleCrewChange = (index, field, value) => {
        setFormData(prev => {
            const newCrew = [...prev.crew];
            newCrew[index] = { ...newCrew[index], [field]: value };

            let newOpinions = [...prev.opinions];
            const opField = { name: 'participantName', patent: 'participantPatent', function: 'participantFunction' }[field];
            const opinionIndex = newOpinions.findIndex(op => op.crewIndex === index);

            const locDate = [prev.cruise.disembarkPort, prev.cruise.disembarkDate].filter(Boolean).join(', ');

            const hasData = newCrew[index].name.trim() !== '' ||
                newCrew[index].patent.trim() !== '' ||
                newCrew[index].function.trim() !== '';

            if (opinionIndex !== -1) {
                if (!hasData) {
                    newOpinions.splice(opinionIndex, 1);
                } else {
                    newOpinions[opinionIndex] = { ...newOpinions[opinionIndex], [opField]: value };
                }
            } else if (hasData && includeOpinion) {
                newOpinions = newOpinions.filter(op => op.crewIndex !== -1);

                newOpinions.push({
                    id: Date.now() + Math.random(),
                    crewIndex: index,
                    participantName: newCrew[index].name,
                    participantPatent: newCrew[index].patent,
                    participantFunction: newCrew[index].function,
                    participantPhone: '', participantEmail: '',
                    general: '', duties: '', seasickness: '', endurance: '', remarks: '',
                    title: 'OPINIA Z REJSU',
                    locationDate: locDate
                });
            }

            if (includeOpinion && newOpinions.length === 0) {
                newOpinions.push({
                    id: Date.now(), crewIndex: -1,
                    participantName: '', participantPatent: '', participantFunction: '',
                    participantPhone: '', participantEmail: '',
                    general: '', duties: '', seasickness: '', endurance: '', remarks: '',
                    title: 'OPINIA Z REJSU', locationDate: locDate
                });
            }

            return { ...prev, crew: newCrew, opinions: newOpinions };
        });
    };

    const toggleOpinionModule = () => {
        if (!includeOpinion) {
            const endPortStr = formData.cruise.disembarkPort || '';
            const endDateStr = formData.cruise.disembarkDate || '';
            const combinedLocationDate = [endPortStr, endDateStr].filter(Boolean).join(', ');

            setFormData(prev => {
                let newOpinions = [...prev.opinions];

                const activeCrewWithIndices = prev.crew
                    .map((c, i) => ({ ...c, originalIndex: i }))
                    .filter(c => c.name.trim() !== '' || c.patent.trim() !== '' || c.function.trim() !== '');

                if (activeCrewWithIndices.length > 0) {
                    activeCrewWithIndices.forEach(c => {
                        const exists = newOpinions.find(op => op.crewIndex === c.originalIndex);
                        if (!exists) {
                            newOpinions.push({
                                id: Date.now() + Math.random(),
                                crewIndex: c.originalIndex,
                                participantName: c.name,
                                participantPatent: c.patent,
                                participantFunction: c.function,
                                participantPhone: '',
                                participantEmail: '',
                                general: '', duties: '', seasickness: '', endurance: '', remarks: '',
                                title: 'OPINIA Z REJSU',
                                locationDate: combinedLocationDate
                            });
                        }
                    });
                } else if (newOpinions.length === 0) {
                    newOpinions.push({
                        id: Date.now(),
                        crewIndex: -1,
                        participantName: '', participantPatent: '', participantFunction: '',
                        participantPhone: '', participantEmail: '',
                        general: '', duties: '', seasickness: '', endurance: '', remarks: '',
                        title: 'OPINIA Z REJSU',
                        locationDate: combinedLocationDate
                    });
                }
                return { ...prev, opinions: newOpinions };
            });
        }
        setIncludeOpinion(!includeOpinion);
    };

    const handleOpinionChange = (id, field, value) => {
        setFormData(prev => ({
            ...prev,
            opinions: prev.opinions.map(op => op.id === id ? { ...op, [field]: value } : op)
        }));
    };

    const handleRemoveOpinion = (id) => {
        setFormData(prev => ({
            ...prev,
            opinions: prev.opinions.filter(op => op.id !== id)
        }));
    };

    const downloadFileFromResponse = (response, defaultFilename) => {
        const contentDisposition = response.headers['content-disposition'];
        let fileName = defaultFilename;

        if (contentDisposition) {
            const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
            if (utf8Match && utf8Match[1]) {
                try {
                    fileName = decodeURIComponent(utf8Match[1]);
                } catch (e) {
                    console.error("Błąd dekodowania UTF-8 nazwy pliku:", e);
                }
            } else {
                const standardMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
                if (standardMatch && standardMatch[1]) {
                    fileName = standardMatch[1];
                }
            }
        }

        const url = window.URL.createObjectURL(response.data);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    };

    const handleGeneratePdf = async () => {
        setIsGeneratingPdf(true);
        setError(null);
        try {
            const modulesToExport = ['summary', 'timeline', 'weather', 'maps', 'meteogram'];
            if (includeCruiseCard) modulesToExport.unshift('cruiseCard');
            if (includeOpinion) modulesToExport.unshift('opinion');

            const response = await api.post(`/trips/${tripId}/download-pdf`, {
                modules: modulesToExport,
                reportData: {
                    ...formData,
                    preferences: units,
                    modules: modulesToExport
                }
            }, {
                responseType: 'blob',
                params: { t: new Date().getTime() }
            });

            downloadFileFromResponse(response, `raport_trasy_${tripId}.pdf`);

        } catch (err) {
            setError("Wystąpił błąd podczas generowania pliku PDF: ");
        } finally {
            setIsGeneratingPdf(false);
        }
    };

    const handleDownloadCsv = async () => {
        setIsGeneratingCsv(true);
        setError(null);
        try {
            const response = await api.get(`/trips/${tripId}/report/csv`, {
                responseType: 'blob',
                params: { t: new Date().getTime() }
            });

            const blob = new Blob([response.data], { type: 'application/zip' });
            downloadFileFromResponse(response, `raport_${tripId}.zip`);
        } catch (err) {
            setError("Wystąpił błąd podczas generowania pliku CSV: ");
        } finally {
            setIsGeneratingCsv(false);
        }
    };

    return (
        <div className="report-page-wrapper">
            <div className="report-main">
                <div className="report-scroll-area">

                    {isFetchingData ? (
                        <div className="dashboard-content">
                            <div className="dashboard-empty" style={{ boxShadow: 'none', background: 'none' }}>
                                <Loader2 size={40} color="var(--theme-report)" className="anim-spin" />
                                <div style={{ fontSize: '1.2rem', fontWeight: '500', color: '#64748b' }}>
                                    Wczytywanie raportu...
                                </div>

                            </div>
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

                            {includeOpinion && formData.opinions.map(op => (
                                <CruiseOpinionForm
                                    key={op.id}
                                    formData={formData}
                                    opinion={op}
                                    handleOpinionChange={(field, value) => handleOpinionChange(op.id, field, value)}
                                    handleRemove={() => handleRemoveOpinion(op.id)}
                                    handleNestedChange={handleNestedChange}
                                />
                            ))}

                            <ReportPreview />
                        </div>
                    )}
                </div>
            </div>

            {/* SIDEBAR */}
            <div className="report-sidebar">
                <div className="report-sidebar-content">
                    <h3 className="panel-title" style={{ fontSize: '1.25rem' }}>Generowanie raportu</h3>
                    <p className="panel-subtitle">
                        <b>Opcjonalnie dodaj kartę i opinię z rejsu.</b>
                    </p>

                    <div className="sidebar-actions-group">
                        <button
                            className={`panel-btn toggle-card-btn ${includeCruiseCard ? 'active' : ''}`}
                            disabled={isFetchingData}
                            onClick={() => setIncludeCruiseCard(!includeCruiseCard)}
                        >
                            <FileSignature size={24} color={includeCruiseCard ? "var(--theme-report)" : "#64748b"} />
                            <span className="panel-label">Karta rejsu</span>
                            <span className={`toggle-card-status ${includeCruiseCard ? 'active' : ''}`}>
                                {includeCruiseCard ? 'WŁ' : 'WYŁ'}
                            </span>
                        </button>

                        <button
                            className={`panel-btn toggle-card-btn ${includeOpinion ? 'active' : ''}`}
                            disabled={isFetchingData}
                            onClick={toggleOpinionModule}
                        >
                            <ClipboardList size={24} color={includeOpinion ? "var(--theme-report)" : "#64748b"} />
                            <span className="panel-label">Opinia z rejsu</span>
                            <span className={`toggle-card-status ${includeOpinion ? 'active' : ''}`}>
                                {includeOpinion ? 'WŁ' : 'WYŁ'}
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
                                TWORZENIE ZIP...
                            </>
                        ) : 'POBIERZ DANE (CSV + PNG)'}
                    </button>
                </div>
            </div>

            <ErrorModal
                isOpen={!!error}
                onClose={() => setError(null)}
                errorMessage={error}
            />
        </div>


    );
};

export default ReportGenerator;