import React, { useRef, useState, useEffect } from 'react';
import HoursCalculatorModal from "./HoursCalculatorModal.jsx";
import { Calculator, X } from 'lucide-react';

const CheckboxOption = ({ label, checked, onClick }) => {
    const lineStyle = {
        position: 'absolute',
        width: '14px',
        height: '1px',
        background: '#000',
        top: '4px',
        left: '-2px',
    };

    return (
        <span
            onClick={onClick}
            style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
        >
            <span style={{ width: '12px', height: '12px', border: '1px solid #000', position: 'relative', flexShrink: 0, boxSizing: 'border-box' }}>
                <span style={{ visibility: checked ? 'visible' : 'hidden' }}>
                    <span style={{ ...lineStyle, transform: 'rotate(45deg)' }} />
                    <span style={{ ...lineStyle, transform: 'rotate(-45deg)' }} />
                </span>
            </span>
            <span style={{ fontSize: '12px' }}>{label}</span>
        </span>
    );
};

const CruiseOpinionForm = ({ formData, opinion, handleOpinionChange, handleRemove, handleNestedChange, formatDateForPicker, formatDateFromPicker }) => {
    const portsRef = useRef(null);
    const remarksRef = useRef(null);
    const locationDateRef = useRef(null);
    const [isCalcOpen, setIsCalcOpen] = useState(false);

    useEffect(() => {
        if (portsRef.current) {
            portsRef.current.style.height = 'auto';
            portsRef.current.style.height = portsRef.current.scrollHeight + 'px';
        }
    }, [formData.cruise.visitedPorts]);

    useEffect(() => {
        if (remarksRef.current) {
            remarksRef.current.style.height = 'auto';
            remarksRef.current.style.height = remarksRef.current.scrollHeight + 'px';
        }
    }, [opinion.remarks]);

    useEffect(() => {
        if (locationDateRef.current) {
            locationDateRef.current.style.height = 'auto';
            locationDateRef.current.style.height = locationDateRef.current.scrollHeight + 'px';
        }
    }, [opinion.locationDate]);

    const handleSaveCalculatedHours = (calculatedData) => {
        handleNestedChange('hours', 'total', calculatedData.total);
        handleNestedChange('hours', 'sails', calculatedData.sails);
        handleNestedChange('hours', 'engine', calculatedData.engine);
        handleNestedChange('hours', 'tidal', calculatedData.tidal);
        handleNestedChange('hours', 'stopped', calculatedData.stopped);
        handleNestedChange('hours', 'dailyLogs', calculatedData.dailyLogs);
    };


    const toggleCheck = (field, value) => {
        handleOpinionChange(field, opinion[field] === value ? '' : value);
    };


    return (
        <div className="a4-paper mb-20">
            <HoursCalculatorModal
                isOpen={isCalcOpen}
                onClose={() => setIsCalcOpen(false)}
                initialLogs={formData.hours.dailyLogs || []}
                onSave={handleSaveCalculatedHours}
                cruiseDates={{ start: formData.cruise.embarkDate, end: formData.cruise.disembarkDate }}
                dailySummaries={formData.cruise.dailySummaries}
            />

            <div className="report-header mb-10" style={{ position: 'relative' }}>
                <button
                    onClick={handleRemove}
                    className="no-print"
                    style={{ position: 'absolute', top: 0, right: 0, background: 'none', border: 'none', cursor: 'pointer', color: '#ef4444' }}
                    title="Usuń opinię dla tej osoby"
                >
                    <X size={24} />
                </button>
                <input
                    className="interactive-input title-input"
                    value={opinion.title !== undefined ? opinion.title : "OPINIA Z REJSU"}
                    onChange={(e) => handleOpinionChange('title', e.target.value)}
                />
                <div className="summary-dates-container" style={{ justifyContent: 'center' }}>
                    <input
                        type="date"
                        className="interactive-input date-input inline-input"
                        value={formatDateForPicker(formData.cruise.startDate)}
                        onChange={(e) => {
                            const val = formatDateFromPicker(e.target.value);
                            handleNestedChange('cruise', 'startDate', val);
                            handleNestedChange('cruise', 'embarkDate', val);
                        }}
                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                    />
                    <span className="date-separator">-</span>
                    <input
                        type="date"
                        className="interactive-input date-input inline-input"
                        value={formatDateForPicker(formData.cruise.endDate)}
                        onChange={(e) => {
                            const val = formatDateFromPicker(e.target.value);
                            handleNestedChange('cruise', 'endDate', val);
                            handleNestedChange('cruise', 'disembarkDate', val);
                        }}
                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                    />
                </div>
            </div>


            <table className="form-table">
                <thead>
                <tr><th colSpan="4">INFORMACJE O UCZESTNIKU REJSU</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td colSpan="2" className="w-50">
                        <span className="label">Imię i nazwisko:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantName || ''}
                            onChange={(e) => handleOpinionChange('participantName', e.target.value)}
                        />
                    </td>
                    <td colSpan="2" className="w-50">
                        <span className="label">stop. żegl. /mot. i nr pat.:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantPatent || ''}
                            onChange={(e) => handleOpinionChange('participantPatent', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="2">
                        <span className="label">tel.:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantPhone || ''}
                            onChange={(e) => handleOpinionChange('participantPhone', e.target.value)}
                        />
                    </td>
                    <td className="w-25">
                        <span className="label">adres e-mail:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantEmail || ''}
                            onChange={(e) => handleOpinionChange('participantEmail', e.target.value)}
                        />
                    </td>
                    <td className="w-25">
                        <span className="label">funkcja na jachcie:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantFunction || ''}
                            onChange={(e) => handleOpinionChange('participantFunction', e.target.value)}
                        />
                    </td>
                </tr>
                </tbody>
            </table>


            <table className="form-table">
                <thead>
                <tr><th colSpan="4">INFORMACJE O JACHCIE</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td className="w-25">
                        <span className="label">Nr rej.:</span>
                        <input className="interactive-input" value={formData.yacht.regNumber} onChange={(e) => handleNestedChange('yacht', 'regNumber', e.target.value)} />
                    </td>
                    <td className="w-50" colSpan="2">
                        <span className="label">nazwa jachtu:</span>
                        <input className="interactive-input" value={formData.yacht.name} onChange={(e) => handleNestedChange('yacht', 'name', e.target.value)} />
                    </td>
                    <td className="w-25">
                        <span className="label">Lc [m]:</span>
                        <input className="interactive-input" value={formData.yacht.length} onChange={(e) => handleNestedChange('yacht', 'length', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td colSpan="3">
                        <span className="label">port macierzysty:</span>
                        <input className="interactive-input" value={formData.yacht.homePort} onChange={(e) => handleNestedChange('yacht', 'homePort', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">moc silnika [kW]:</span>
                        <input className="interactive-input" value={formData.yacht.enginePower} onChange={(e) => handleNestedChange('yacht', 'enginePower', e.target.value)} />
                    </td>
                </tr>
                </tbody>
            </table>


            <table className="form-table">
                <thead>
                <tr><th colSpan="4">INFORMACJE O REJSIE</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td colSpan="4">
                        <span className="label">Wpisu dokonano na podstawie dziennika jachtowego, nr pływania:</span>
                        <input className="interactive-input" value={formData.cruise.logbookNumber} onChange={(e) => handleNestedChange('cruise', 'logbookNumber', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td className="w-35">
                        <span className="label">Port zaokrętowania:</span>
                        <input className="interactive-input" value={formData.cruise.embarkPort} onChange={(e) => handleNestedChange('cruise', 'embarkPort', e.target.value)} />
                    </td>
                    <td className="w-25">
                        <span className="label">Data:</span>
                        <input
                            type="date"
                            className="interactive-input date-input"
                            value={formatDateForPicker(formData.cruise.embarkDate)}
                            onChange={(e) => handleNestedChange('cruise', 'embarkDate', formatDateFromPicker(e.target.value))}
                            onClick={(e) => e.target.showPicker && e.target.showPicker()}
                        />
                    </td>
                    <td className="w-40" colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input className="interactive-input" value={formData.cruise.embarkTidal} onChange={(e) => handleNestedChange('cruise', 'embarkTidal', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td>
                        <span className="label">Port wyokrętowania:</span>
                        <input className="interactive-input" value={formData.cruise.disembarkPort} onChange={(e) => handleNestedChange('cruise', 'disembarkPort', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">Data:</span>
                        <input
                            type="date"
                            className="interactive-input date-input"
                            value={formatDateForPicker(formData.cruise.disembarkDate)}
                            onChange={(e) => handleNestedChange('cruise', 'disembarkDate', formatDateFromPicker(e.target.value))}
                            onClick={(e) => e.target.showPicker && e.target.showPicker()}
                        />
                    </td>
                    <td colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input className="interactive-input" value={formData.cruise.disembarkTidal} onChange={(e) => handleNestedChange('cruise', 'disembarkTidal', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td colSpan="4">
                        <span className="label">Odwiedzone porty:</span>
                        <textarea
                            ref={portsRef}
                            className="interactive-textarea"
                            spellCheck="false"
                            value={formData.cruise.visitedPorts}
                            onChange={(e) => handleNestedChange('cruise', 'visitedPorts', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="2">
                        <span className="label">W tym liczba portów pływowych:</span>
                        <input className="interactive-input" value={formData.cruise.tidalPortsCount} onChange={(e) => handleNestedChange('cruise', 'tidalPortsCount', e.target.value)} />
                    </td>
                    <td colSpan="2">
                        <span className="label">Liczba dni rejsu:</span>
                        <input className="interactive-input" value={formData.cruise.daysCount} onChange={(e) => handleNestedChange('cruise', 'daysCount', e.target.value)} />
                    </td>
                </tr>
                </tbody>
            </table>


            <table className="form-table text-center">
                <thead>
                <tr>
                    <th colSpan="4" style={{ position: 'relative' }}>
                        GODZINY ŻEGLUGI
                        <button type="button" className="open-calc-btn no-print" onClick={() => setIsCalcOpen(true)}>
                            <Calculator size={14} /> Kalkulator
                        </button>
                    </th>
                    <th className="w-20">GODZINY POSTOJU</th>
                    <th className="w-20">PRZEBYTO MIL </th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td className="w-15">
                        <span className="label">razem (żagle+silnik)</span>
                        <input className="interactive-input center-input" value={formData.hours.total} onChange={(e) => handleNestedChange('hours', 'total', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">pod żaglami</span>
                        <input className="interactive-input center-input" value={formData.hours.sails} onChange={(e) => handleNestedChange('hours', 'sails', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">na silniku</span>
                        <input className="interactive-input center-input" value={formData.hours.engine} onChange={(e) => handleNestedChange('hours', 'engine', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">wody pływowe</span>
                        <input className="interactive-input center-input" value={formData.hours.tidal} onChange={(e) => handleNestedChange('hours', 'tidal', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">w portach / kotwica</span>
                        <input className="interactive-input center-input" value={formData.hours.stopped} onChange={(e) => handleNestedChange('hours', 'stopped', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">łącznie</span>
                        <input className="interactive-input center-input" value={formData.distance.nauticalMiles} onChange={(e) => handleNestedChange('distance', 'nauticalMiles', e.target.value)} />
                    </td>
                </tr>
                </tbody>
            </table>

            {/* OPINIA KAPITANA */}
            <table className="form-table">
                <thead>
                <tr>
                    <th style={{ padding: '4px 8px' }}>
                        <table style={{ width: '100%', border: 'none', margin: 0, background: 'transparent', tableLayout: 'fixed' }}>
                            <tbody>
                            <tr>
                                <td style={{ border: 'none', padding: 0, width: '25%', fontWeight: 'bold', textAlign: 'left' }}>OPINIA KAPITANA</td>
                                <td style={{ border: 'none', padding: 0, width: '25%', fontWeight: 'normal', textAlign: 'left' }}>
                                    <CheckboxOption checked={opinion.general === 'pozytywna'} label="pozytywna" onClick={() => toggleCheck('general', 'pozytywna')} />
                                </td>
                                <td style={{ border: 'none', padding: 0, width: '50%', fontWeight: 'normal', textAlign: 'left' }} colSpan={2}>
                                    <CheckboxOption checked={opinion.general === 'negatywna'} label="negatywna" onClick={() => toggleCheck('general', 'negatywna')} />
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td style={{ padding: '6px 8px' }}>
                        <span className="label" style={{ fontWeight: 'bold', color: '#000', fontSize: '10px' }}>Z obowiązków wywiązywał/a się:</span>
                        <table style={{ width: '100%', border: 'none', margin: '4px 0 10px 0', tableLayout: 'fixed' }}>
                            <tbody>
                            <tr>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.duties === 'bardzo dobrze'} label="bardzo dobrze" onClick={() => toggleCheck('duties', 'bardzo dobrze')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.duties === 'dobrze'} label="dobrze" onClick={() => toggleCheck('duties', 'dobrze')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.duties === 'dostatecznie'} label="dostatecznie" onClick={() => toggleCheck('duties', 'dostatecznie')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.duties === 'niedostatecznie'} label="niedostatecznie" onClick={() => toggleCheck('duties', 'niedostatecznie')} /></td>
                            </tr>
                            </tbody>
                        </table>

                        <span className="label" style={{ fontWeight: 'bold', color: '#000', fontSize: '10px' }}>Chorobie morskiej:</span>
                        <table style={{ width: '100%', border: 'none', margin: '4px 0 10px 0', tableLayout: 'fixed' }}>
                            <tbody>
                            <tr>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.seasickness === 'nie podlegał/a'} label="nie podlegał/a" onClick={() => toggleCheck('seasickness', 'nie podlegał/a')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.seasickness === 'chorował/a ciężko'} label="chorował/a ciężko" onClick={() => toggleCheck('seasickness', 'chorował/a ciężko')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '50%' }} colSpan={2}><CheckboxOption checked={opinion.seasickness === 'chorował/a lecz mógł/mogła pracować'} label="chorował/a lecz mógł/mogła pracować" onClick={() => toggleCheck('seasickness', 'chorował/a lecz mógł/mogła pracować')} /></td>
                            </tr>
                            </tbody>
                        </table>

                        <span className="label" style={{ fontWeight: 'bold', color: '#000', fontSize: '10px' }}>Odporność w trudnych warunkach:</span>
                        <table style={{ width: '100%', border: 'none', margin: '4px 0 4px 0', tableLayout: 'fixed' }}>
                            <tbody>
                            <tr>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.endurance === 'dobra'} label="dobra" onClick={() => toggleCheck('endurance', 'dobra')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.endurance === 'dostateczna'} label="dostateczna" onClick={() => toggleCheck('endurance', 'dostateczna')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.endurance === 'niedostateczna'} label="niedostateczna" onClick={() => toggleCheck('endurance', 'niedostateczna')} /></td>
                                <td style={{ border: 'none', padding: 0, width: '25%' }}><CheckboxOption checked={opinion.endurance === 'nie sprawdzano'} label="nie sprawdzano" onClick={() => toggleCheck('endurance', 'nie sprawdzano')} /></td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                </tbody>
            </table>

            {/* UWAGI KAPITANA */}
            <table className="form-table">
                <thead>
                <tr><th style={{ textAlign: 'left', paddingLeft: '8px' }}>UWAGI KAPITANA</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td>
                            <textarea
                                ref={remarksRef}
                                className="interactive-textarea"
                                spellCheck="false"
                                value={opinion.remarks || ''}
                                onChange={(e) => handleOpinionChange('remarks', e.target.value)}
                                style={{ minHeight: '40px' }}
                            />
                    </td>
                </tr>
                </tbody>
            </table>


            <table className="form-table" style={{ marginBottom: '8px' }}>
                <thead>
                <tr><th colSpan="4">INFORMACJE O KAPITANIE</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td colSpan="2" className="w-50">
                        <span className="label">Imię i nazwisko:</span>
                        <input className="interactive-input" value={formData.captain.name} onChange={(e) => handleNestedChange('captain', 'name', e.target.value)} />
                    </td>
                    <td colSpan="2" className="w-50">
                        <span className="label">stop. żegl./mot. i nr pat.:</span>
                        <input className="interactive-input" value={formData.captain.patent} onChange={(e) => handleNestedChange('captain', 'patent', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td className="w-18" style={{ verticalAlign: 'top' }}>
                        <span className="label">tel.:</span>
                        <input className="interactive-input" value={formData.captain.phone} onChange={(e) => handleNestedChange('captain', 'phone', e.target.value)} />
                    </td>
                    <td className="w-25" style={{ verticalAlign: 'top' }}>
                        <span className="label">adres e-mail:</span>
                        <input className="interactive-input" value={formData.captain.email} onChange={(e) => handleNestedChange('captain', 'email', e.target.value)} />
                    </td>
                    <td className="w-25" style={{ verticalAlign: 'top' }}>
                        <span className="label">miejscowość, data:</span>
                        <textarea
                            ref={locationDateRef}
                            className="interactive-textarea"
                            spellCheck="false"
                            rows="1"
                            value={opinion.locationDate || ''}
                            onChange={(e) => handleOpinionChange('locationDate', e.target.value)}
                            style={{ resize: 'none', overflow: 'hidden' }}
                        />
                    </td>
                    <td className="w-25" style={{ verticalAlign: 'top' }}>
                        <span className="label">podpis kapitana:</span>
                        <div style={{ borderBottom: '1px dotted #000', width: '95%', margin: '20px auto 4px auto' }}></div>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    );
};

export default CruiseOpinionForm;