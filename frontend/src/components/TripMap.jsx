import {useEffect, useState} from "react";
import { MapContainer, TileLayer, Polyline, useMap } from 'react-leaflet';


function RecenterMap({ positions }) {
    const map = useMap();

    useEffect(() => {
        if (positions && positions.length > 0) {
            map.fitBounds(positions); // Dopasuj zoom do trasy
        }
    }, [positions, map]);

    return null;
}

const TripMap = ({ tripId }) => {
    const [positions, setPositions] = useState([]);

    useEffect(() => {
        if (!tripId) return;

        console.log("Pobieram dane dla ID:", tripId);

        fetch(`http://localhost:8080/api/trips/${tripId}/coordinates`)
            .then(res => res.json())
            .then(data => {
                setPositions(data); // Zapisujemy punkty w stanie mapy
            })
            .catch(err => console.error("Błąd pobierania trasy:", err));
    }, [tripId]);

    return (
        <div style={{ height: "100vh", width: "100%", background: "red" }}>
            <MapContainer center={[52, 19]} zoom={6} style={{ height: "100%", width: "100%" }}>

                {/* Podkład mapy (OpenStreetMap) */}
                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {/* Jeśli mamy punkty, rysujemy linię */}
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