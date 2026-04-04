import {useEffect, useState} from "react";
import api from "../api/axios.js";
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
            alert("Proszę wybrać plik GPX");
            return;
        }

        setIsUploading(true);
        setStatus("Wgrywanie...");

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
            setStatus("Błąd wgrywania.");
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h3>Wgraj nową trasę GPX</h3>
                    <button onClick={onClose} className="modal-close-btn">&times;</button>
                </div>

                <div className="modal-body">
                    <input
                        type="file"
                        accept=".gpx"
                        onChange={handleFileChange}
                        className="modal-input"
                    />
                    {status && <p style={{ color: status.includes("Błąd") ? "red" : "#007bff", margin: 0, fontSize: "0.9rem" }}>{status}</p>}
                </div>

                <div className="modal-footer">
                    <button onClick={onClose} disabled={isUploading} className="modal-btn btn-cancel">
                        Anuluj
                    </button>
                    <button onClick={handleUpload} disabled={isUploading || !file} className="modal-btn btn-submit">
                        {isUploading ? "Wgrywanie..." : "Wgraj"}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default FileUploadModal;