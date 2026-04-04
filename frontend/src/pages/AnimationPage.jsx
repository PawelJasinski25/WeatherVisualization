import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import Map, { Source, Layer, Marker } from 'react-map-gl/maplibre';
import "maplibre-gl/dist/maplibre-gl.css";

import Navbar from '../components/Navbar';
import AnimationPanel from '../components/AnimationPanel';
import api from '../api/axios';
import { metricConfig } from '../config/metricConfig';
import "../styles/animation.css";

const OSM_STYLE = {
    version: 8,
    sources: { osm: { type: "raster", tiles: ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"], tileSize: 256, attribution: "&copy; OpenStreetMap" } },
    layers: [{ id: "osm-tiles", type: "raster", source: "osm" }]
};

const AnimationPage = () => {
    const location = useLocation();
    const tripId = location.state?.tripId || null;

    const [tripData, setTripData] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);

    const [selectedParams, setSelectedParams] = useState(['wind', 'rain', 'clouds']);
    const [isPanelOpen, setIsPanelOpen] = useState(true);
    const [playbackSpeed, setPlaybackSpeed] = useState(1);
    const [isLoading, setIsLoading] = useState(false);

    const mapRef = useRef(null);

    // 1. Pobieranie danych
    useEffect(() => {
        if (tripId) {
            setIsLoading(true);
            api.get(`/trips/${tripId}/coordinates`).then(res => {
                setTripData(res.data);
                setIsLoading(false);
                if (res.data.length > 0 && mapRef.current) {
                    const lats = res.data.map(d => d.latitude);
                    const lngs = res.data.map(d => d.longitude);
                    mapRef.current.getMap().fitBounds(
                        [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]],
                        { padding: 40 }
                    );
                }
            }).catch(() => {
                setIsLoading(false);
            });
        }
    }, [tripId]);

    // 2. Silnik Animacji
    useEffect(() => {
        let interval;
        if (isPlaying && tripData.length > 1) {

            // 1. WYKRYWANIE GĘSTOŚCI TRASY
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
        return date.toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute:'2-digit' });
    };

    const currentPoint = tripData[currentIndex];

    return (
        <div className="anim-wrapper">
            <Navbar activeTab="animation" currentTripId={tripId} />

            <div className="anim-content">
                <div className="anim-main" style={{ paddingRight: isPanelOpen ? 'calc(min(90vw, 400px) + 90px)' : '20px' }}>

                    {/* MAPA */}
                    <div className="anim-map-box">
                        {isLoading ? (
                            <div className="anim-loading">⏳ Wczytywanie trasy...</div>
                        ) : tripData.length > 0 ? (
                            <Map ref={mapRef} initialViewState={{ longitude: 20, latitude: 55, zoom: 5 }} mapStyle={OSM_STYLE} style={{ width: "100%", height: "100%" }}>
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
                        ) : (
                            <div className="anim-empty">Brak danych lub nie wybrano trasy.</div>
                        )}
                    </div>

                    {/* DOLNY PASEK KONTROLNY */}
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
                                    {isPlaying ? '⏸' : '▶'}
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
        </div>
    );
};

export default AnimationPage;