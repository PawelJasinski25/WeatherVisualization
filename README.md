# Pogoda na trasie
Aplikacja webowa zrealizowana w ramach pracy magisterskiej. Służy do wizualizacji i analizy warunków pogodowych na historycznych trasach zarejestrowanych w plikach GPX. Jej główne funkcje obejmują prezentację warunków meteorologicznych na mapie oraz przy użyciu animacji, a także generowanie raportów analitycznych zawierających podsumowanie wyprawy w formacie PDF.

## Uruchomienie aplikacji
### Wymagania wstępne
Upewnij się, że masz zainstalowane:
* [Docker Desktop](https://www.docker.com/products/docker-desktop/)
*  [Git](https://git-scm.com/downloads) (opcjonalnie, do pobrania kodu przez terminal)

### Kolejne kroki
1. **Pobierz kod źródłowy:**

   Opcja A: Z użyciem narzędzia Git (Zalecane)
   ```bash
   git clone https://github.com/PawelJasinski25/WeatherVisualization.git
   cd WeatherVisualization
   ```

   Opcja B: Bez użycia Git (Pobranie archiwum ZIP)
   1. Wejdź na stronę repozytorium: `https://github.com/PawelJasinski25/WeatherVisualization`
   2. Kliknij zielony przycisk **"<> Code"** w prawym górnym rogu, a następnie wybierz **"Download ZIP"**.
   3. Wypakuj pobrane archiwum w dogodnym miejscu na dysku.
   4. Otwórz terminal bezpośrednio wewnątrz wypakowanego folderu `WeatherVisualization`.

3. **Uruchom aplikację:**
   ```bash
   docker compose up --build
4. Aplikacja będzie znajdować się pod adresem : **http://localhost:3000**


