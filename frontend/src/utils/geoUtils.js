
//zamienia na wspolrzedne w projekcji Mercatora
export const getProjectedCoords = (lat, lon) => {
    const toRad = Math.PI / 180;
    const x = lon * toRad;
    const y = Math.log(Math.tan(Math.PI / 4 + (lat * toRad) / 2));
    return { x, y };
};

//wraca do zwyklych wspolrzednych
export const unprojectMercator = (x, y) => {
    const toDeg = 180 / Math.PI;
    const lon = x * toDeg;
    const lat = (2 * Math.atan(Math.exp(y)) - Math.PI / 2) * toDeg;
    return { lat, lon };
};

//liczy dystans na mapie
export const getProjectedDistance = (lat1, lon1, lat2, lon2) => {
    const p1 = getProjectedCoords(lat1, lon1);
    const p2 = getProjectedCoords(lat2, lon2);
    return Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
};

//liczy dystans na ziemi (wzór Haversine)
export const getDistanceFromLatLonInKm = (lat1, lon1, lat2, lon2) => {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
};

//liczy kierunek miedzy punktami
export const getBearing = (lat1, lon1, lat2, lon2) => {
    const toRad = Math.PI / 180;
    const toDeg = 180 / Math.PI;
    const dLon = (lon2 - lon1) * toRad;
    const y = Math.sin(dLon) * Math.cos(lat2 * toRad);
    const x = Math.cos(lat1 * toRad) * Math.sin(lat2 * toRad) -
        Math.sin(lat1 * toRad) * Math.cos(lat2 * toRad) * Math.cos(dLon);
    let brng = Math.atan2(y, x) * toDeg;
    return (brng + 360) % 360;
};

export const interpolateColor = (value, palette) => {
    if (value === null || value === undefined) return "rgba(0,0,0,0)";

    if (value <= palette[0][0]) return `rgb(${palette[0][1].join(",")})`;
    if (value >= palette[palette.length - 1][0]) return `rgb(${palette[palette.length - 1][1].join(",")})`;

    for (let i = 0; i < palette.length - 1; i++) {
        if (value >= palette[i][0] && value <= palette[i + 1][0]) {
            const f = (value - palette[i][0]) / (palette[i + 1][0] - palette[i][0]);
            const r = Math.round(palette[i][1][0] + f * (palette[i + 1][1][0] - palette[i][1][0]));
            const g = Math.round(palette[i][1][1] + f * (palette[i + 1][1][1] - palette[i][1][1]));
            const b = Math.round(palette[i][1][2] + f * (palette[i + 1][1][2] - palette[i][1][2]));
            return `rgb(${r}, ${g}, ${b})`;
        }
    }
    return "rgba(0,0,0,0)";
};