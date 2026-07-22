package jasinski.pawel.weather_visualization.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;

@Service
public class GeoNamesService {

    static final String USERNAME = "paweljasinski";

    //promień 5km
    static final String PLACE_API_URL = "http://api.geonames.org/findNearbyPlaceNameJSON?lat={lat}&lng={lng}&radius=5&username={username}";

    static final String OCEAN_API_URL = "http://api.geonames.org/oceanJSON?lat={lat}&lng={lng}&username={username}";

    private final RestTemplate restTemplate;

    public GeoNamesService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    record GeoName(String name, String countryName) {}
    record GeoNamesResponse(List<GeoName> geonames) {}

    record Ocean(String name) {}
    record OceanResponse(Ocean ocean) {}

    @Cacheable(
            value = "placeNames",
            key = "T(java.lang.Math).round(#lat * 1000.0) + '_' + T(java.lang.Math).round(#lng * 1000.0)",
            sync = true
    )
    public String getPlaceName(double lat, double lng) {

        // Zaokrąglenie do 3 miejsc po przecinku (~100m)
        lat = Math.round(lat * 1000.0) / 1000.0;
        lng = Math.round(lng * 1000.0) / 1000.0;

        try {
            // Szukamy miasta w promieniu max 5 km
            GeoNamesResponse placeResp = restTemplate.getForObject(PLACE_API_URL, GeoNamesResponse.class, lat, lng, USERNAME);

            if (placeResp != null && placeResp.geonames() != null && !placeResp.geonames().isEmpty()) {
                GeoName place = placeResp.geonames().get(0);
                return place.name() + " (" + place.countryName() + ")";
            }

            // Szukamy nazwy oceanu/morza
            OceanResponse oceanResp = restTemplate.getForObject(OCEAN_API_URL, OceanResponse.class, lat, lng, USERNAME);

            if (oceanResp != null && oceanResp.ocean() != null && oceanResp.ocean().name() != null) {
                return oceanResp.ocean().name();
            }

        } catch (Exception e) {
            System.err.println("Błąd GeoNames dla współrzędnych " + lat + ", " + lng + ": " + e.getMessage());
        }

        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }
}