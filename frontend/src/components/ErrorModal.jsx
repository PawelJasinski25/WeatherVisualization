import React from 'react';
import { AlertCircle } from 'lucide-react';
import "../styles/modal.css";

const ErrorModal = ({ isOpen, onClose, errorMessage }) => {
    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-small">
                <div className="modal-header">
                    <h3>Wystąpił błąd</h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="modal-close-btn">
                        &times;
                    </button>
                </div>

                <div className="modal-body modal-body-centered">
                    <AlertCircle size={48} color="#dc2626" className="modal-icon-margin" />
                    <p className="modal-text-lead">
                        {errorMessage}
                    </p>
                </div>

                <div className="modal-footer">
                    <button
                        type="button"
                        onClick={onClose}
                        className="modal-btn btn-cancel btn-block"
                    >
                        Zamknij
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ErrorModal;