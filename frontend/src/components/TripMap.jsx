import React, { useEffect, useState, useMemo, useRef } from "react";
import Map, { Source, Layer } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import api from "../api/axios.js";

import { metricConfig } from "../config/metricConfig.js";
import { generateSegmentsData, generateSampledPoints } from "../utils/mapProcessors.js";
import MapLegend from "./MapLegend.jsx";
import MetricMarker from "./MetricMarker.jsx";
import "../styles/map-elements.css";

const OSM_STYLE = {
    version: 8,
    sources: { osm: { type: "raster", tiles: ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"], tileSize: 256, attribution: "&copy; OpenStreetMap" } },
    layers: [{ id: "osm-tiles", type: "raster", source: "osm" }]
};

const TripMap = ({ tripId, selectedPrimary = [], selectedSecondary = [], isPanelOpen = true }) => {
    const [tripData, setTripData] = useState([]);
    const [currentZoom, setCurrentZoom] = useState(6);
    const mapRef = useRef(null);

    const activeMetrics = selectedPrimary.filter(Boolean);

    useEffect(() => {
        if (tripId) {
            api.get(`/trips/${tripId}/coordinates`).then(res => {
                setTripData(res.data);
                if (res.data && res.data.length > 0 && mapRef.current) {
                    const lats = res.data.map(d => d.latitude);
                    const lngs = res.data.map(d => d.longitude);
                    mapRef.current.getMap().fitBounds(
                        [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]],
                        { padding: 40 }
                    );
                }
            });
        }
    }, [tripId]);

    const segmentsData = useMemo(() => generateSegmentsData(tripData, activeMetrics), [tripData, activeMetrics]);
    const sampledPoints = useMemo(() => generateSampledPoints(tripData, selectedSecondary, currentZoom), [tripData, selectedSecondary, currentZoom]);


    return (
        <div className="trip-map-wrapper">
            <Map
                ref={mapRef}
                initialViewState={{ longitude: 20, latitude: 55, zoom: 6 }}
                mapStyle={OSM_STYLE}
                style={{ width: "100%", height: "100%" }}
                onMove={(e) => setCurrentZoom(e.viewState.zoom)}
            >
                {/* Renderowanie linii  */}
                {segmentsData.map(seg => {
                    const GRADIENT_WIDTH = 8;
                    const CONTOUR_THICKNESS = 2;

                    return (
                        <Source key={seg.id} id={seg.id} type="geojson" data={seg.geojson} lineMetrics>
                            {/* Domyślna czarna warstwa */}
                            {activeMetrics.length === 0 && (
                                <Layer key={`${seg.id}-default`} id={`${seg.id}-default`} type="line" paint={{ "line-color": "#000000", "line-width": 8 }} layout={{ "line-join": "round", "line-cap": "round" }} />
                            )}

                            {/* Kontury */}
                            {activeMetrics.map((metricId, index) => {
                                if (!metricConfig[metricId]) return null;
                                const offset = activeMetrics.length === 2 ? (index === 0 ? -((GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2)) : ((GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2))) : 0;
                                return (
                                    <Layer
                                        key={`${seg.id}-${metricId}-contour`}
                                        id={`${seg.id}-${metricId}-contour`}
                                        type="line"
                                        paint={{ "line-gradient": seg.contourGradients[metricId], "line-width": GRADIENT_WIDTH + (CONTOUR_THICKNESS * 2), "line-offset": offset }}
                                        layout={{ "line-join": "round", "line-cap": "round" }}
                                    />
                                );
                            })}

                            {/* Gradienty */}
                            {activeMetrics.map((metricId, index) => {
                                if (!metricConfig[metricId]) return null;
                                const offset = activeMetrics.length === 2 ? (index === 0 ? -((GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2)) : ((GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2))) : 0;
                                return (
                                    <Layer
                                        key={`${seg.id}-${metricId}-gradient`}
                                        id={`${seg.id}-${metricId}-gradient`}
                                        type="line"
                                        paint={{ "line-width": GRADIENT_WIDTH, "line-offset": offset, "line-gradient": seg.gradients[metricId] }}
                                        layout={{ "line-join": "round", "line-cap": "round" }}
                                    />
                                );
                            })}

                            {activeMetrics.length === 2 && seg.separatorStops && (
                                <Layer key={`${seg.id}-separator`} id={`${seg.id}-separator`} type="line" paint={{ "line-gradient": seg.separatorStops, "line-width": 1.5, "line-offset": 0 }} layout={{ "line-join": "round", "line-cap": "round" }} />
                            )}
                        </Source>
                    );
                })}

                {/* Renderowanie etykiet/strzałek dla punktów */}
                {sampledPoints.map((pt, i) =>
                    selectedSecondary.map((metricId, index) => (
                        <MetricMarker key={`sec-${i}-${metricId}`} pt={pt} metricId={metricId} index={index} />
                    ))
                )}
            </Map>

            <div
                className="map-legend-container"
                style={{ right: isPanelOpen ? "calc(min(90vw, 28rem) + 1.25rem)" : "1.25rem" }}
            >
                {activeMetrics.map((metricId, index) => (
                    <MapLegend key={`${metricId}-${index}`} metricId={metricId} />
                ))}
            </div>
        </div>
    );
};

export default TripMap;