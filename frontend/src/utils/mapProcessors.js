// src/utils/mapProcessors.js
import { metricConfig } from '../config/metricConfig';
import { getProjectedDistance, getDistanceFromLatLonInKm, getProjectedCoords, unprojectMercator, getBearing, interpolateColor } from './geoUtils';

export const generateSegmentsData = (tripData, activeMetrics) => {
    if (!tripData || !tripData.length) return [];

    const allSegments = [];
    let currentSegment = [];
    let lastPt = null;

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

        let totalDist = 0;
        const dists = [0];
        for (let i = 1; i < seg.length; i++) {
            const d = getProjectedDistance(seg[i-1].latitude, seg[i-1].longitude, seg[i].latitude, seg[i].longitude);
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

                const color = isNull ? "#000000" : interpolateColor(val, metricConfig[metricKey].palette);
                const cColor = isNull ? "#000000" : contourColor;

                if (i === 0) {
                    stops.push(progress, color);
                    contourStops.push(progress, cColor);
                } else {
                    const gap = progress - lastProgress;
                    const epsilon = gap * 0.001;

                    if (!prevIsNull && isNull) {
                        stops.push(lastProgress + epsilon, "#000000");
                        contourStops.push(lastProgress + epsilon, "#000000");
                    } else if (prevIsNull && !isNull) {
                        stops.push(progress - epsilon, "#000000");
                        contourStops.push(progress - epsilon, "#000000");
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
                const sepColor = isNull ? "#000000" : "rgba(0,0,0,1)";

                if (i === 0) {
                    separatorStops.push(progress, sepColor);
                } else {
                    const gap = progress - lastProgress;
                    const epsilon = gap * 0.001;

                    if (!prevIsNull && isNull) separatorStops.push(lastProgress + epsilon, "#000000");
                    else if (prevIsNull && !isNull) separatorStops.push(progress - epsilon, "#000000");

                    separatorStops.push(progress, sepColor);
                }

                lastProgress = progress;
                prevIsNull = isNull;
            });
        }

        return { id: segmentId, geojson: { type: "Feature", geometry: { type: "LineString", coordinates: seg.map(p => p.coordinates) } }, gradients: allGradients, contourGradients: allContourGradients, separatorStops: separatorStops };
    });
};

export const generateSampledPoints = (tripData, selectedSecondary, currentZoom) => {
    if (!tripData || !tripData.length || selectedSecondary.filter(Boolean).length === 0 || currentZoom < 9.0) return [];

    const allSegments = [];
    let currentSegment = [];
    let lastPt = null;

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

    let INTERVAL_KM = 10;
    const points = [];

    allSegments.forEach(segment => {
        let accumulatedDist = 0;
        let lastLat = segment[0].latitude;
        let lastLng = segment[0].longitude;

        let initialBearing = 0;
        if (segment.length > 1) {
            initialBearing = getBearing(segment[0].latitude, segment[0].longitude, segment[1].latitude, segment[1].longitude);
        }

        points.push({ ...segment[0], lat: segment[0].latitude, lng: segment[0].longitude, routeBearing: initialBearing });

        for (let i = 1; i < segment.length; i++) {
            const lat = segment[i].latitude;
            const lng = segment[i].longitude;
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
                        ...segment[i],
                        lat: interpolatedCoords.lat,
                        lng: interpolatedCoords.lon,
                        routeBearing: lineBearing
                    });

                    distanceCoveredInSegment += INTERVAL_KM;
                }
                accumulatedDist = segmentDist - (distanceCoveredInSegment - INTERVAL_KM);
            } else {
                accumulatedDist += segmentDist;
                if (accumulatedDist >= INTERVAL_KM) {
                    const currentBearing = getBearing(lastLat, lastLng, lat, lng);
                    points.push({ ...segment[i], lat: lat, lng: lng, routeBearing: currentBearing });
                    accumulatedDist = 0;
                }
            }

            lastLat = lat;
            lastLng = lng;
        }
    });

    return points;
};