import { useEffect, useState } from "react";
import api from "../api/axios.js";
import { FileUp, Loader2, AlertCircle } from "lucide-react";
import "../styles/modal.css";

const FileUploadModal = ({ isOpen, onClose, onUploadSuccess }) => {
    const [file, setFile] = useState(null);
    const [status, setStatus] = useState("");
    const [isUploading, setIsUploading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setFile(null);
            setStatus("");
            setIsUploading(false);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
        setStatus("");
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

            const newTripId = response.data;
            onUploadSuccess(newTripId);
            onClose();
        } catch (error) {
            console.error("Błąd:", error);
            setStatus("Błąd: Nie udało się wgrać pliku.");
        } finally {
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

                    {status && status.includes("Błąd") && (
                        <div className="modal-error-message">
                            <AlertCircle size={18} />
                            <span>{status}</span>
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    <button onClick={onClose} disabled={isUploading} className="modal-btn btn-cancel">
                        Anuluj
                    </button>
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
                </div>
            </div>
        </div>
    );
};

export default FileUploadModal;