import { useState } from "react";
import TripMap from "../components/TripMap.jsx";
import Navbar from "../components/Navbar.jsx";
import FileUploadModal from "../components/FileUploadModal.jsx";
import { useLocation } from "react-router-dom";
import SidePanel from "../components/SidePanel.jsx";
import "../styles/dashboard.css";

function DashboardPage() {
    const location = useLocation();
    const [currentTripId, setCurrentTripId] = useState(location.state?.tripId || null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    const [selectedPrimary, setSelectedPrimary] = useState(['wind', 'temp']);
    const [selectedSecondary, setSelectedSecondary] = useState(['wind_dir']);
    const [isSidePanelOpen, setIsSidePanelOpen] = useState(true);

    return (
        <div className="dashboard-wrapper">
            <Navbar
                onOpenUpload={() => setIsUploadModalOpen(true)}
                activeTab="map"
                currentTripId={currentTripId}
            />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => setCurrentTripId(id)}
            />

            <div className="dashboard-content">
                {currentTripId && (
                    <SidePanel
                        selectedPrimary={selectedPrimary}
                        setSelectedPrimary={setSelectedPrimary}
                        selectedSecondary={selectedSecondary}
                        setSelectedSecondary={setSelectedSecondary}
                        isOpen={isSidePanelOpen}
                        setIsOpen={setIsSidePanelOpen}
                    />
                )}

                {currentTripId ? (
                    <TripMap
                        tripId={currentTripId}
                        selectedPrimary={selectedPrimary}
                        selectedSecondary={selectedSecondary}
                        isPanelOpen={isSidePanelOpen}
                    />
                ) : (
                    <div className="dashboard-empty">
                        <div className="dashboard-empty-icon">🗺️</div>
                        <h2 className="dashboard-empty-title">Witaj w Weather Visualization</h2>
                        <p className="dashboard-empty-text">
                            Kliknij przycisk <b>"Nowa Trasa"</b> w prawym górnym rogu,<br/>
                            aby wgrać plik GPX i zobaczyć wizualizację.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}

export default DashboardPage;