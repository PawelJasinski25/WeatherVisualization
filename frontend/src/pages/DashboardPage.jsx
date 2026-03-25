import { useState } from "react";
import TripMap from "../components/TripMap.jsx";
import Navbar from "../components/Navbar.jsx";
import FileUploadModal from "../components/FileUploadModal.jsx";
import { useLocation } from "react-router-dom";
import SidePanel from "../components/SidePanel.jsx";

function DashboardPage() {
    const location = useLocation();
    const [currentTripId, setCurrentTripId] = useState(location.state?.tripId || null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    const [selectedPrimary, setSelectedPrimary] = useState(['wind', 'temp']);
    const [selectedSecondary, setSelectedSecondary] = useState(['wind_dir']);

    return (
        <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>

            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => setCurrentTripId(id)}
            />

            <div style={{ flex: 1, position: "relative", backgroundColor: "#f0f0f0", zIndex: 0 }}>

                {currentTripId && (
                    <SidePanel
                        selectedPrimary={selectedPrimary}
                        setSelectedPrimary={setSelectedPrimary}
                        selectedSecondary={selectedSecondary}
                        setSelectedSecondary={setSelectedSecondary}
                    />
                )}


                {currentTripId ? (
                    <TripMap tripId={currentTripId} />
                ) : (
                    <div style={styles.emptyState}>
                        <div style={{ fontSize: '4rem' }}>🗺️</div>
                        <h2 style={{ color: '#555', marginBottom: '10px' }}>Witaj w Weather Visualization</h2>
                        <p style={{ color: '#888' }}>
                            Kliknij przycisk <b>"Nowa Trasa"</b> w prawym górnym rogu,<br/>
                            aby wgrać plik GPX i zobaczyć wizualizację.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}

const styles = {
    emptyState: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        height: "100%",
        textAlign: "center"
    }
};

export default DashboardPage;