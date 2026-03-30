import React, { useEffect, useState, useMemo, useRef } from "react";
import Map, { Source, Layer, Marker } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import api from "../api/axios.js";

const OSM_STYLE = {
    version: 8,
    sources: {
        osm: {
            type: "raster",
            tiles: ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"],
            tileSize: 256,
            attribution: "&copy; OpenStreetMap"
        }
    },
    layers: [{ id: "osm-tiles", type: "raster", source: "osm" }]
};

const tempColors = [[203, [115, 70, 105]], [218, [202, 172, 195]], [233, [162, 70, 145]], [248, [143, 89, 169]], [258, [157, 219, 217]], [265, [106, 191, 181]], [269, [100, 166, 189]], [273.15, [93, 133, 198]], [274, [68, 125, 99]], [283, [128, 147, 24]], [294, [243, 183, 4]], [303, [232, 83, 25]], [320, [71, 14, 0]]];
const humidityColors = [[0, [173, 85, 56]], [30, [173, 110, 56]], [40, [173, 146, 56]], [50, [105, 173, 56]], [60, [56, 173, 121]], [70, [56, 174, 173]], [75, [56, 160, 173]], [80, [56, 157, 173]], [83, [56, 148, 173]], [87, [56, 135, 173]], [90, [56, 132, 173]], [93, [56, 123, 173]], [97, [56, 98, 157]], [100, [56, 70, 114]]];
const dewColors = [[203, [115, 70, 105]], [218, [202, 172, 195]], [233, [162, 70, 145]], [248, [143, 89, 169]], [258, [157, 219, 217]], [265, [106, 191, 181]], [269, [100, 166, 189]], [273.15, [93, 133, 198]], [274, [68, 125, 99]], [283, [128, 147, 24]], [294, [243, 183, 4]], [303, [232, 83, 25]], [320, [71, 14, 0]]];
const pressureColors = [[90000, [8, 16, 48]], [95000, [0, 32, 96]], [97600, [0, 52, 146]], [98600, [0, 90, 148]], [99500, [0, 117, 146]], [100200, [26, 140, 147]], [100700, [103, 162, 155]], [101125, [155, 183, 172]], [101325, [182, 182, 182]], [101525, [176, 174, 152]], [101900, [167, 147, 107]], [102400, [163, 116, 67]], [103000, [159, 81, 44]], [103800, [142, 47, 57]], [104600, [111, 24, 64]], [108000, [48, 8, 24]]];
const rainColors = [[0, [97, 97, 97]], [1, [64, 64, 163]], [5, [70, 106, 227]], [10, [41, 187, 236]], [30, [49, 241, 153]], [50, [163, 253, 61]], [80, [237, 208, 59]], [120, [251, 128, 34]], [200, [210, 49, 4]], [320, [122, 4, 3]], [600, [48, 0, 0]], [8000, [24, 0, 0]]];
const snowColors = [[0, [97, 97, 97]], [2, [69, 82, 152]], [10, [65, 165, 167]], [20, [65, 141, 65]], [50, [168, 168, 65]], [80, [170, 126, 63]], [120, [167, 65, 65]], [500, [168, 65, 168]]];
const cloudsColors = [[0, [146, 130, 70]], [10, [132, 119, 70]], [50, [116, 116, 116]], [95, [171, 180, 179]], [98, [198, 201, 201]], [100, [213, 213, 205]]];
const cloudsLowColors = [[0, [156, 142, 87]], [10, [143, 131, 87]], [30, [129, 129, 129]], [90, [137, 159, 182]], [100, [187, 187, 187]]];
const cloudsMidColors = [[0, [156, 142, 87]], [10, [143, 131, 87]], [30, [157, 192, 157]], [90, [145, 171, 145]], [100, [187, 187, 187]]];
const cloudsHighColors = [[0, [156, 142, 87]], [10, [143, 131, 87]], [30, [125, 157, 157]], [90, [141, 169, 169]], [100, [187, 187, 187]]];
const windColors = [[0, [98, 113, 183]], [1, [57, 97, 159]], [3, [74, 148, 169]], [5, [77, 141, 123]], [7, [83, 165, 83]], [9, [53, 159, 53]], [11, [167, 157, 81]], [13, [159, 127, 58]], [15, [161, 108, 92]], [17, [129, 58, 78]], [19, [175, 80, 136]], [21, [117, 74, 147]], [24, [109, 97, 163]], [27, [68, 105, 141]], [29, [92, 144, 152]], [36, [125, 68, 165]], [46, [231, 215, 215]], [51, [219, 212, 135]], [77, [205, 202, 112]], [104, [128, 128, 128]]];
const gustsColors = [[0, [98, 113, 183]], [1, [57, 97, 159]], [3, [74, 148, 169]], [5, [77, 141, 123]], [7, [83, 165, 83]], [9, [53, 159, 53]], [11, [167, 157, 81]], [13, [159, 127, 58]], [15, [161, 108, 92]], [17, [129, 58, 78]], [19, [175, 80, 136]], [21, [117, 74, 147]], [24, [109, 97, 163]], [27, [68, 105, 141]], [29, [92, 144, 152]], [36, [125, 68, 165]], [46, [231, 215, 215]], [51, [219, 212, 135]], [77, [205, 202, 112]], [104, [128, 128, 128]]];
const waveColors = [[0, [159, 185, 191]], [0.5, [48, 157, 185]], [1, [48, 98, 141]], [1.5, [56, 104, 191]], [2, [57, 60, 142]], [2.5, [187, 90, 191]], [3, [154, 48, 151]], [4, [133, 48, 48]], [5, [191, 51, 95]], [7, [191, 103, 87]], [10, [191, 191, 191]], [12, [154, 127, 155]]];
const wavePeriodColors = [[0, [150, 200, 255]], [4, [90, 180, 220]], [7, [100, 220, 150]], [10, [255, 200, 50]], [13, [255, 120, 50]], [16, [200, 50, 80]], [20, [130, 20, 150]]];

