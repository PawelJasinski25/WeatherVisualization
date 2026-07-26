import React, { useState } from 'react';
import Navbar from '../components/Navbar.jsx';
import FileUploadModal from '../components/FileUploadModal.jsx';
import {GraduationCap, Cpu, CloudDownload} from 'lucide-react';
import "../styles/about.css";

const AboutPage = () => {
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    return (
        <div className="about-wrapper">
            <Navbar onOpenUpload={() => setIsUploadModalOpen(true)} activeTab="none" />

            <FileUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => setIsUploadModalOpen(false)}
            />

            <div className="about-container">
                <div className="about-header">
                    <h2 className="about-title">O aplikacji</h2>
                </div>

                <div className="about-scroll-area">
                    <div className="about-card">

                        <section className="about-section">
                            <div className="about-section-icon">
                                <GraduationCap className="about-icon-blue" />
                            </div>
                            <div className="about-section-content">
                                <p className="about-text">
                                    Aplikacja została zrealizowana w ramach pracy magisterskiej na kierunku informatyka stosowana na Wydziale Fizyki, Astronomii i Informatyki Stosowanej Uniwersytetu Jagiellońskiego w Krakowie.
                                    Służy do wizualizacji i analizy warunków pogodowych na trasach zarejestrowanych w plikach GPX.
                                </p>
                                <div className="about-meta-info">
                                    <div className="about-meta-item">
                                        <span><strong>Autor:</strong> Paweł Jasiński</span>
                                    </div>
                                    <div className="about-meta-item">
                                        <span><strong>Promotor:</strong> dr Iwona Grabska-Gradzińska</span>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <div className="about-divider"></div>

                        <section className="about-section">
                            <div className="about-section-icon">
                                <Cpu className="about-icon-purple" />
                            </div>
                            <div className="about-section-content">
                                <p className="about-text">
                                    Pobieranie danych meteorologicznych dla każdego punktu pliku GPX wiązałoby się z dużą liczbą nadmiarowych zapytań.
                                    Aby temu zapobiec, aplikacja w pierwszej kolejności agreguje punkty trasy w przestrzeni geograficznej.
                                    Szerokość i długość geograficzna są zaokrąglane do jednego miejsca po przecinku, co tworzy siatkę pomiarową o rozdzielczości około 11 km.
                                </p>
                                <p className="about-text" style={{ marginTop: '0.5rem' }}>
                                    Na podstawie daty oraz zaokrąglonych współrzędnych tworzony jest klucz pamięci podręcznej w formacie <code>YYYY-MM-DD_Lat_Lon</code>.
                                    Zapytania do zewnętrznego API wysyłane są wyłącznie dla unikalnych, brakujących w cache kluczy.
                                    Ponieważ odpowiedź serwisu zawiera dane dla całej doby (24 wartości godzinowe), aplikacja może wykorzystać jeden wpis w cache dla wielu punktów trasy oraz przypisać każdemu punktowi warunki pogodowe odpowiadające godzinie jego rejestracji.
                                    Dodatkowo, na podstawie pliku GeoJSON, system określa, czy punkt znajduje się na wodzie, aby zdecydować, czy należy pobrać dane morskie.
                                </p>
                            </div>
                        </section>

                        <div className="about-divider"></div>

                        <section className="about-section">
                            <div className="about-section-icon">
                                <CloudDownload className="about-icon-green" />
                            </div>
                            <div className="about-section-content">
                                <p className="about-text">
                                    Aplikacja wykorzystuje dane pozyskiwane z trzech zewnętrznych interfejsów API:
                                </p>
                                <ul className="about-list">
                                    <li>
                                        <span>
                                            <strong>
                                                <a
                                                    href="https://open-meteo.com/en/docs/historical-weather-api" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    Open-Meteo Historical Weather API
                                                </a>
                                            </strong>
                                            <br /> Pobieranie historycznych danych meteorologicznych.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://open-meteo.com/en/docs/marine-weather-api" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    Open-Meteo Marine Weather API
                                                </a>
                                            </strong>
                                            <br /> Pobieranie historycznych parametrów morskich.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://www.geonames.org/export/web-services.html" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    GeoNames API
                                                </a>
                                            </strong>
                                            <br /> Pobieranie nazw miejscowości, w których odbywał się postój na trasie.
                                        </span>
                                    </li>
                                </ul>
                            </div>
                        </section>

                    </div>
                </div>
            </div>
        </div>
    );
};

export default AboutPage;