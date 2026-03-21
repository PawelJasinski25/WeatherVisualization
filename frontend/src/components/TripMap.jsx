import React, { useEffect, useState, useMemo, useRef } from "react";
import Map, { Source, Layer } from "react-map-gl/maplibre";
import "maplibre-gl/dist/maplibre-gl.css";
import api from "../api/axios.js";

const OSM_STYLE = {
    "version": 8,
    "sources": {
        "osm": {
            "type": "raster",
            "tiles": ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"],
            "tileSize": 256,
            "attribution": "&copy; OpenStreetMap"
        }
    },
    "layers": [{ "id": "osm-tiles", "type": "raster", "source": "osm" }]
};

const windColors = [[0, [98, 113, 183]], [1, [57, 97, 159]], [3, [74, 148, 169]], [5, [77, 141, 123]], [7, [83, 165, 83]], [9, [53, 159, 53]], [11, [167, 157, 81]], [13, [159, 127, 58]], [15, [161, 108, 92]], [17, [129, 58, 78]], [19, [175, 80, 136]], [21, [117, 74, 147]], [24, [109, 97, 163]], [27, [68, 105, 141]], [29, [92, 144, 152]], [36, [125, 68, 165]], [46, [231, 215, 215]], [51, [219, 212, 135]], [77, [205, 202, 112]], [104, [128, 128, 128]]];
const tempColors = [[203, [115, 70, 105]], [218, [202, 172, 195]], [233, [162, 70, 145]], [248, [143, 89, 169]], [258, [157, 219, 217]], [265, [106, 191, 181]], [269, [100, 166, 189]], [273.15, [93, 133, 198]], [274, [68, 125, 99]], [283, [128, 147, 24]], [294, [243, 183, 4]], [303, [232, 83, 25]], [320, [71, 14, 0]]];

const interpolateColor = (value, palette) => {
    if (value <= palette[0][0]) return `rgb(${palette[0][1].join(',')})`;
    if (value >= palette[palette.length - 1][0]) return `rgb(${palette[palette.length - 1][1].join(',')})`;
    for (let i = 0; i < palette.length - 1; i++) {
        if (value >= palette[i][0] && value <= palette[i+1][0]) {
            const fraction = (value - palette[i][0]) / (palette[i+1][0] - palette[i][0]);
            const r = Math.round(palette[i][1][0] + fraction * (palette[i+1][1][0] - palette[i][1][0]));
            const g = Math.round(palette[i][1][1] + fraction * (palette[i+1][1][1] - palette[i][1][1]));
            const b = Math.round(palette[i][1][2] + fraction * (palette[i+1][1][2] - palette[i][1][2]));
            return `rgb(${r}, ${g}, ${b})`;
        }
    }
    return "rgb(128, 128, 128)";
};

const TripMap = ({ tripId }) => {
    const [tripData, setTripData] = useState([]);
    const mapRef = useRef(null);

    useEffect(() => {
        if (tripId) {
            api.get(`/trips/${tripId}/coordinates`).then(res => {
                setTripData(res.data);
                if (res.data.length > 0 && mapRef.current) {
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

    const { geojson, windGradient, tempGradient } = useMemo(() => {
        if (!tripData.length) return { geojson: null };

        const windStops = ["interpolate", ["linear"], ["line-progress"]];
        const tempStops = ["interpolate", ["linear"], ["line-progress"]];

        tripData.forEach((pt, i) => {
            const progress = i / (tripData.length - 1);
            windStops.push(progress, interpolateColor(progress * 50, windColors));
            tempStops.push(progress, interpolateColor(265 + progress * 38, tempColors));
        });

        return {
            geojson: { type: "Feature", geometry: { type: "LineString", coordinates: tripData.map(d => [d[1], d[0]]) } },
            windGradient: windStops,
            tempGradient: tempStops
        };
    }, [tripData]);

    const NUM_REPEATS = 5;
    const createRepeatingGradient = (stops, repeats) => {
        return [
            "interpolate",
            ["linear"],
            ["%", ["*", ["line-progress"], repeats], 1]
        ].concat(stops.slice(3));
    };

    const windRepeating = useMemo(() => windGradient ? createRepeatingGradient(windGradient, NUM_REPEATS) : [], [windGradient, NUM_REPEATS]);
    const tempRepeating = useMemo(() => tempGradient ? createRepeatingGradient(tempGradient, NUM_REPEATS) : [], [tempGradient, NUM_REPEATS]);

    return (
        <div style={{ width: "100%", height: "100%", position: "relative" }}>
            <Map
                ref={mapRef}
                initialViewState={{ longitude: 20, latitude: 55, zoom: 6 }}
                mapStyle={OSM_STYLE}
                style={{ width: "100%", height: "100%" }}
            >
                {geojson && (
                    <Source id="route-source" type="geojson" data={geojson} lineMetrics={true}>

                        <Layer id="wind-contour" type="line" paint={{
                            "line-color": "#213BD4",
                            "line-width": 9,         // 5 + 4
                            "line-offset": -2.5      // Połowa z 5
                        }} layout={{ "line-join": "round", "line-cap": "round" }} />

                        <Layer id="temp-contour" type="line" paint={{
                            "line-color": "#f8760d",
                            "line-width": 9,         // 5 + 4
                            "line-offset": 2.5       // Połowa z 5
                        }} layout={{ "line-join": "round", "line-cap": "round" }} />


                        <Layer id="wind-layer" type="line" paint={{
                            "line-width": 5,
                            "line-offset": -2.5,
                            "line-gradient": windRepeating
                        }} layout={{ "line-join": "round", "line-cap": "round" }} />

                        <Layer id="temp-layer" type="line" paint={{
                            "line-width": 5,
                            "line-offset": 2.5,
                            "line-gradient": tempRepeating
                        }} layout={{ "line-join": "round", "line-cap": "round" }} />
                    </Source>
                )}
            </Map>
        </div>
    );
};

export default TripMap;