const metricConfig = {
    wind: { label: "Wiatr", unit: "km/h", palette: windColors, getValue: pt => pt.windSpeed, formatValue: val => Number(val).toFixed(1), labelCount: 7 },
    gusts: { label: "Porywy", unit: "km/h", palette: gustsColors, getValue: pt => pt.gusts, formatValue: val => Number(val).toFixed(1), labelCount: 7 },
    temp: { label: "Temperatura", unit: "°C", palette: tempColors, getValue: pt => pt.tempC !== null ? pt.tempC + 273.15 : null,
        formatValue: val => (val - 273.15).toFixed(1), labelCount: 7 },
    dew: { label: "Punkt rosy", unit: "°C", palette: dewColors, getValue: pt => pt.dewC !== null ? pt.dewC + 273.15 : null,
        formatValue: val => (val - 273.15).toFixed(1), labelCount: 7 },
    wave_h: { label: "Fale", unit: "m", palette: waveColors, getValue: pt => pt.waveHeight, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    wave_p: { label: "Okres fal", unit: "s", palette: wavePeriodColors, getValue: pt => pt.wavePeriod, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    rain: { label: "Deszcz", unit: "mm", palette: rainColors, getValue: pt => pt.rain, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    snow: { label: "Śnieg", unit: "cm", palette: snowColors, getValue: pt => pt.snow, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    humidity: { label: "Wilgotność", unit: "%", palette: humidityColors, getValue: pt => pt.humidity, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    pressure: { label: "Ciśnienie", unit: "hPa", palette: pressureColors, getValue: pt => pt.pressure !== null ? pt.pressure * 100 : null,
        formatValue: val => (val / 100).toFixed(1), labelCount: 5 },
    clouds: { label: "Zachmurzenie", unit: "%", palette: cloudsColors, getValue: pt => pt.clouds, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_low: { label: "Chmury niskie", unit: "%", palette: cloudsLowColors, getValue: pt => pt.cloudsLow, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_mid: { label: "Chmury średnie", unit: "%", palette: cloudsMidColors, getValue: pt => pt.cloudsMid, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_high: { label: "Chmury wysokie", unit: "%", palette: cloudsHighColors, getValue: pt => pt.cloudsHigh, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
};

// --- FUNKCJE DLA ODWZOROWANIA MERCATORA ---
const getProjectedCoords = (lat, lon) => {
    const toRad = Math.PI / 180;
    const x = lon * toRad;
    const y = Math.log(Math.tan(Math.PI / 4 + (lat * toRad) / 2));
    return { x, y };
};

const unprojectMercator = (x, y) => {
    const toDeg = 180 / Math.PI;
    const lon = x * toDeg;
    const lat = (2 * Math.atan(Math.exp(y)) - Math.PI / 2) * toDeg;
    return { lat, lon };
};

const getProjectedDistance = (lat1, lon1, lat2, lon2) => {
    const p1 = getProjectedCoords(lat1, lon1);
    const p2 = getProjectedCoords(lat2, lon2);
    return Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
};


// Obliczanie odległości w kilometrach fizycznych
const getDistanceFromLatLonInKm = (lat1, lon1, lat2, lon2) => {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
};

// Obliczanie kierunku trasy
const getBearing = (lat1, lon1, lat2, lon2) => {
    const toRad = Math.PI / 180;
    const toDeg = 180 / Math.PI;
    const dLon = (lon2 - lon1) * toRad;
    const y = Math.sin(dLon) * Math.cos(lat2 * toRad);
    const x = Math.cos(lat1 * toRad) * Math.sin(lat2 * toRad) -
        Math.sin(lat1 * toRad) * Math.cos(lat2 * toRad) * Math.cos(dLon);
    let brng = Math.atan2(y, x) * toDeg;
    return (brng + 360) % 360;
};

const interpolateColor = (value, palette) => {
    if (value === null || value === undefined) return "rgba(0,0,0,0)";

    if (value <= palette[0][0]) return `rgb(${palette[0][1].join(",")})`;
    if (value >= palette[palette.length - 1][0]) return `rgb(${palette[palette.length - 1][1].join(",")})`;

    for (let i = 0; i < palette.length - 1; i++) {
        if (value >= palette[i][0] && value <= palette[i + 1][0]) {
            const f = (value - palette[i][0]) / (palette[i + 1][0] - palette[i][0]);
            const r = Math.round(palette[i][1][0] + f * (palette[i + 1][1][0] - palette[i][1][0]));
            const g = Math.round(palette[i][1][1] + f * (palette[i + 1][1][1] - palette[i][1][1]));
            const b = Math.round(palette[i][1][2] + f * (palette[i + 1][1][2] - palette[i][1][2]));
            return `rgb(${r}, ${g}, ${b})`;
        }
    }
    return "rgba(0,0,0,0)";
};

const MapLegend = ({ metricId }) => {
    if (!metricId || !metricConfig[metricId]) return null;

    const config = metricConfig[metricId];
    const palette = config.palette;

    const gradientString = palette.map(stop => `rgb(${stop[1].join(",")})`).join(", ");

    const limit = config.labelCount || 6;
    const numLabels = Math.min(limit, palette.length);
    const labelIndices = [];
    for (let i = 0; i < numLabels; i++) {
        labelIndices.push(Math.floor(i * (palette.length - 1) / (numLabels - 1)));
    }

    return (
        <div style={{
            backgroundColor: "rgba(255, 255, 255, 0.95)",
            padding: "8px 12px",
            borderRadius: "8px",
            boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
            display: "flex",
            flexDirection: "column",
            gap: "4px",
            backdropFilter: "blur(5px)"
        }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontWeight: "700", fontSize: "0.8rem", color: "#333" }}>{config.label}</span>
                <span style={{ color: "#777", fontSize: "0.7rem", fontWeight: "600" }}>{config.unit}</span>
            </div>

            <div style={{
                position: "relative",
                height: "22px",
                borderRadius: "4px",
                background: `linear-gradient(to right, ${gradientString})`,
                width: "100%",
                boxShadow: "inset 0 1px 3px rgba(0,0,0,0.2)",
                display: "flex",
                alignItems: "center"
            }}>
                {labelIndices.map((index, i) => {
                    const val = palette[index][0];
                    const formattedVal = Math.round(Number(config.formatValue ? config.formatValue(val) : val));
                    const leftPercent = (index / (palette.length - 1)) * 100;

                    let transform = "translateX(-50%)";
                    if (index === 0) transform = "translateX(4px)";
                    if (index === palette.length - 1) transform = "translateX(calc(-100% - 4px))";

                    return (
                        <span key={i} style={{
                            position: "absolute",
                            left: `${leftPercent}%`,
                            transform: transform,
                            fontSize: "0.70rem",
                            fontWeight: "700",
                            color: "#ffffff",
                            textShadow: "1px 1px 0 #000, -1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 0 0 3px rgba(0,0,0,0.8)",
                            whiteSpace: "nowrap",
                            pointerEvents: "none"
                        }}>
                            {formattedVal}
                        </span>
                    );
                })}
            </div>
        </div>
    );
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
                    const lats = res.data.map(d => d[0]);
                    const lngs = res.data.map(d => d[1]);
                    mapRef.current.getMap().fitBounds(
                        [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]],
                        { padding: 40 }
                    );
                }
            });
        }
    }, [tripId]);

    const segmentsData = useMemo(() => {
        if (!tripData.length) return [];

        const allSegments = [];
        let currentSegment = [];
        let lastPt = null;

        tripData.forEach(d => {
            const lat = d[0];
            const lng = d[1];
            const segmentId = d[2];

            const params = {
                windSpeed: d[4], tempC: d[5], gusts: d[6], dewC: d[7],
                rain: d[8], humidity: d[9], pressure: d[10],
                clouds: d[11], cloudsLow: d[12], cloudsMid: d[13], cloudsHigh: d[14], windDir: d[15], snow: d[16],
                waveHeight: d[17], wavePeriod: d[18], waveDir: d[19]
            };

            let cut = false;
            if (lastPt && segmentId !== undefined && lastPt[2] !== undefined && segmentId !== lastPt[2]) {
                cut = true;
            }

            if (cut) {
                if (currentSegment.length >= 2) allSegments.push(currentSegment);
                currentSegment = [];
            }

            currentSegment.push({ coordinates: [lng, lat], ...params });
            lastPt = d;
        });

        if (currentSegment.length >= 2) {
            allSegments.push(currentSegment);
        }

        return allSegments.map((seg, idx) => {
            const segmentId = `segment-${idx}`;

            // Przygotowujemy pojemniki na nowe obrysy
            const allGradients = {};
            const allContourGradients = {};
            let separatorStops = null;

            let totalDist = 0;
            const dists = [0];
            for (let i = 1; i < seg.length; i++) {
                const d = getProjectedDistance(seg[i-1].coordinates[1], seg[i-1].coordinates[0], seg[i].coordinates[1], seg[i].coordinates[0]);
                totalDist += d;
                dists.push(totalDist);
            }

            activeMetrics.forEach((metricKey, index) => {
                const stops = ["interpolate", ["linear"], ["line-progress"]];
                const contourStops = ["interpolate", ["linear"], ["line-progress"]];

                const contourColor = index === 0 ? "rgba(33,59,212,1)" : "rgba(248,118,13,1)";

                let lastProgress = -1;
                let prevIsNull = false;

                seg.forEach((pt, i) => {
                    let progress = totalDist === 0 ? (i / (seg.length - 1)) : (dists[i] / totalDist);

                    if (progress <= lastProgress) progress = lastProgress + 0.00000001;

                    const val = metricConfig[metricKey].getValue(pt);
                    const isNull = val === null || val === undefined;

                    const color = isNull ? "rgba(0,0,0,0)" : interpolateColor(val, metricConfig[metricKey].palette);
                    const cColor = isNull ? "rgba(0,0,0,0)" : contourColor;

                    if (i === 0) {
                        stops.push(progress, color);
                        contourStops.push(progress, cColor);
                    } else {
                        const gap = progress - lastProgress;
                        const epsilon = gap * 0.001;

                        if (!prevIsNull && isNull) {
                            stops.push(lastProgress + epsilon, "rgba(0,0,0,0)");
                            contourStops.push(lastProgress + epsilon, "rgba(0,0,0,0)");
                        } else if (prevIsNull && !isNull) {
                            stops.push(progress - epsilon, "rgba(0,0,0,0)");
                            contourStops.push(progress - epsilon, "rgba(0,0,0,0)");
                        }

                        stops.push(progress, color);
                        contourStops.push(progress, cColor);
                    }

                    lastProgress = progress;
                    prevIsNull = isNull;
                });

                allGradients[metricKey] = stops;
                allContourGradients[metricKey] = contourStops;
            });

            if (activeMetrics.length === 2) {
                separatorStops = ["interpolate", ["linear"], ["line-progress"]];
                let lastProgress = -1;
                let prevIsNull = false;

                seg.forEach((pt, i) => {
                    let progress = totalDist === 0 ? (i / (seg.length - 1)) : (dists[i] / totalDist);
                    if (progress <= lastProgress) progress = lastProgress + 0.00000001;

                    const val0 = metricConfig[activeMetrics[0]].getValue(pt);
                    const val1 = metricConfig[activeMetrics[1]].getValue(pt);
                    const isNull = (val0 === null || val0 === undefined) && (val1 === null || val1 === undefined);
                    const sepColor = isNull ? "rgba(0,0,0,0)" : "rgba(0,0,0,1)";

                    if (i === 0) {
                        separatorStops.push(progress, sepColor);
                    } else {
                        const gap = progress - lastProgress;
                        const epsilon = gap * 0.001;

                        if (!prevIsNull && isNull) separatorStops.push(lastProgress + epsilon, "rgba(0,0,0,0)");
                        else if (prevIsNull && !isNull) separatorStops.push(progress - epsilon, "rgba(0,0,0,0)");

                        separatorStops.push(progress, sepColor);
                    }

                    lastProgress = progress;
                    prevIsNull = isNull;
                });
            }

            return {
                id: segmentId,
                geojson: { type: "Feature", geometry: { type: "LineString", coordinates: seg.map(p => p.coordinates) } },
                gradients: allGradients,
                contourGradients: allContourGradients,
                separatorStops: separatorStops
            };
        });
    }, [tripData, selectedPrimary]);


    const sampledPoints = useMemo(() => {
        if (!tripData.length || selectedSecondary.filter(Boolean).length === 0 || currentZoom < 9.0) return [];

        const allSegments = [];
        let currentSegment = [];
        let lastPt = null;

        tripData.forEach(d => {
            const segmentId = d[2];
            let cut = false;
            if (lastPt && segmentId !== undefined && lastPt[2] !== undefined && segmentId !== lastPt[2]) {
                cut = true;
            }
            if (cut) {
                if (currentSegment.length >= 2) allSegments.push(currentSegment);
                currentSegment = [];
            }
            currentSegment.push(d);
            lastPt = d;
        });

        if (currentSegment.length >= 2) {
            allSegments.push(currentSegment);
        }

        let INTERVAL_KM = 10;

        const points = [];

        allSegments.forEach(segment => {
            let accumulatedDist = 0;
            let lastLat = segment[0][0];
            let lastLng = segment[0][1];

            let initialBearing = 0;
            if (segment.length > 1) {
                initialBearing = getBearing(segment[0][0], segment[0][1], segment[1][0], segment[1][1]);
            }

            points.push({
                lat: segment[0][0],
                lng: segment[0][1],
                windSpeed:  segment[0][4],
                tempC:      segment[0][5],
                gusts:      segment[0][6],
                dewC:       segment[0][7],
                rain:       segment[0][8],
                humidity:   segment[0][9],
                pressure:   segment[0][10],
                clouds:     segment[0][11],
                cloudsLow:  segment[0][12],
                cloudsMid:  segment[0][13],
                cloudsHigh: segment[0][14],
                windDir:    segment[0][15],
                snow:       segment[0][16],
                waveHeight: segment[0][17],
                wavePeriod: segment[0][18],
                waveDir:    segment[0][19],
                routeBearing: initialBearing
            });

            for (let i = 1; i < segment.length; i++) {
                const lat = segment[i][0];
                const lng = segment[i][1];

                const segmentDist = getDistanceFromLatLonInKm(lastLat, lastLng, lat, lng);

                if (segmentDist > INTERVAL_KM) {
                    let distanceCoveredInSegment = INTERVAL_KM - accumulatedDist;

                    while (distanceCoveredInSegment <= segmentDist) {
                        const fraction = distanceCoveredInSegment / segmentDist;

                        const p1 = getProjectedCoords(lastLat, lastLng);
                        const p2 = getProjectedCoords(lat, lng);

                        const interX = p1.x + (p2.x - p1.x) * fraction;
                        const interY = p1.y + (p2.y - p1.y) * fraction;

                        const interpolatedCoords = unprojectMercator(interX, interY);
                        const lineBearing = getBearing(lastLat, lastLng, lat, lng);

                        points.push({
                            lat: interpolatedCoords.lat,
                            lng: interpolatedCoords.lon,
                            windSpeed:  segment[i][4],
                            tempC:      segment[i][5],
                            gusts:      segment[i][6],
                            dewC:       segment[i][7],
                            rain:       segment[i][8],
                            humidity:   segment[i][9],
                            pressure:   segment[i][10],
                            clouds:     segment[i][11],
                            cloudsLow:  segment[i][12],
                            cloudsMid:  segment[i][13],
                            cloudsHigh: segment[i][14],
                            windDir:    segment[i][15],
                            snow:       segment[i][16],
                            waveHeight: segment[i][17],
                            wavePeriod: segment[i][18],
                            waveDir:    segment[i][19],
                            routeBearing: lineBearing
                        });

                        distanceCoveredInSegment += INTERVAL_KM;
                    }
                    accumulatedDist = segmentDist - (distanceCoveredInSegment - INTERVAL_KM);
                }
                else {
                    accumulatedDist += segmentDist;
                    if (accumulatedDist >= INTERVAL_KM) {
                        const currentBearing = getBearing(lastLat, lastLng, lat, lng);

                        points.push({
                            lat: lat,
                            lng: lng,
                            windSpeed:  segment[i][4],
                            tempC:      segment[i][5],
                            gusts:      segment[i][6],
                            dewC:       segment[i][7],
                            rain:       segment[i][8],
                            humidity:   segment[i][9],
                            pressure:   segment[i][10],
                            clouds:     segment[i][11],
                            cloudsLow:  segment[i][12],
                            cloudsMid:  segment[i][13],
                            cloudsHigh: segment[i][14],
                            windDir:    segment[i][15],
                            snow:       segment[i][16],
                            waveHeight: segment[i][17],
                            wavePeriod: segment[i][18],
                            waveDir:    segment[i][19],
                            routeBearing: currentBearing
                        });

                        accumulatedDist = 0;
                    }
                }

                lastLat = lat;
                lastLng = lng;
            }
        });

        return points;
    }, [tripData, selectedSecondary, currentZoom]);

    return (
        <div style={{ width: "100%", height: "100%", position: "relative" }}>
            <Map
                ref={mapRef}
                initialViewState={{ longitude: 20, latitude: 55, zoom: 6 }}
                mapStyle={OSM_STYLE}
                style={{ width: "100%", height: "100%" }}
                onMove={(e) => setCurrentZoom(e.viewState.zoom)}
            >
                {segmentsData.map(seg => {
                    const GRADIENT_WIDTH = 8;
                    const CONTOUR_THICKNESS = 2;

                    return (
                        <Source key={seg.id} id={seg.id} type="geojson" data={seg.geojson} lineMetrics>
                            {/* ZMIANA: ZASTOSOWANO LINE-GRADIENT DLA KONTURÓW */}
                            {activeMetrics.map((metricId, index) => {
                                if (!metricConfig[metricId]) return null;
                                const isTwoMetrics = activeMetrics.length === 2;

                                const contourWidth = GRADIENT_WIDTH + (CONTOUR_THICKNESS * 2);
                                const offsetCalc = (GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2);
                                const offset = isTwoMetrics ? (index === 0 ? -offsetCalc : offsetCalc) : 0;

                                return (
                                    <Layer
                                        key={`${seg.id}-${metricId}-contour`}
                                        id={`${seg.id}-${metricId}-contour`}
                                        type="line"
                                        paint={{
                                            "line-gradient": seg.contourGradients[metricId],
                                            "line-width": contourWidth,
                                            "line-offset": offset
                                        }}
                                        layout={{ "line-join": "round", "line-cap": "round" }}
                                    />
                                );
                            })}

                            {activeMetrics.map((metricId, index) => {
                                if (!metricConfig[metricId]) return null;
                                const isTwoMetrics = activeMetrics.length === 2;

                                const offsetCalc = (GRADIENT_WIDTH / 2) + (CONTOUR_THICKNESS / 2);
                                const offset = isTwoMetrics ? (index === 0 ? -offsetCalc : offsetCalc) : 0;

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
                                <Layer
                                    key={`${seg.id}-separator`}
                                    id={`${seg.id}-separator`}
                                    type="line"
                                    paint={{
                                        "line-gradient": seg.separatorStops,
                                        "line-width": 1.5,
                                        "line-offset": 0
                                    }}
                                    layout={{ "line-join": "round", "line-cap": "round" }}
                                />
                            )}
                        </Source>
                    );
                })}

                {sampledPoints.map((pt, i) => {
                    const bearingRad = pt.routeBearing * (Math.PI / 180);

                    return selectedSecondary.map((metricId, index) => {
                        if (!metricId) return null;

                        const isArrow = metricId.includes('dir');

                        let rawVal = null;
                        if (isArrow) {
                            rawVal = metricId === 'wind_dir' ? pt.windDir : pt.waveDir;
                        } else {
                            rawVal = metricConfig[metricId] ? metricConfig[metricId].getValue(pt) : null;
                        }

                        if (rawVal === null || rawVal === undefined) return null;

                        const themeColor = index === 0 ? "#109346" : "#a11ecf";
                        const distancePx = isArrow ? 22 : 16;

                        const perpX = Math.cos(bearingRad) * distancePx;
                        const perpY = Math.sin(bearingRad) * distancePx;

                        const dx = index === 0 ? -perpX : perpX;
                        const dy = index === 0 ? -perpY : perpY;

                        if (isArrow) {
                            const safeDir = rawVal;

                            return (
                                <Marker key={`sec-${i}-${metricId}`} longitude={pt.lng} latitude={pt.lat} offset={[dx, dy]} anchor="center">
                                    <div style={{
                                        width: '20px',
                                        height: '20px',
                                        backgroundColor: '#000000',
                                        WebkitMaskImage: `url(/icons/direction.png)`,
                                        WebkitMaskSize: 'contain',
                                        WebkitMaskRepeat: 'no-repeat',
                                        WebkitMaskPosition: 'center',
                                        maskImage: `url(/icons/direction.png)`,
                                        maskSize: 'contain',
                                        maskRepeat: 'no-repeat',
                                        maskPosition: 'center',
                                        transform: `rotate(${safeDir - 90}deg) scale(1.2, 0.6)`
                                    }} />
                                </Marker>
                            );
                        } else {
                            const displayVal = metricConfig[metricId].formatValue ? metricConfig[metricId].formatValue(rawVal) : Math.round(rawVal);
                            const text = `${displayVal}${metricConfig[metricId].unit}`;

                            let anchorY = "center";
                            if (dy < -4) anchorY = "bottom";
                            else if (dy > 4) anchorY = "top";

                            let anchorX = "center";
                            if (dx < -4) anchorX = "right";
                            else if (dx > 4) anchorX = "left";

                            let finalAnchor = "center";
                            if (anchorY !== "center" && anchorX !== "center") finalAnchor = `${anchorY}-${anchorX}`;
                            else if (anchorY !== "center") finalAnchor = anchorY;
                            else if (anchorX !== "center") finalAnchor = anchorX;

                            return (
                                <Marker key={`sec-${i}-${metricId}`} longitude={pt.lng} latitude={pt.lat} offset={[dx, dy]} anchor={finalAnchor}>
                                    <div style={{
                                        backgroundColor: "white",
                                        border: `2px solid ${themeColor}`,
                                        color: themeColor,
                                        padding: "3px 6px",
                                        borderRadius: "6px",
                                        fontSize: "0.75rem",
                                        fontWeight: "800",
                                        boxShadow: "0 2px 4px rgba(0,0,0,0.15)",
                                        whiteSpace: "nowrap"
                                    }}>
                                        {text}
                                    </div>
                                </Marker>
                            );
                        }
                    });
                })}
            </Map>

            <div style={{
                position: "absolute",
                bottom: "20px",
                right: isPanelOpen ? "370px" : "20px",
                width: "280px",
                display: "flex",
                flexDirection: "column",
                gap: "10px",
                zIndex: 10,
                transition: "right 0.3s ease-in-out"
            }}>
                {activeMetrics.map((metricId, index) => (
                    <MapLegend key={`${metricId}-${index}`} metricId={metricId} />
                ))}
            </div>

        </div>
    );
};

export default TripMap;