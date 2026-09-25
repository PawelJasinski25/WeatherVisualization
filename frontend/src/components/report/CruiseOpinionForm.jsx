import React, { useRef, useState, useEffect } from 'react';
import HoursCalculatorModal from "./HoursCalculatorModal.jsx";
import { Calculator, X, ImagePlus } from 'lucide-react';

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

const CruiseOpinionForm = ({ formData, opinion, handleOpinionChange, handleRemove, handleNestedChange, formatDateForPicker, formatDateFromPicker, handleLogoUpload,handleLogoRemove }) => {
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

    const handleLocalNestedChange = (section, field, value) => {
        let processedValue = value;
        const numericFields = ['total', 'sails', 'engine', 'tidal', 'stopped', 'nauticalMiles'];
        if (numericFields.includes(field) && typeof value === 'string') {
            processedValue = value.replace(/\./g, ',').replace(/[^\d,]/g, '');
            const parts = processedValue.split(',');
            if (parts.length > 2) {
                processedValue = parts[0] + ',' + parts.slice(1).join('');
            }
        }
        handleOpinionChange(section, { ...opinion[section], [field]: processedValue });
    };

    const handleSaveCalculatedHours = (calculatedData) => {
        handleOpinionChange('hours', {
            ...opinion.hours,
            total: calculatedData.total,
            sails: calculatedData.sails,
            engine: calculatedData.engine,
            tidal: calculatedData.tidal,
            stopped: calculatedData.stopped,
            gap: calculatedData.gap,
            exactSeconds: calculatedData.exactSeconds,
            dailyLogs: calculatedData.dailyLogs
        });
    };


    const toggleCheck = (field, value) => {
        handleOpinionChange(field, opinion[field] === value ? '' : value);
    };


    return (
        <div className="a4-paper mb-20" style={{ position: 'relative' }}>
            <HoursCalculatorModal
                isOpen={isCalcOpen}
                onClose={() => setIsCalcOpen(false)}
                initialLogs={opinion?.hours?.dailyLogs || []}
                onSave={handleSaveCalculatedHours}
                cruiseDates={{ start: opinion?.cruise?.embarkDate, end: opinion?.cruise?.disembarkDate }}
                dailySummaries={opinion?.cruise?.dailySummaries}
            />

            <button
                onClick={handleRemove}
                className="remove-opinion-btn"
                title="Usuń opinię dla tej osoby"
            >
                <X size={26} />
            </button>

            <div className="report-header mb-10" style={{ position: 'relative' }}>
                <div className="logo-upload-box" style={{ right: 0, top: '-5px' }} title="Wgraj logo">
                    {formData.logoBase64 ? (
                        <>
                            <img src={formData.logoBase64} alt="Logo" />
                            <button type="button" className="remove-logo-btn no-print" onClick={handleLogoRemove} title="Usuń logo">
                                <X size={14} />
                            </button>
                        </>
                    ) : (
                        <label className="logo-upload-label">
                            <input type="file" accept="image/png, image/jpeg" style={{ display: 'none' }} onChange={handleLogoUpload} />
                            <ImagePlus size={40} strokeWidth={1.5} className="no-print" />
                        </label>
                    )}
                </div>

                <input
                    className="interactive-input title-input"
                    value={opinion.title !== undefined ? opinion.title : "OPINIA Z REJSU"}
                    onChange={(e) => handleOpinionChange('title', e.target.value)}
                />
                <div className="summary-dates-container" style={{ justifyContent: 'center', alignItems: 'center' }}>
                    {formData.tripName && (
                        <span style={{ marginRight: '6px' }}>
                            {formData.tripName},
                        </span>
                    )}
                    <input
                        type="date"
                        className="interactive-input date-input inline-input"
                        value={formatDateForPicker(opinion.cruise.startDate)}
                        onChange={(e) => {
                            const val = formatDateFromPicker(e.target.value);
                            handleOpinionChange('cruise', { ...opinion.cruise, startDate: val, embarkDate: val });
                        }}
                        onClick={(e) => e.target.showPicker && e.target.showPicker()}
                    />
                    <span>-</span>
                    <input
                        type="date"
                        className="interactive-input date-input inline-input"
                        value={formatDateForPicker(opinion.cruise.endDate)}
                        onChange={(e) => {
                            const val = formatDateFromPicker(e.target.value);
                            handleOpinionChange('cruise', { ...opinion.cruise, endDate: val, disembarkDate: val });
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
                    <td colSpan="2" className="w-40">
                        <span className="label">Imię i nazwisko:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantName || ''}
                            onChange={(e) => handleOpinionChange('participantName', e.target.value)}
                        />
                    </td>
                    <td colSpan="2" className="w-60">
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
                    <td className="w-33">
                        <span className="label">adres e-mail:</span>
                        <input
                            className="interactive-input"
                            value={opinion.participantEmail || ''}
                            onChange={(e) => handleOpinionChange('participantEmail', e.target.value)}
                        />
                    </td>
                    <td className="w-21">
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
                        <input className="interactive-input" value={opinion.cruise.logbookNumber} onChange={(e) => handleLocalNestedChange('cruise', 'logbookNumber', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td className="w-35">
                        <span className="label">Port zaokrętowania:</span>
                        <input className="interactive-input" value={opinion.cruise.embarkPort} onChange={(e) => handleLocalNestedChange('cruise', 'embarkPort', e.target.value)} />
                    </td>
                    <td className="w-25">
                        <span className="label">Data:</span>
                        <input
                            type="date"
                            className="interactive-input date-input"
                            value={formatDateForPicker(opinion.cruise.embarkDate)}
                            onChange={(e) => {
                                const val = formatDateFromPicker(e.target.value);
                                handleOpinionChange('cruise', { ...opinion.cruise, embarkDate: val, startDate: val });
                            }}
                            onClick={(e) => e.target.showPicker && e.target.showPicker()}
                        />
                    </td>
                    <td className="w-40" colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input className="interactive-input" value={opinion.cruise.embarkTidal} onChange={(e) => handleLocalNestedChange('cruise', 'embarkTidal', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td>
                        <span className="label">Port wyokrętowania:</span>
                        <input className="interactive-input" value={opinion.cruise.disembarkPort} onChange={(e) => handleLocalNestedChange('cruise', 'disembarkPort', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">Data:</span>
                        <input
                            type="date"
                            className="interactive-input date-input"
                            value={formatDateForPicker(opinion.cruise.disembarkDate)}
                            onChange={(e) => {
                                const val = formatDateFromPicker(e.target.value);
                                handleOpinionChange('cruise', { ...opinion.cruise, disembarkDate: val, endDate: val });
                            }}
                            onClick={(e) => e.target.showPicker && e.target.showPicker()}
                        />
                    </td>
                    <td colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input className="interactive-input" value={opinion.cruise.disembarkTidal} onChange={(e) => handleLocalNestedChange('cruise', 'disembarkTidal', e.target.value)} />
                    </td>
                </tr>
                <tr>
                    <td colSpan="4">
                        <span className="label">Odwiedzone porty:</span>
                        <textarea
                            ref={portsRef}
                            className="interactive-textarea"
                            spellCheck="false"
                            value={opinion.cruise.visitedPorts}
                            onChange={(e) => handleLocalNestedChange('cruise', 'visitedPorts', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="2">
                        <span className="label">W tym liczba portów pływowych:</span>
                        <input className="interactive-input" value={opinion.cruise.tidalPortsCount} onChange={(e) => handleLocalNestedChange('cruise', 'tidalPortsCount', e.target.value)} />
                    </td>
                    <td colSpan="2">
                        <span className="label">Liczba dni rejsu:</span>
                        <input className="interactive-input" value={opinion.cruise.daysCount} onChange={(e) => handleLocalNestedChange('cruise', 'daysCount', e.target.value)} />
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
                        <input className="interactive-input center-input" value={opinion.hours.total} onChange={(e) => handleLocalNestedChange('hours', 'total', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">pod żaglami</span>
                        <input className="interactive-input center-input" value={opinion.hours.sails} onChange={(e) => handleLocalNestedChange('hours', 'sails', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">na silniku</span>
                        <input className="interactive-input center-input" value={opinion.hours.engine} onChange={(e) => handleLocalNestedChange('hours', 'engine', e.target.value)} />
                    </td>
                    <td className="w-15">
                        <span className="label">wody pływowe</span>
                        <input className="interactive-input center-input" value={opinion.hours.tidal} onChange={(e) => handleLocalNestedChange('hours', 'tidal', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">w portach / kotwica</span>
                        <input className="interactive-input center-input" value={opinion.hours.stopped} onChange={(e) => handleLocalNestedChange('hours', 'stopped', e.target.value)} />
                    </td>
                    <td>
                        <span className="label">łącznie</span>
                        <input className="interactive-input center-input" value={opinion.distance.nauticalMiles} onChange={(e) => handleLocalNestedChange('distance', 'nauticalMiles', e.target.value)} />
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
                <tr><th style={{ textAlign: 'center' }}>UWAGI KAPITANA</th></tr>
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
                                style={{ minHeight: '60px' }}
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
                    <td className="w-15" style={{ verticalAlign: 'top' }}>
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