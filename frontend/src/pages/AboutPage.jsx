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
                                    Służy do wizualizacji i analizy warunków pogodowych na trasach zarejestrowanych w plikach GPX. Kod źródłowy aplikacji udostępniany jest na licencji <strong>GNU GPLv3</strong>.
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
                                    Współrzędne geograficzne są zaokrąglane do jednego miejsca po przecinku, co tworzy siatkę pomiarową o wysokości około 11,1 km i szerokości zależnej od położenia na kuli ziemskiej (około 6,8 km na terytorium Polski).
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
                                    Aplikacja korzysta z następujących zasobów i narzędzi:
                                </p>
                                <ul className="about-list">
                                    <li>
                                        <span>
                                            <strong>
                                                <a
                                                    href="https://open-meteo.com/en/docs/historical-weather-api" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    Open-Meteo Historical Weather API (CC BY 4.0)
                                                </a>
                                            </strong>
                                            <br /> Pobieranie historycznych danych meteorologicznych.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://open-meteo.com/en/docs/marine-weather-api" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    Open-Meteo Marine Weather API (CC BY 4.0)
                                                </a>
                                            </strong>
                                            <br /> Pobieranie historycznych parametrów morskich.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://www.geonames.org/export/web-services.html" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    GeoNames API (CC BY 4.0)
                                                </a>
                                            </strong>
                                            <br /> Pobieranie nazw miejscowości, w których odbywał się postój na trasie.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    OpenStreetMap (ODbL)
                                                </a>
                                            </strong>
                                            <br /> Dostarczanie podkładu mapowego.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://maplibre.org/" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    MapLibre GL JS (BSD-3-Clause License)
                                                </a>
                                            </strong>
                                            <br /> Wyświetlanie i obsługa mapy.
                                        </span>
                                    </li>
                                    <li>
                                        <span>
                                            <strong>
                                                <a href="https://www.flaticon.com" target="_blank" rel="noopener noreferrer" className="about-link">
                                                    Flaticon (Flaticon License)
                                                </a>
                                            </strong>
                                            <br /> Ikony wykorzystywane w panelu parametrów i piktogramach na mapie.<br />
                                            <i>
                                                Autorzy: <a href="https://www.flaticon.com/authors/freepik" target="_blank" rel="noreferrer" className="about-link">Freepik</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/sonnycandra" target="_blank" rel="noreferrer" className="about-link">sonnycandra</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/iconading" target="_blank" rel="noreferrer" className="about-link">iconading</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/bqlqn" target="_blank" rel="noreferrer" className="about-link">bqlqn</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/adriansyah" target="_blank" rel="noreferrer" className="about-link">adriansyah</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/lutfix" target="_blank" rel="noreferrer" className="about-link">lutfix</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/ayub-irawan" target="_blank" rel="noreferrer" className="about-link">Ayub Irawan</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/rsetiawan" target="_blank" rel="noreferrer" className="about-link">rsetiawan</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/whocon" target="_blank" rel="noreferrer" className="about-link">WhoCon</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/lafs" target="_blank" rel="noreferrer" className="about-link">LAFS</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/culmbio" target="_blank" rel="noreferrer" className="about-link">Culmbio</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/pixel-perfect" target="_blank" rel="noreferrer" className="about-link">Pixel perfect</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/sudowoodo" target="_blank" rel="noreferrer" className="about-link">Sudowoodo</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/slidicon" target="_blank" rel="noreferrer" className="about-link">Slidicon</a>,{' '}
                                                <a href="https://www.flaticon.com/authors/good-ware" target="_blank" rel="noreferrer" className="about-link">Good Ware</a>{' '}
                                            </i>
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