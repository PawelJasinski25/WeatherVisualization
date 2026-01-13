import {useEffect, useState} from "react";
import { MapContainer, TileLayer, Polyline, useMap } from 'react-leaflet';
import api from "../api/axios.js";


function RecenterMap({ positions }) {
    const map = useMap();

    useEffect(() => {
        if (positions && positions.length > 0) {
            map.fitBounds(positions);
        }
    }, [positions, map]);

    return null;
}

const TripMap = ({ tripId }) => {
    const [positions, setPositions] = useState([]);

    useEffect(() => {
        if (!tripId) return;

        console.log("Pobieram dane dla ID:", tripId);

        api.get(`/trips/${tripId}/coordinates`)
            .then(response => {
                setPositions(response.data);
            })
            .catch(err => {
                console.error("Błąd pobierania trasy:", err);
                if (err.response && err.response.status === 403) {
                    console.error("Brak dostępu! Czy na pewno jesteś zalogowany?");
                }
            });
    }, [tripId]);

    const mapBounds = [
        [-85, -Infinity]
        [85, Infinity]
    ];

    return (
        <div style={{ height: "100%", width: "100%", background: "red" }}>
            <MapContainer center={[52, 19]} zoom={6} style={{ height: "100%", width: "100%" }}
                          minZoom={2}
                          maxBounds={mapBounds}
                          maxBoundsViscosity={1.0}>
                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                {positions.length > 0 && (
                    <>
                        <Polyline positions={positions} color="blue" weight={4} />
                        <RecenterMap positions={positions} />
                    </>
                )}

            </MapContainer>
        </div>
    );
};

export default TripMap;