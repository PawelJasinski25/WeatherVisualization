import {useState} from "react";
import FileUpload from "./components/FileUpload.jsx";
import TripMap from "./components/TripMap.jsx";

import 'leaflet/dist/leaflet.css';

function App(){
    const [currentTripId, setCurrentTripId] = useState(null);


    return (
        <div style={{ display: "flex", height: "100vh", fontFamily: "Arial, sans-serif" }}>

            {/* LEWA STRONA: Panel wgrywania */}
            {/* Przekazujemy funkcję: "Jak skończysz, ustaw mi nowe ID" */}
            <FileUpload onUploadSuccess={(id) => setCurrentTripId(id)} />

            {/* PRAWA STRONA: Mapa */}
            <div style={{ flex: 1, position: "relative" }}>

                {currentTripId ? (
                    // Jeśli mamy ID, pokazujemy mapę dla tego ID
                    <TripMap tripId={currentTripId} />
                ) : (
                    // Jeśli nie mamy ID, pokazujemy powitanie
                    <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
                        <div style={{ textAlign: "center", color: "#666" }}>
                            <h1>Witaj w Weather Visualization 🗺️</h1>
                            <p>⬅️ Wgraj plik GPX w panelu po lewej, aby zobaczyć trasę.</p>
                        </div>
                    </div>
                )}

            </div>

        </div>
    );
}

export default App;
