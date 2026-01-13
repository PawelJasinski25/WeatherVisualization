import { useState } from "react";
import TripMap from "../components/TripMap.jsx";
import Navbar from "../components/Navbar.jsx";
import FileUploadModal from "../components/FileUploadModal.jsx";

function DashboardPage() {
    const [currentTripId, setCurrentTripId] = useState(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    return (
        <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>

            {/*Navbar*/}
            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} />

            {/*Modal */}
            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => setCurrentTripId(id)}
            />

            <div style={{ flex: 1, position: "relative", backgroundColor: "#f0f0f0", zIndex: 0 }}>
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