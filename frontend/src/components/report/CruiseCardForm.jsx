import React from 'react';

const CruiseCardForm = ({ formData, handleFieldChange, handleNestedChange, handleCrewChange }) => {
    return (
        <div className="a4-paper mb-20">

            {/* NAGŁÓWEK */}
            <div className="report-header mb-10">
                <input
                    className="interactive-input title-input"
                    value={formData.tripName}
                    onChange={(e) => handleFieldChange('tripName', e.target.value)}
                    placeholder="Nazwa rejsu..."
                />
                <div className="cruise-dates-container">
                    <input
                        className="interactive-input inline-input"
                        value={formData.cruise.startDate}
                        onChange={(e) => handleNestedChange('cruise', 'startDate', e.target.value)}
                        placeholder="Od"
                    />
                    <span> — </span>
                    <input
                        className="interactive-input inline-input"
                        value={formData.cruise.endDate}
                        onChange={(e) => handleNestedChange('cruise', 'endDate', e.target.value)}
                        placeholder="Do"
                    />
                </div>
            </div>

            {/* KAPITAN */}
            <table className="form-table">
                <thead>
                <tr>
                    <th colSpan="4">INFORMACJE O KAPITANIE</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colSpan="2" className="w-50">
                        <span className="label">Imię i nazwisko:</span>
                        <input
                            className="interactive-input"
                            value={formData.captain.name}
                            onChange={(e) => handleNestedChange('captain', 'name', e.target.value)}
                        />
                    </td>
                    <td colSpan="2" className="w-50">
                        <span className="label">stop. żegl./mot. i nr pat.:</span>
                        <input
                            className="interactive-input"
                            value={formData.captain.patent}
                            onChange={(e) => handleNestedChange('captain', 'patent', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="2">
                        <span className="label">tel.:</span>
                        <input
                            className="interactive-input"
                            value={formData.captain.phone}
                            onChange={(e) => handleNestedChange('captain', 'phone', e.target.value)}
                        />
                    </td>
                    <td colSpan="2">
                        <span className="label">adres e-mail:</span>
                        <input
                            className="interactive-input"
                            value={formData.captain.email}
                            onChange={(e) => handleNestedChange('captain', 'email', e.target.value)}
                        />
                    </td>
                </tr>
                </tbody>
            </table>

            {/* JACHT */}
            <table className="form-table">
                <thead>
                <tr>
                    <th colSpan="4">INFORMACJE O JACHCIE</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td className="w-25">
                        <span className="label">Nr rej.:</span>
                        <input
                            className="interactive-input"
                            value={formData.yacht.regNumber}
                            onChange={(e) => handleNestedChange('yacht', 'regNumber', e.target.value)}
                        />
                    </td>
                    <td className="w-50" colSpan="2">
                        <span className="label">nazwa jachtu:</span>
                        <input
                            className="interactive-input"
                            value={formData.yacht.name}
                            onChange={(e) => handleNestedChange('yacht', 'name', e.target.value)}
                        />
                    </td>
                    <td className="w-25">
                        <span className="label">Lc [m]:</span>
                        <input
                            className="interactive-input"
                            value={formData.yacht.length}
                            onChange={(e) => handleNestedChange('yacht', 'length', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="3">
                        <span className="label">port macierzysty:</span>
                        <input
                            className="interactive-input"
                            value={formData.yacht.homePort}
                            onChange={(e) => handleNestedChange('yacht', 'homePort', e.target.value)}
                        />
                    </td>
                    <td>
                        <span className="label">moc silnika [kW]:</span>
                        <input
                            className="interactive-input"
                            value={formData.yacht.enginePower}
                            onChange={(e) => handleNestedChange('yacht', 'enginePower', e.target.value)}
                        />
                    </td>
                </tr>
                </tbody>
            </table>

            {/* REJS */}
            <table className="form-table">
                <thead>
                <tr>
                    <th colSpan="4">INFORMACJE O REJSIE</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colSpan="4">
                        <span className="label">Wpisu dokonano na podstawie dziennika jachtowego*, nr pływania:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.logbookNumber}
                            onChange={(e) => handleNestedChange('cruise', 'logbookNumber', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td className="w-35">
                        <span className="label">Port zaokrętowania:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.embarkPort}
                            onChange={(e) => handleNestedChange('cruise', 'embarkPort', e.target.value)}
                        />
                    </td>
                    <td className="w-25">
                        <span className="label">Data:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.embarkDate}
                            onChange={(e) => handleNestedChange('cruise', 'embarkDate', e.target.value)}
                        />
                    </td>
                    <td className="w-40" colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.embarkTidal}
                            onChange={(e) => handleNestedChange('cruise', 'embarkTidal', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td>
                        <span className="label">Port wyokrętowania:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.disembarkPort}
                            onChange={(e) => handleNestedChange('cruise', 'disembarkPort', e.target.value)}
                        />
                    </td>
                    <td>
                        <span className="label">Data:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.disembarkDate}
                            onChange={(e) => handleNestedChange('cruise', 'disembarkDate', e.target.value)}
                        />
                    </td>
                    <td colSpan="2">
                        <span className="label">Pływowy:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.disembarkTidal}
                            onChange={(e) => handleNestedChange('cruise', 'disembarkTidal', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="4">
                        <span className="label">Odwiedzone porty:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.visitedPorts}
                            onChange={(e) => handleNestedChange('cruise', 'visitedPorts', e.target.value)}
                        />
                    </td>
                </tr>
                <tr>
                    <td colSpan="2">
                        <span className="label">W tym liczba portów pływowych:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.tidalPortsCount}
                            onChange={(e) => handleNestedChange('cruise', 'tidalPortsCount', e.target.value)}
                        />
                    </td>
                    <td colSpan="2">
                        <span className="label">Liczba dni rejsu:</span>
                        <input
                            className="interactive-input"
                            value={formData.cruise.daysCount}
                            onChange={(e) => handleNestedChange('cruise', 'daysCount', e.target.value)}
                        />
                    </td>
                </tr>
                </tbody>
            </table>

            {/* GODZINY */}
            <table className="form-table text-center">
                <thead>
                <tr>
                    <th colSpan="4">GODZINY ŻEGLUGI</th>
                    <th className="w-20">GODZINY POSTOJU</th>
                    <th className="w-20">PRZEBYTO MIL</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td className="w-15">
                        <span className="label">razem (żagle+silnik)</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.hours.total}
                            onChange={(e) => handleNestedChange('hours', 'total', e.target.value)}
                        />
                    </td>
                    <td className="w-15">
                        <span className="label">pod żaglami</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.hours.sails}
                            onChange={(e) => handleNestedChange('hours', 'sails', e.target.value)}
                        />
                    </td>
                    <td className="w-15">
                        <span className="label">na silniku</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.hours.engine}
                            onChange={(e) => handleNestedChange('hours', 'engine', e.target.value)}
                        />
                    </td>
                    <td className="w-15">
                        <span className="label">wody pływowe</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.hours.tidal}
                            onChange={(e) => handleNestedChange('hours', 'tidal', e.target.value)}
                        />
                    </td>
                    <td>
                        <span className="label">w portach / kotwica</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.hours.stopped}
                            onChange={(e) => handleNestedChange('hours', 'stopped', e.target.value)}
                        />
                    </td>
                    <td>
                        <span className="label">łącznie</span>
                        <input
                            className="interactive-input center-input"
                            value={formData.distance.nauticalMiles}
                            onChange={(e) => handleNestedChange('distance', 'nauticalMiles', e.target.value)}
                        />
                    </td>
                </tr>
                </tbody>
            </table>

            {/* ZAŁOGA */}
            <table className="form-table">
                <thead>
                <tr>
                    <th colSpan="8">INFORMACJE O ZAŁODZE</th>
                </tr>
                </thead>
                <tbody>
                <tr className="text-center font-xs bg-light-gray font-bold">
                    <td className="w-4">Lp.</td>
                    <td className="w-21">Imię i nazwisko</td>
                    <td className="w-12">stopień żegl./mot.</td>
                    <td className="w-13 border-r-strong">funkcja na jachcie</td>
                    <td className="w-4">Lp.</td>
                    <td className="w-21">Imię i nazwisko</td>
                    <td className="w-12">stopień żegl./mot.</td>
                    <td className="w-13">funkcja na jachcie</td>
                </tr>
                {Array.from({ length: 8 }).map((_, i) => {
                    const lIdx = i ;
                    const rIdx = i +8;
                    return (
                        <tr key={i}>
                            <td className="text-center">{lIdx + 1}</td>
                            <td>
                                <input
                                    className="interactive-input"
                                    value={formData.crew[lIdx].name}
                                    onChange={(e) => handleCrewChange(lIdx, 'name', e.target.value)}
                                />
                            </td>
                            <td>
                                <input
                                    className="interactive-input"
                                    value={formData.crew[lIdx].patent}
                                    onChange={(e) => handleCrewChange(lIdx, 'patent', e.target.value)}
                                />
                            </td>
                            <td className="border-r-strong">
                                <input
                                    className="interactive-input"
                                    value={formData.crew[lIdx].function}
                                    onChange={(e) => handleCrewChange(lIdx, 'function', e.target.value)}
                                />
                            </td>

                            <td className="text-center">{rIdx + 1}</td>
                            <td>
                                <input
                                    className="interactive-input"
                                    value={formData.crew[rIdx].name}
                                    onChange={(e) => handleCrewChange(rIdx, 'name', e.target.value)}
                                />
                            </td>
                            <td>
                                <input
                                    className="interactive-input"
                                    value={formData.crew[rIdx].patent}
                                    onChange={(e) => handleCrewChange(rIdx, 'patent', e.target.value)}
                                />
                            </td>
                            <td>
                                <input
                                    className="interactive-input"
                                    value={formData.crew[rIdx].function}
                                    onChange={(e) => handleCrewChange(rIdx, 'function', e.target.value)}
                                />
                            </td>
                        </tr>
                    );
                })}
                </tbody>
            </table>
        </div>
    );
};

export default CruiseCardForm;