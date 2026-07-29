import React from 'react';
import '../styles/map-elements.css';
import { useMetricConfig } from '../config/metricConfig';

const AstronomyLegend = ({ config }) => {
    const blockHeight = 26;

    const events = [
        { label: "Świt Astronomiczny", offset: 1},
        { label: "Świt Nautyczny", offset: 2},
        { label: "Świt Cywilny", offset: 3 },
        { label: "Wschód Słońca", offset: 4 },
        { label: "Zachód Słońca", offset: 5 },
        { label: "Zmierzch Cywilny", offset: 6 },
        { label: "Zmierzch Nautyczny", offset: 7 },
        { label: "Zmierzch Astronomiczny", offset: 8},
    ];

    const pointEvents = [
        { label: "Kulminacja", color: "#FFFFFF", border: "#cbd5e1" },
        { label: "Wschód Księżyca", color: "#5cd546", border: "#94a3b8" },
        { label: "Zachód Księżyca", color: "#125c0c", border: "#475569" }
    ];

    const containerHeight = (config.palette ? config.palette.length : 9) * blockHeight;

    return (
        <div className="legend-box astro-legend-padding">
            <div className="legend-header astro-legend-header">
                <span className="legend-title">{config.label}</span>
            </div>

            <div className="astro-legend-body" style={{ flexDirection: 'column' }}>

                <div style={{ position: 'relative', height: `${containerHeight}px` }}>
                    <div className="astro-color-bar">
                        {config.palette.map((stop, i) => (
                            <div
                                key={`color-${i}`}
                                style={{
                                    height: `${blockHeight}px`,
                                    backgroundColor: `rgb(${stop[1].join(',')})`
                                }}
                            />
                        ))}
                    </div>

                    <div className="astro-labels-container">
                        {events.map((ev, i) => (
                            <div
                                key={`ev-${i}`}
                                className="astro-event-row"
                                style={{ top: `${ev.offset * blockHeight}px` }}
                            >
                                <div className="astro-tick" />
                                <span>{ev.label}</span>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="astro-point-events-wrapper">
                    {pointEvents.map((pt, i) => (
                        <div key={`pt-${i}`} className="astro-point-item">
                            <div className="astro-point-dot" style={{
                                backgroundColor: pt.color,
                                border: `2px solid ${pt.border}`
                            }} />
                            <span className="astro-point-text">
                                {pt.label}
                            </span>
                        </div>
                    ))}
                </div>

            </div>
        </div>
    );
};

const DefaultLegend = ({ metricId, config, minVal, maxVal }) => {
    const { palette, formatValue, label, unit, labelCount } = config;

    let startIndex = 0;
    let endIndex = palette.length - 1;

    if (minVal !== undefined && maxVal !== undefined && minVal !== null && maxVal !== null) {
        const start = palette.findIndex(stop => stop[0] >= minVal);
        startIndex = start > 0 ? start - 1 : 0;

        const end = palette.findIndex(stop => stop[0] >= maxVal);
        endIndex = end !== -1 ? end : palette.length - 1;

        if (startIndex >= endIndex) {
            if (startIndex > 0) startIndex -= 1;
            else if (endIndex < palette.length - 1) endIndex += 1;
        }
    }

    const croppedPalette = palette.slice(startIndex, endIndex + 1);
    const croppedMin = croppedPalette[0][0];
    const croppedMax = croppedPalette[croppedPalette.length - 1][0];
    const range = croppedMax - croppedMin || 1;

    const gradientString = croppedPalette.map(stop => {
        const percent = ((stop[0] - croppedMin) / range) * 100;
        return `rgb(${stop[1].join(",")}) ${percent}%`;
    }).join(", ");

    let limit = labelCount || 6;
    let step = range / (limit - 1);
    let labelValues = Array.from({ length: limit }, (_, i) => croppedMin + (i * step));

    let rawFormattedLabels = labelValues.map(val => formatValue ? formatValue(val) : val);
    let roundedNumericLabels = rawFormattedLabels.map(str => Math.round(Number(str)));
    let hasDuplicates = new Set(roundedNumericLabels).size !== roundedNumericLabels.length;

    if (hasDuplicates && limit > 5) {
        limit = 5;
        step = range / (limit - 1);
        labelValues = Array.from({ length: limit }, (_, i) => croppedMin + (i * step));
        rawFormattedLabels = labelValues.map(val => formatValue ? formatValue(val) : val);
    }

    return (
        <div className="legend-box">
            <div className="legend-header">
                <span className="legend-title">{label}</span>
                <span className="legend-unit">{unit}</span>
            </div>

            <div className="legend-bar-wrapper" style={{ background: `linear-gradient(to right, ${gradientString})` }}>
                {labelValues.map((val, i) => {
                    const rawStr = rawFormattedLabels[i];
                    const numVal = Number(rawStr);

                    let displayStr;
                    if (isNaN(numVal)) {
                        displayStr = rawStr;
                    } else {
                        displayStr = hasDuplicates ? Number(rawStr) : Math.round(numVal);
                    }

                    const leftPercent = ((val - croppedMin) / range) * 100;
                    let transform = "translateX(-50%)";
                    if (i === 0) transform = "translateX(4px)";
                    if (i === limit - 1) transform = "translateX(calc(-100% - 4px))";

                    return (
                        <span key={i} className="legend-label" style={{ left: `${leftPercent}%`, transform }}>
                            {displayStr}
                        </span>
                    );
                })}
            </div>
        </div>
    );
};

const MapLegend = ({ metricId, minVal, maxVal }) => {
    const metricConfig = useMetricConfig();

    if (!metricId || !metricConfig[metricId]) return null;

    const config = metricConfig[metricId];

    if (metricId === 'astronomy') {
        return <AstronomyLegend config={config} />;
    }

    return <DefaultLegend metricId={metricId} config={config} minVal={minVal} maxVal={maxVal} />;

};

export default MapLegend;