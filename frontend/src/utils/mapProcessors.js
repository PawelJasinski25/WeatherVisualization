// src/utils/mapProcessors.js
import { metricConfig } from '../config/metricConfig';
import { getProjectedDistance, getDistanceFromLatLonInKm, getProjectedCoords, unprojectMercator, getBearing, interpolateColor } from './geoUtils';

const GAP_THRESHOLD_KM = 150.0;

export const generateSegmentsData = (tripData, activeMetrics) => {
    if (!tripData || !tripData.length) return [];

    const allSegments = [];
    let currentSegment = [];
    let lastPt = null;

    //dzielimy całą trasę na segmenty zgodnie z segmentId
    tripData.forEach(d => {
        const cut = lastPt && d.segmentId !== undefined && lastPt.segmentId !== undefined && d.segmentId !== lastPt.segmentId;
        if (cut) {
            if (currentSegment.length >= 2) allSegments.push(currentSegment);
            currentSegment = [];
        }
        currentSegment.push({ coordinates: [d.longitude, d.latitude], ...d });
        lastPt = d;
    });

    if (currentSegment.length >= 2) allSegments.push(currentSegment);

    return allSegments.map((seg, idx) => {
        const segmentId = `segment-${idx}`;
        const allGradients = {};
        const allContourGradients = {};
        let separatorStops = null;

        //liczymy dystanse między punktami w segmencie
        let totalDist = 0;
        const dists = [0];

        const isGap = [false];
        for (let i = 1; i < seg.length; i++) {
            const d = getProjectedDistance(seg[i-1].latitude, seg[i-1].longitude, seg[i].latitude, seg[i].longitude);
            const dKm = getDistanceFromLatLonInKm(seg[i-1].latitude, seg[i-1].longitude, seg[i].latitude, seg[i].longitude);
            totalDist += d;
            dists.push(totalDist)
            isGap.push(dKm > GAP_THRESHOLD_KM);
        }

        activeMetrics.forEach((metricKey, index) => {
            const stops = ["interpolate", ["linear"], ["line-progress"]];
            const contourStops = ["interpolate", ["linear"], ["line-progress"]];
            const contourColor = index === 0 ? "rgba(33,59,212,1)" : "rgba(248,118,13,1)";
            let lastProgress = -1;
            let prevIsNull = false;

            //liczymy progres dla rysowania gradientu
            seg.forEach((pt, i) => {
                let progress = totalDist === 0 ? (i / (seg.length - 1)) : (dists[i] / totalDist);
                if (progress <= lastProgress) progress = lastProgress + 0.00000001;

                const val = metricConfig[metricKey].getValue(pt);
                const isNull = val === null || val === undefined;
                const gapHere = isGap[i];

                const color = isNull ? "#000000" : interpolateColor(val, metricConfig[metricKey].palette);
                const cColor = isNull ? "#000000" : contourColor;

                if (i === 0) {
                    stops.push(progress, color);
                    contourStops.push(progress, cColor);
                } else {
                    const gap = progress - lastProgress;
                    const epsilon = gap * 0.001;

                    if (gapHere) {
                        if (!prevIsNull) {
                            stops.push(lastProgress + epsilon, "#000000");
                            contourStops.push(lastProgress + epsilon, "#000000");
                        }
                        if (!isNull) {
                            stops.push(progress - epsilon, "#000000");
                            contourStops.push(progress - epsilon, "#000000");
                        }
                    } else {
                        if (!prevIsNull && isNull) {
                            stops.push(lastProgress + epsilon, "#000000");
                            contourStops.push(lastProgress + epsilon, "#000000");
                        } else if (prevIsNull && !isNull) {
                            stops.push(progress - epsilon, "#000000");
                            contourStops.push(progress - epsilon, "#000000");
                        }
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
                const gapHere = isGap[i];
                const sepColor = isNull ? "#000000" : "rgba(0,0,0,1)";

                if (i === 0) {
                    separatorStops.push(progress, sepColor);
                } else {
                    const gap = progress - lastProgress;
                    const epsilon = gap * 0.001;

                    if (gapHere) {
                        if (!prevIsNull) separatorStops.push(lastProgress + epsilon, "#000000");
                        if (!isNull) separatorStops.push(progress - epsilon, "#000000");

                    } else {
                        if (!prevIsNull && isNull) separatorStops.push(lastProgress + epsilon, "#000000");
                        else if (prevIsNull && !isNull) separatorStops.push(progress - epsilon, "#000000");
                    }

                    separatorStops.push(progress, sepColor);
                }

                lastProgress = progress;
                prevIsNull = isNull;
            });
        }

        return { id: segmentId, geojson: { type: "Feature", geometry: { type: "LineString", coordinates: seg.map(p => p.coordinates) } }, gradients: allGradients, contourGradients: allContourGradients, separatorStops: separatorStops };
    });
};

export const generateSampledPoints = (tripData) => {
    if (!tripData || !tripData.length) return [];

    const allSegments = [];
    let currentSegment = [];
    let lastPt = null;

    //dzielenie na segmenty
    tripData.forEach(d => {
        const cut = lastPt && d.segmentId !== undefined && lastPt.segmentId !== undefined && d.segmentId !== lastPt.segmentId;
        if (cut) {
            if (currentSegment.length >= 2) allSegments.push(currentSegment);
            currentSegment = [];
        }
        currentSegment.push(d);
        lastPt = d;
    });

    if (currentSegment.length >= 2) allSegments.push(currentSegment);

    let totalTripDistance = 0;
    allSegments.forEach(segment => {
        for (let i = 1; i < segment.length; i++) {
            totalTripDistance += getDistanceFromLatLonInKm(
                segment[i-1].latitude, segment[i-1].longitude,
                segment[i].latitude, segment[i].longitude
            );
        }
    });

    let baseInterval = 10;
    if (totalTripDistance < 20) baseInterval = 2;
    else if (totalTripDistance < 100) baseInterval = 5;

    const INTERVAL_KM = baseInterval;
    const candidatePoints = [];

    //obliczamy kierunek, w któym biegnie trasa
    allSegments.forEach(segment => {
        let accumulatedDist = 0;
        let lastLat = segment[0].latitude;
        let lastLng = segment[0].longitude;

        let initialBearing = 0;
        if (segment.length > 1) {
            initialBearing = getBearing(segment[0].latitude, segment[0].longitude, segment[1].latitude, segment[1].longitude);
        }

        candidatePoints.push({ ...segment[0], lat: segment[0].latitude, lng: segment[0].longitude, routeBearing: initialBearing });

        //rozmieszczenie punktow mniej wiecej co interval
        for (let i = 1; i < segment.length; i++) {
            const lat = segment[i].latitude;
            const lng = segment[i].longitude;
            const segmentDist = getDistanceFromLatLonInKm(lastLat, lastLng, lat, lng);

            if (segmentDist > GAP_THRESHOLD_KM) {
                accumulatedDist = 0;
                const currentBearing = getBearing(lastLat, lastLng, lat, lng);
                candidatePoints.push({ ...segment[i], lat: lat, lng: lng, routeBearing: currentBearing });

            } else {
                accumulatedDist += segmentDist;
                if (accumulatedDist >= INTERVAL_KM) {
                    const currentBearing = getBearing(lastLat, lastLng, lat, lng);
                    candidatePoints.push({ ...segment[i], lat: lat, lng: lng, routeBearing: currentBearing });
                    accumulatedDist = 0;
                }
            }
            lastLat = lat;
            lastLng = lng;
        }
    });

    candidatePoints.forEach(p => p.minZoom = 99);

    //sprawdzanie kolizji
    let clearanceFactor = 6000;
    if (totalTripDistance < 30) clearanceFactor = 200;
    else if (totalTripDistance < 100) clearanceFactor = 1500;

    const zoomLevels = [5, 6, 7, 8, 9, 10];

    for (const z of zoomLevels) {
        const MIN_CLEARANCE_KM = clearanceFactor / Math.pow(2, z);
        const selectedAtThisZoom = candidatePoints.filter(p => p.minZoom <= z);

        for (const candidate of candidatePoints) {
            if (candidate.minZoom <= z) continue;

            let hasCollision = false;
            for (const selected of selectedAtThisZoom) {
                if (getDistanceFromLatLonInKm(candidate.lat, candidate.lng, selected.lat, selected.lng) < MIN_CLEARANCE_KM) {
                    hasCollision = true;
                    break;
                }
            }

            if (!hasCollision) {
                candidate.minZoom = z;
                selectedAtThisZoom.push(candidate);
            }
        }
    }

    return candidatePoints;
};


// Funkcja odcinająca płynne kolory dla MapLibre
export const sharpenMapboxGradient = (gradientExp) => {
    if (!Array.isArray(gradientExp) || gradientExp[0] !== 'interpolate') {
        return gradientExp;
    }

    const sharpExp = ['interpolate', ['linear'], ['line-progress']];
    const EPSILON = 0.000001;

    let lastProg = -1;
    let lastCol = null;

    for (let i = 3; i < gradientExp.length; i += 2) {
        let prog = gradientExp[i];
        const col = gradientExp[i + 1];

        if (prog > 1.0) prog = 1.0;
        if (prog < lastProg) prog = lastProg;

        if (lastCol === null) {
            sharpExp.push(prog, col);
            lastProg = prog;
            lastCol = col;
            continue;
        }

        if (col !== lastCol) {
            let cutProg = prog - EPSILON;

            if (cutProg <= lastProg) {
                cutProg = lastProg + EPSILON;
                prog = cutProg + EPSILON;

                if (prog > 1.0) {
                    cutProg = 1.0 - EPSILON;
                    prog = 1.0;
                }
            }

            sharpExp.push(cutProg, lastCol);
            sharpExp.push(prog, col);

            lastProg = prog;
            lastCol = col;
        } else {
            lastProg = prog;
        }
    }
    if (lastProg > sharpExp[sharpExp.length - 2]) {
        sharpExp.push(lastProg, lastCol);
    }

    if (sharpExp.length < 5) return gradientExp;
    return sharpExp;
};

export const calculateMarkersOffset = (markers) => {
    if (!markers || markers.length === 0) return [];

    const groups = {};
    markers.forEach(marker => {
        const key = `${marker.lat.toFixed(5)}-${marker.lng.toFixed(5)}`;
        if (!groups[key]) groups[key] = [];
        groups[key].push(marker);
    });

    const result = [];
    Object.values(groups).forEach(group => {
        const total = group.length;
        group.forEach((marker, i) => {
            const offsetX = total > 1 ? (i - (total - 1) / 2) * 14 : 0;
            result.push({ ...marker, offsetX, offsetY: 0 });
        });
    });

    return result;
};