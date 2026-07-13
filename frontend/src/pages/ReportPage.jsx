import { useState } from "react";
import { useLocation } from "react-router-dom";
import Navbar from "../components/Navbar.jsx";
import FileUploadModal from "../components/FileUploadModal.jsx";
import ReportGenerator from "../components/report/ReportGenerator.jsx";
import "../styles/dashboard.css";
import {FileText} from "lucide-react";

function ReportPage() {
    const location = useLocation();

    const [currentTripId, setCurrentTripId] = useState(location.state?.tripId || null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    return (
        <div className="dashboard-wrapper">
            <Navbar
                onOpenUpload={() => setIsUploadModalOpen(true)}
                activeTab="report"
                currentTripId={currentTripId}
            />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
                onUploadSuccess={(id) => setCurrentTripId(id)}
            />

            <div className="dashboard-content" style={{ overflow: 'hidden' }}>
                {currentTripId ? (
                    <ReportGenerator tripId={currentTripId} />
                ) : (
                    <div className="dashboard-empty">
                        <div className="dashboard-empty-icon"><FileText size={100} color="var(--theme-report)"/></div>
                        <h2 className="dashboard-empty-title">Brak wybranej trasy</h2>
                        <p className="dashboard-empty-text">
                            Przejdź do zakładki <b>"Moje trasy"</b> lub wgraj nowy plik GPX,<br/>
                            aby wygenerować raport PDF.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}

export default ReportPage;