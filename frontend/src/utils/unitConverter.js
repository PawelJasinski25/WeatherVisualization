export const formatWind = (valMs, pref) => {
    if (valMs === null || valMs === undefined) return { val: '--', unit: '' };

    if (pref === 'km/h') return { val: (valMs * 3.6).toFixed(1), unit: 'km/h' };
    if (pref === 'kt') return { val: (valMs * 1.94384).toFixed(1), unit: 'kt' };
    if (pref === 'mph') return { val: (valMs * 2.23694).toFixed(1), unit: 'mph' };

    if (pref === 'bft') {
        let bft = 0;
        if (valMs >= 0.3) bft = 1; if (valMs >= 1.6) bft = 2; if (valMs >= 3.4) bft = 3;
        if (valMs >= 5.5) bft = 4; if (valMs >= 8.0) bft = 5; if (valMs >= 10.8) bft = 6;
        if (valMs >= 13.9) bft = 7; if (valMs >= 17.2) bft = 8; if (valMs >= 20.8) bft = 9;
        if (valMs >= 24.5) bft = 10; if (valMs >= 28.5) bft = 11; if (valMs >= 32.7) bft = 12;
        return { val: bft.toString(), unit: 'bft' };
    }

    return { val: Number(valMs).toFixed(1), unit: 'm/s' };
};

export const formatCurrents = (valMs, pref) => {
    if (valMs === null || valMs === undefined) return { val: '--', unit: '' };
    if (pref === 'km/h') return { val: (valMs * 3.6).toFixed(1), unit: 'km/h' };
    if (pref === 'mph') return { val: (valMs * 2.23694).toFixed(1), unit: 'mph' };
    if (pref === 'kt') return { val: (valMs * 1.94384).toFixed(1), unit: 'kt' };
    return { val: Number(valMs).toFixed(2), unit: 'm/s' };
};

export const formatTemp = (valKelvin, pref) => {
    if (valKelvin === null || valKelvin === undefined) return { val: '--', unit: '' };
    const c = valKelvin - 273.15;
    if (pref === '°F') return { val: ((c * 9/5) + 32).toFixed(1), unit: '°F' };
    return { val: c.toFixed(1), unit: '°C' };
};

export const formatWave = (valM, pref) => {
    if (valM === null || valM === undefined) return { val: '--', unit: '' };
    if (pref === 'ft') return { val: (valM * 3.28084).toFixed(1), unit: 'ft' };
    return { val: Number(valM).toFixed(1), unit: 'm' };
};

export const formatPressure = (valPa, pref) => {
    if (valPa === null || valPa === undefined) return { val: '--', unit: '' };
    const hPa = valPa / 100;
    if (pref === 'inHg') return { val: (hPa * 0.02953).toFixed(2), unit: 'inHg' };
    if (pref === 'mmHg') return { val: (hPa * 0.75006).toFixed(1), unit: 'mmHg' };
    return { val: hPa.toFixed(1), unit: 'hPa' };
};

export const formatRain = (valMm, pref) => {
    if (valMm === null || valMm === undefined) return { val: '--', unit: '' };
    if (pref === 'inch') return { val: (valMm / 25.4).toFixed(2), unit: 'inch' };
    return { val: Number(valMm).toFixed(1), unit: 'mm' };
};

export const formatSnow = (valCm, pref) => {
    if (valCm === null || valCm === undefined) return { val: '--', unit: '' };
    if (pref === 'inch') return { val: (valCm / 2.54).toFixed(2), unit: 'ch' };
    if (pref === 'mm') return { val: (valCm * 10).toFixed(1), unit: 'mm' };
    return { val: Number(valCm).toFixed(1), unit: 'cm' };
};