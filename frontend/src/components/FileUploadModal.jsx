import { useEffect, useState } from "react";
import api from "../api/axios.js";
import { FileUp, Loader2, AlertCircle, AlertTriangle } from "lucide-react";
import "../styles/modal.css";

const FileUploadModal = ({ isOpen, onClose, onUploadSuccess }) => {
    const [file, setFile] = useState(null);
    const [status, setStatus] = useState("");
    const [isUploading, setIsUploading] = useState(false);
    const [existingId, setExistingId] = useState(null);
    const isTripsPage = window.location.pathname.includes('/trips');

    useEffect(() => {
        if (isOpen) {
            setFile(null);
            setStatus("");
            setIsUploading(false);
            setExistingId(null);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
        setStatus("");
        setExistingId(null);
    };

    const handleUpload = async () => {
        if (!file) {
            setStatus("Błąd: Proszę wybrać plik GPX.");
            return;
        }

        setIsUploading(true);
        setStatus("");

        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await api.post("/trips/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });

            const { tripId, isDuplicate, tripName } = response.data;

            if (isDuplicate) {
                setExistingId(tripId);
                setStatus({
                    type: "warning",
                    message: "Plik z tą trasą znajduje się już na twoim koncie pod nazwą:",
                    tripName: tripName
                });
                setIsUploading(false);
            } else {
                onUploadSuccess(tripId);
                onClose();
            }
        } catch (error) {
            console.error("Błąd:", error);
            setStatus({ type: "error", message: "Nie udało się wgrać pliku." });
            setIsUploading(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h3>Wgraj nową trasę GPX</h3>
                    <button onClick={onClose} disabled={isUploading} className="modal-close-btn">&times;</button>
                </div>

                <div className="modal-body">
                    <label className={`modal-input file-drop-zone ${isUploading ? 'disabled' : ''}`}>
                        <FileUp size={48} color="#64748b" />

                        <span className="file-drop-zone-text">
                            {file ? `Wybrano: ${file.name}` : "Kliknij, aby wybrać plik GPX"}
                        </span>

                        <input
                            type="file"
                            accept=".gpx"
                            onChange={handleFileChange}
                            className="hidden-input"
                            disabled={isUploading}
                        />
                    </label>

                    {status.message && (
                        <div className={`modal-message ${status.type === 'warning' ? 'modal-warning-message' : 'modal-error-message'}`}>
                            {status.type === 'warning' ? <AlertTriangle size={18} /> : <AlertCircle size={18} />}
                            <div>
                                <span>{status.message} </span>
                                {status.tripName && (
                                    <strong style={{ color: '#b45309' }}>
                                        "{status.tripName}"
                                    </strong>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    <button onClick={onClose} disabled={isUploading} className="modal-btn btn-cancel">
                        Anuluj
                    </button>

                    {existingId ? (
                        <button
                            onClick={() => {
                                onUploadSuccess(existingId);
                                onClose();
                            }}
                            className="modal-btn btn-submit"
                        >
                            {isTripsPage ? "Przejdź do tras" : "Otwórz trasę"}
                        </button>
                    ) : (
                        <button
                            onClick={handleUpload}
                            disabled={isUploading || !file}
                            className="modal-btn btn-submit"
                        >
                            {isUploading ? (
                                <span className="btn-loading-content">
                                    <Loader2 size={16} className="anim-spin" /> Wgrywanie...
                                </span>
                            ) : "Wgraj"}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default FileUploadModal;