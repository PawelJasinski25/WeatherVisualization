import React from 'react';
import { Map, LineChart, Clock } from 'lucide-react';

const PlaceholderWeatherTable = () => (
    <table className="report-table weather-table">
        <thead>
        <tr>
            <th className="w-40">Parametr</th>
            <th>Średnia</th>
            <th>W ruchu</th>
            <th>Maks.</th>
        </tr>
        </thead>
        <tbody>
        <tr className="weather-header-row">
            <th colSpan="4">TEMPERATURA/POWIETRZE</th>
        </tr>
        <tr>
            <td>Temperatura</td>
            <td><b>11,5°C</b></td>
            <td><b>12,1°C</b></td>
            <td><b>23,6°C</b></td>
        </tr>
        <tr>
            <td>Punkt rosy</td>
            <td><b>7,5°C</b></td>
            <td><b>7,8°C</b></td>
            <td><b>18,0°C</b></td>
        </tr>
        <tr>
            <td>Wilgotność</td>
            <td><b>77%</b></td>
            <td><b>75%</b></td>
            <td><b>97%</b></td>
        </tr>
        <tr>
            <td>Ciśnienie</td>
            <td><b>1008,0 hPa</b></td>
            <td><b>1009,2 hPa</b></td>
            <td><b>1024,8 hPa</b></td>
        </tr>

        <tr className="weather-header-row">
            <th colSpan="4">WIATR</th>
        </tr>
        <tr>
            <td>Siła wiatru</td>
            <td><b>24,0 km/h</b></td>
            <td><b>26,5 km/h</b></td>
            <td><b>47,6 km/h</b></td>
        </tr>
        <tr>
            <td>Porywy wiatru</td>
            <td><b>44,5 km/h</b></td>
            <td><b>48,2 km/h</b></td>
            <td><b>72,0 km/h</b></td>
        </tr>

        <tr className="weather-header-row">
            <th colSpan="4">OPADY / CHMURY</th>
        </tr>
        <tr>
            <td>Deszcz (suma)</td>
            <td><b>3,1 mm</b></td>
            <td><b>0,0 mm</b></td>
            <td>--</td>
        </tr>
        <tr>
            <td>Śnieg (suma)</td>
            <td><b>2,0 cm</b></td>
            <td><b>0,0 cm</b></td>
            <td>--</td>
        </tr>
        <tr>
            <td>Zachmurzenie</td>
            <td><b>65%</b></td>
            <td><b>60%</b></td>
            <td><b>100%</b></td>
        </tr>

        <tr className="weather-header-row">
            <th colSpan="4">MORZE</th>
        </tr>
        <tr>
            <td>Temperatura morza</td>
            <td><b>13,4°C</b></td>
            <td><b>13,5°C</b></td>
            <td><b>22,5°C</b></td>
        </tr>
        <tr>
            <td>Fale (wys. | okr.)</td>
            <td><b>1,3 m | 6,6 s</b></td>
            <td><b>1,5 m | 6,8 s</b></td>
            <td><b>2,8 m | 12,4 s</b></td>
        </tr>
        <tr>
            <td>Fale martwe (wys. | okr.)</td>
            <td><b>0,8 m | 6,2 s</b></td>
            <td><b>0,9 m | 6,5 s</b></td>
            <td><b>2,5 m | 11,7 s</b></td>
        </tr>
        <tr>
            <td>Prądy </td>
            <td><b>0,6 m/s</b></td>
            <td><b>0,7 m/s</b></td>
            <td><b>2,1 m/s</b></td>
        </tr>
        </tbody>
    </table>
);

const DummyChart = ({ icon, label, height = 'auto', flex = 'none' }) => (
    <div
        className="dummy-chart"
        style={{ height, flex }}
    >
        <span className="dummy-chart-icon">{icon}</span>
        <span className="dummy-chart-label">{label}</span>
    </div>
);

