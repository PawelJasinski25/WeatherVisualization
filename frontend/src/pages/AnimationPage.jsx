import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Map, { Source, Layer, Marker } from 'react-map-gl/maplibre';
import "maplibre-gl/dist/maplibre-gl.css";

import Navbar from '../components/Navbar';
import AnimationPanel from '../components/AnimationPanel';
import api from '../api/axios';
import FileUploadModal from '../components/FileUploadModal.jsx';
import "../styles/animation.css";
import { useMetricConfig } from '../config/metricConfig';
import { Loader2, PlayCircle, Play, Pause } from 'lucide-react';
import { useUnits } from '../contexts/UnitContext';

const OSM_STYLE = {
    version: 8,
    sources: { osm: { type: "raster", tiles: ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"], tileSize: 256, attribution: "&copy; OpenStreetMap" } },
    layers: [{ id: "osm-tiles", type: "raster", source: "osm" }]
};

const AnimationPage = () => {
    const metricConfig = useMetricConfig();
    const location = useLocation();
    const navigate = useNavigate();
    const tripId = location.state?.tripId || null;

    const [tripData, setTripData] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);

    const [selectedParams, setSelectedParams] = useState(['wind', 'rain', 'clouds']);
    const [isPanelOpen, setIsPanelOpen] = useState(true);
    const [playbackSpeed, setPlaybackSpeed] = useState(1);
    const [isLoading, setIsLoading] = useState(false);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    const mapRef = useRef(null);
    const initialFitDone = useRef(false);
    const lastTripId = useRef(null);
    const { units } = useUnits();

    useEffect(() => {
        initialFitDone.current = false;
        setCurrentIndex(0);
        setIsPlaying(false);
    }, [tripId]);

    // 1. Pobieranie danych
    useEffect(() => {
        if (tripId) {
            if (!initialFitDone.current) {
                setIsLoading(true);
            }
            const tz = units.timezone || 'UTC'; // <--- DODANO STREFĘ
            api.get(`/trips/${tripId}/coordinates?timezone=${encodeURIComponent(tz)}`).then(res => {
                const points = res.data.route || [];
                setTripData(points);
                setIsLoading(false);

                if (points.length > 0 && mapRef.current && !initialFitDone.current) {
                    const lats = points.map(d => d.latitude);
                    const lngs = points.map(d => d.longitude);
                    mapRef.current.getMap().fitBounds(
                        [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]],
                        { padding: 40 }
                    );
                    initialFitDone.current = true;
                }
            }).catch(() => {
                setIsLoading(false);
            });
        }
    }, [tripId, units.timezone]);

    // 2. Silnik Animacji
    useEffect(() => {
        let interval;
        if (isPlaying && tripData.length > 1) {


            const totalTimeMs = tripData[tripData.length - 1].timeMs - tripData[0].timeMs;
            const avgTimeBetweenPoints = totalTimeMs / tripData.length;

            let currentInterval;
            let step = 1;

            if (avgTimeBetweenPoints < 10000) {
                currentInterval = 40 / playbackSpeed;
                step = Math.max(1, Math.floor(tripData.length / 500));
            } else {
                currentInterval = 150 / playbackSpeed;
                step = 1;
            }

            interval = setInterval(() => {
                setCurrentIndex(prev => {
                    const next = prev + step;
                    if (next >= tripData.length - 1) {
                        setIsPlaying(false);
                        return tripData.length - 1;
                    }
                    return next;
                });
            }, currentInterval);
        }
        return () => clearInterval(interval);
    }, [isPlaying, tripData, playbackSpeed]);

    // 3. Budowa Linii Trasy
    const routeGeoJSON = useMemo(() => {
        if (!tripData.length) return null;

        const multiLineCoords = [];
        let currentLine = [];
        let lastPt = null;

        tripData.forEach(pt => {
            const cut = lastPt && pt.segmentId !== undefined && lastPt.segmentId !== undefined && pt.segmentId !== lastPt.segmentId;

            if (cut) {
                if (currentLine.length >= 2) {
                    multiLineCoords.push(currentLine);
                }
                currentLine = [];
            }

            currentLine.push([pt.longitude, pt.latitude]);
            lastPt = pt;
        });

        if (currentLine.length >= 2) {
            multiLineCoords.push(currentLine);
        }

        return {
            type: "Feature",
            geometry: {
                type: "MultiLineString",
                coordinates: multiLineCoords
            }
        };
    }, [tripData]);

    // Formatowanie czasu
    const formatTime = (timeMs) => {
        if (!timeMs) return "--:--";
        const date = new Date(timeMs);
        return date.toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute:'2-digit',timeZone: units.timezone || 'UTC' });
    };

    const currentPoint = tripData[currentIndex];

    return (
        <div className="anim-wrapper dashboard-wrapper">
            <Navbar
                activeTab="animation"
                currentTripId={tripId}
                onOpenUpload={() => setIsUploadModalOpen(true)}
            />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => {
                    setIsUploadModalOpen(false);
                    navigate('/animation', { state: { tripId: id } });
                    setCurrentIndex(0);
                    setIsPlaying(false);
                }}
            />

            {isLoading ? (
                <div className="dashboard-content">
                    <div className="dashboard-empty" style={{ boxShadow: 'none', background: 'none' }}>
                        <Loader2 size={40} color="var(--theme-anim)" className="anim-spin" />
                        <div style={{ fontSize: '1.2rem', fontWeight: '500', color: '#64748b' }}>
                            Wczytywanie trasy...
                        </div>

                    </div>
                </div>
            ) : tripData.length > 0 ? (
                <div className="anim-content">
                    <div
                        className="anim-main"
                        style={{
                            paddingRight: isPanelOpen
                                ? "calc(min(90vw, var(--panel-width)) + 60px)"
                                : "20px"
                        }}
                    >
                        <div className="anim-map-box">
                            <Map ref={mapRef} initialViewState={{ longitude: 20, latitude: 55, zoom: 3 }} mapStyle={OSM_STYLE} style={{ width: "100%", height: "100%" }}>
                                {routeGeoJSON && (
                                    <Source id="route" type="geojson" data={routeGeoJSON}>
                                        <Layer id="route-line" type="line" paint={{ "line-color": "#2563eb", "line-width": 4 }} />
                                    </Source>
                                )}
                                {currentPoint && (
                                    <Marker longitude={currentPoint.longitude} latitude={currentPoint.latitude} anchor="center">
                                        <div style={{
                                            width: '16px', height: '16px', backgroundColor: '#1e40af',
                                            border: '3px solid white', borderRadius: '50%', boxShadow: '0 0 5px rgba(0,0,0,0.5)',
                                            transition: 'transform 0.1s linear'
                                        }} />
                                    </Marker>
                                )}
                            </Map>
                        </div>

                        <div className="anim-controls">
                            <div className="anim-slider-row">
                                <span className="anim-time">
                                    {currentPoint ? formatTime(currentPoint.timeMs) : "--/--"}
                                </span>
                                <input
                                    type="range" min="0" max={tripData.length > 0 ? tripData.length - 1 : 100}
                                    value={currentIndex}
                                    onChange={(e) => { setCurrentIndex(Number(e.target.value)); setIsPlaying(false); }}
                                    className="anim-slider" disabled={tripData.length === 0}
                                />
                            </div>

                            <div className="anim-bottom-row">
                                <div className="anim-params">
                                    {selectedParams.map(metricId => {
                                        const config = metricConfig[metricId];
                                        if (!config || !currentPoint) return null;
                                        const rawVal = config.getValue(currentPoint);
                                        const displayVal = (rawVal === null || rawVal === undefined) ? '--' : (config.formatValue ? config.formatValue(rawVal) : Math.round(rawVal));

                                        return (
                                            <div key={metricId} className="anim-param-box">
                                                <div className="anim-param-content">
                                                    <span className="anim-param-label">{config.label}</span>
                                                    <div className="anim-param-val">
                                                        <span className="anim-val-num">{displayVal}</span>
                                                        <span className="anim-val-unit">{config.unit}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>

                                <div className="anim-actions">
                                    <div className="speed-box">
                                        {[0.5, 1, 2, 5, 10].map(speed => (
                                            <button
                                                key={speed}
                                                onClick={() => setPlaybackSpeed(speed)}
                                                className={`speed-btn ${playbackSpeed === speed ? 'active' : 'inactive'}`}
                                            >
                                                {speed}x
                                            </button>
                                        ))}
                                    </div>

                                    <button
                                        onClick={() => {
                                            if (currentIndex >= tripData.length - 1) setCurrentIndex(0);
                                            setIsPlaying(!isPlaying);
                                        }}
                                        disabled={tripData.length === 0}
                                        className={`play-btn ${isPlaying ? 'playing' : 'paused'}`}
                                    >
                                        {isPlaying ? <Pause size={18} fill="currentColor" /> : <Play size={18} fill="currentColor" />}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <AnimationPanel
                        selectedParams={selectedParams}
                        setSelectedParams={setSelectedParams}
                        isOpen={isPanelOpen}
                        setIsOpen={setIsPanelOpen}
                    />
                </div>
            ) : (

                <div className="dashboard-content" style={{ overflow: 'hidden' }}>
                    <div className="dashboard-empty">
                        <div className="dashboard-empty-icon">
                            <PlayCircle size={100} color="var(--theme-anim)" />
                        </div>
                        <h2 className="dashboard-empty-title">Brak wybranej trasy</h2>
                        <p className="dashboard-empty-text">
                            Przejdź do zakładki <b>„Moje trasy"</b> lub wgraj nowy plik GPX,<br/>
                            aby odtworzyć animację warunków pogodowych.
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AnimationPage;