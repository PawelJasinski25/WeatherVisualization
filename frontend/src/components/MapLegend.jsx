import React from 'react';
import { metricConfig } from '../config/metricConfig';

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
                    const formattedVal = metricId === 'ocean_current_velocity'
                        ? (config.formatValue ? config.formatValue(val) : val)
                        : Math.round(Number(config.formatValue ? config.formatValue(val) : val));
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

export default MapLegend;