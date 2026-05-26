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

const DefaultLegend = ({ metricId, config }) => {
    const { palette, formatValue, label, unit, labelCount } = config;
    const gradientString = palette.map(stop => `rgb(${stop[1].join(",")})`).join(", ");

    const limit = labelCount || 6;
    const numLabels = Math.min(limit, palette.length);
    const labelIndices = Array.from({ length: numLabels }, (_, i) =>
        Math.floor(i * (palette.length - 1) / (numLabels - 1))
    );

    return (
        <div className="legend-box">
            <div className="legend-header">
                <span className="legend-title">{label}</span>
                <span className="legend-unit">{unit}</span>
            </div>

            <div className="legend-bar-wrapper" style={{ background: `linear-gradient(to right, ${gradientString})` }}>
                {labelIndices.map((index, i) => {
                    const val = palette[index][0];
                    const formattedVal = metricId === 'ocean_current_velocity'
                        ? (formatValue ? formatValue(val) : val)
                        : Math.round(Number(formatValue ? formatValue(val) : val));

                    const leftPercent = (index / (palette.length - 1)) * 100;

                    let transform = "translateX(-50%)";
                    if (index === 0) transform = "translateX(4px)";
                    if (index === palette.length - 1) transform = "translateX(calc(-100% - 4px))";

                    return (
                        <span key={i} className="legend-label" style={{ left: `${leftPercent}%`, transform }}>
                            {formattedVal}
                        </span>
                    );
                })}
            </div>
        </div>
    );
};

const MapLegend = ({ metricId }) => {
    const metricConfig = useMetricConfig();

    if (!metricId || !metricConfig[metricId]) return null;

    const config = metricConfig[metricId];

    if (metricId === 'astronomy') {
        return <AstronomyLegend config={config} />;
    }

    return <DefaultLegend metricId={metricId} config={config} />;
};

export default MapLegend;