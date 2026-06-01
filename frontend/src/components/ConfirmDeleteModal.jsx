import React, { useState } from 'react';
import { AlertTriangle, Loader2 } from 'lucide-react';
import '../styles/modal.css';

const ConfirmDeleteModal = ({ isOpen, onClose, onConfirm, tripName }) => {
    const [isDeleting, setIsDeleting] = useState(false);

    if (!isOpen) return null;

    const handleConfirm = async () => {
        setIsDeleting(true);
        await onConfirm();
        setIsDeleting(false);
        onClose();
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-small">
                <div className="modal-header">
                    <h3>Usuń trasę</h3>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isDeleting}
                        className="modal-close-btn">
                        &times;
                    </button>
                </div>

                <div className="modal-body modal-body-centered">
                    <AlertTriangle size={48} color="#dc2626" className="modal-icon-margin" />
                    <p className="modal-text-lead">
                        Czy na pewno chcesz usunąć trasę <br/><b>{tripName || 'wybraną trasę'}</b>?
                    </p>
                </div>

                <div className="modal-footer">
                    <button type="button" onClick={onClose} disabled={isDeleting} className="modal-btn btn-cancel">
                        Anuluj
                    </button>
                    <button
                        type="button"
                        onClick={handleConfirm}
                        disabled={isDeleting}
                        className="modal-btn btn-danger"
                    >
                        {isDeleting ? (
                            <span className="btn-loading-content">
                                <Loader2 size={16} className="anim-spin" /> Usuwanie...
                            </span>
                        ) : "Usuń"}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmDeleteModal;