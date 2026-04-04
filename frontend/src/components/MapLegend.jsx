import React from 'react';
import { metricConfig } from '../config/metricConfig';
import '../styles/dashboard.css';

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
        <div className="legend-box">
            <div className="legend-header">
                <span className="legend-title">{config.label}</span>
                <span className="legend-unit">{config.unit}</span>
            </div>

            <div className="legend-bar-wrapper" style={{ background: `linear-gradient(to right, ${gradientString})` }}>
                {labelIndices.map((index, i) => {
                    const val = palette[index][0];
                    const formattedVal = metricId === 'ocean_current_velocity'
                        ? (config.formatValue ? config.formatValue(val) : val)
                        : Math.round(Number(config.formatValue ? config.formatValue(val) : val));
                    const leftPercent = (index / (palette.length - 1)) * 100;

                    let transform = "translateX(-50%)";
                    if (index === 0) transform = "translateX(4px)";
                    if (index === palette.length - 1) transform = "translateX(calc(-100% - 4px))";

                    return (
                        <span key={i} className="legend-label" style={{ left: `${leftPercent}%`, transform: transform }}>
                            {formattedVal}
                        </span>
                    );
                })}
            </div>
        </div>
    );
};

export default MapLegend;