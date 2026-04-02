import React from 'react';
import { Marker } from "react-map-gl/maplibre";
import { metricConfig } from '../config/metricConfig';

const MetricMarker = ({ pt, metricId, index }) => {
    if (!metricId) return null;

    const isArrow = metricId.includes('dir');
    let rawVal = null;

    if (isArrow) {
        if (metricId === 'wind_dir') rawVal = pt.windDir;
        else if (metricId === 'wave_dir') rawVal = pt.waveDir;
        else if (metricId === 'ocean_current_direction') rawVal = pt.oceanCurrentDir;
    } else {
        rawVal = metricConfig[metricId] ? metricConfig[metricId].getValue(pt) : null;
    }

    if (rawVal === null || rawVal === undefined) return null;

    const themeColor = index === 0 ? "#109346" : "#a11ecf";
    const distancePx = isArrow ? 22 : 16;
    const bearingRad = pt.routeBearing * (Math.PI / 180);

    const perpX = Math.cos(bearingRad) * distancePx;
    const perpY = Math.sin(bearingRad) * distancePx;
    const dx = index === 0 ? -perpX : perpX;
    const dy = index === 0 ? -perpY : perpY;

    if (isArrow) {
        const safeDir = rawVal;
        const outlineWidth = "3";

        return (
            <Marker longitude={pt.lng} latitude={pt.lat} offset={[dx, dy]} anchor="center">
                <svg
                    width="20" height="20" viewBox="0 0 24 24"
                    style={{ transform: `rotate(${safeDir - 90}deg) scale(1.2, 0.6)`, transformOrigin: 'center', overflow: 'visible' }}
                >
                    <path d="M3 3 L22 12 L3 21 L8 12 Z" fill="#000000" stroke={themeColor} strokeWidth={outlineWidth} strokeLinejoin="round" />
                </svg>
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
            <Marker longitude={pt.lng} latitude={pt.lat} offset={[dx, dy]} anchor={finalAnchor}>
                <div style={{
                    backgroundColor: "white", border: `2px solid ${themeColor}`, color: themeColor,
                    padding: "3px 6px", borderRadius: "6px", fontSize: "0.75rem", fontWeight: "800",
                    boxShadow: "0 2px 4px rgba(0,0,0,0.15)", whiteSpace: "nowrap"
                }}>
                    {text}
                </div>
            </Marker>
        );
    }
};

export default MetricMarker;