const ReportPreview = ({ formData, handleFieldChange, handleNestedChange, formatDateForPicker, formatDateFromPicker }) => {
    return (
        <>
            {/* STRONA 2: PODSUMOWANIE TRASY */}
            <div className="a4-paper">
                <div className="report-header summary-header-wrapper">
                    <input
                        className="interactive-input summary-title-input"
                        value={formData?.summaryTitle || ''}
                        onChange={(e) => handleFieldChange('summaryTitle', e.target.value)}
                        placeholder="Podsumowanie trasy"
                    />
                    <div className="summary-dates-container">
                        <input
                            type="date"
                            className="interactive-input date-input inline-input"
                            value={formatDateForPicker(formData?.cruise?.startDate)}
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
                            value={formatDateForPicker(formData?.cruise?.endDate)}
                            onChange={(e) => {
                                const val = formatDateFromPicker(e.target.value);
                                handleNestedChange('cruise', 'endDate', val);
                                handleNestedChange('cruise', 'disembarkDate', val);
                            }}
                            onClick={(e) => e.target.showPicker && e.target.showPicker()}
                        />
                    </div>
                </div>

                <p className="summary-mock-info">Podgląd raportu (dane przykładowe)</p>

                <div className="report-flex-container">
                    {/* LEWA KOLUMNA (STATYSTYKI) */}
                    <div className="report-col w-70">
                        <div className="report-mock-section mb-0">
                            <h3 className="section-header">Sumaryczny czas trasy</h3>
                            <table className="report-table">
                                <thead>
                                <tr>
                                    <th>W RUCHU</th>
                                    <th>POSTOJE</th>
                                    <th>ŁĄCZNIE</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr>
                                    <td><b>108:49:00</b></td>
                                    <td><b>150:12:15</b></td>
                                    <td><b>259:01:15</b></td>
                                </tr>
                                </tbody>
                            </table>
                        </div>

                        <div className="report-mock-section report-flex-1">
                            <h3 className="section-header">Statystyki pogodowe</h3>
                            <PlaceholderWeatherTable />
                        </div>
                    </div>

                    {/* PRAWA KOLUMNA (RÓŻE I MAPA) */}
                    <div className="report-col w-50">
                        <div className="report-mock-section report-flex-container mb-0 p-10">
                            <div className="w-50">
                                <DummyChart icon={<LineChart size={40} strokeWidth={1.5} />} label="Róża Wiatrów" height="200px" />
                            </div>
                            <div className="w-50">
                                <DummyChart icon={<LineChart size={40} strokeWidth={1.5} />} label="Róża Falowania" height="200px" />
                            </div>
                        </div>

                        <div className="report-mock-section report-flex-1">
                            <h3 className="section-header">Mapa trasy</h3>
                            <DummyChart icon={<Map size={48} strokeWidth={1.5} />} label="Mapa" flex="1" />
                        </div>
                    </div>
                </div>

                <div className="report-mock-section mt-10">
                    <h3 className="section-header">Meteogram</h3>
                    <DummyChart icon={<LineChart size={48} strokeWidth={1.5} />} label="Wykres złożony" height="220px" />
                </div>
            </div>

            {/* STRONA 3: DZIEŃ 1 */}
            <div className="a4-paper mt-20">
                <div className="report-header">
                    <h2 className="mb-0">Dzień 1 (03.01.2025)</h2>
                </div>

                <div className="report-mock-section mb-10">
                    <h3 className="section-header">Osie czasu (Ruch i Astro)</h3>
                    <DummyChart icon={<Clock size={32} strokeWidth={1.5} />} label="Osie czasu dla ruchu i postojów oraz dla zjawisk astronomicznych" height="80px" />
                </div>

                <div className="report-flex-container">
                    <div className="report-col w-70">
                        <div className="report-mock-section report-flex-1">
                            <h3 className="section-header">Statystyki pogodowe</h3>
                            <PlaceholderWeatherTable />
                        </div>
                    </div>

                    <div className="report-col w-50">
                        <div className="report-mock-section report-flex-container mb-0 p-10">
                            <div className="w-50">
                                <DummyChart icon={<LineChart size={40} strokeWidth={1.5} />} label="Róża Wiatrów" height="200px" />
                            </div>
                            <div className="w-50">
                                <DummyChart icon={<LineChart size={40} strokeWidth={1.5} />} label="Róża Falowania" height="200px" />
                            </div>
                        </div>

                        <div className="report-mock-section report-flex-1">
                            <h3 className="section-header">Mapa trasy</h3>
                            <DummyChart icon={<Map size={48} strokeWidth={1.5} />} label="Mapa" flex="1" />
                        </div>
                    </div>
                </div>

                <div className="report-mock-section mt-10">
                    <h3 className="section-header">Meteogram</h3>
                    <DummyChart icon={<LineChart size={48} strokeWidth={1.5} />} label="Wykres złożony (Dzień 1)" height="220px" />
                </div>
            </div>
        </>
    );
};

export default ReportPreview;