import {useEffect, useState} from "react";
import api from "../api/axios.js";

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
        <div className="modal-overlay" onClick={onClose}>
            <div style={styles.modal} className="modal-content" onClick={(e) => e.stopPropagation()}>

                <div style={styles.header}>
                    <h3 style={{margin: 0}}>Wgraj Trasę GPX</h3>
                    <button onClick={onClose} style={styles.closeBtn}>&times;</button>
                </div>

                <div style={styles.body}>

                    <input
                        type="file"
                        accept=".gpx"
                        onChange={handleFileChange}
                        style={styles.input}
                    />
                    {status && <p style={{color: 'orange', fontSize: '0.9rem'}}>{status}</p>}
                </div>

                <div style={styles.footer}>
                    <button onClick={onClose} style={styles.cancelBtn}>Anuluj</button>
                    <button
                        onClick={handleUpload}
                        disabled={isUploading}
                        style={{...styles.uploadBtn, opacity: isUploading ? 0.7 : 1}}
                    >
                        {isUploading ? 'Wgrywanie...' : 'Wgraj plik'}
                    </button>
                </div>
            </div>
        </div>
    );
};

const styles = {
    modal: {
        backgroundColor: 'white',
        borderRadius: '12px',
        width: '400px',
        padding: '20px',
        boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid #eee',
        paddingBottom: '10px'
    },
    closeBtn: {
        background: 'none',
        border: 'none',
        fontSize: '1.5rem',
        cursor: 'pointer',
        color: '#999'
    },
    body: {
        display: 'flex',
        flexDirection: 'column',
        gap: '10px'
    },
    input: {
        border: '1px dashed #ccc',
        padding: '20px',
        borderRadius: '8px',
        width: '100%',
        boxSizing: 'border-box'
    },
    footer: {
        display: 'flex',
        justifyContent: 'flex-end',
        gap: '10px',
        paddingTop: '10px'
    },
    cancelBtn: {
        padding: '8px 16px',
        background: 'none',
        border: '1px solid #ddd',
        borderRadius: '6px',
        cursor: 'pointer',
        color: '#555'
    },
    uploadBtn: {
        padding: '8px 16px',
        background: '#007bff',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold'
    }
};

export default FileUploadModal;