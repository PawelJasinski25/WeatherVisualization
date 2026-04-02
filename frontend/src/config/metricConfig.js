import {
    tempColors, humidityColors, dewColors, pressureColors, rainColors, snowColors,
    cloudsColors, cloudsLowColors, cloudsMidColors, cloudsHighColors,
    windColors, gustsColors, waveColors, wavePeriodColors, seaTempColors, oceanCurrentColors
} from "./mapColors";

export const metricConfig = {
    wind: { label: "Wiatr", unit: "km/h", palette: windColors, getValue: pt => pt.windSpeed, formatValue: val => Number(val).toFixed(1), labelCount: 7 },
    gusts: { label: "Porywy", unit: "km/h", palette: gustsColors, getValue: pt => pt.gusts, formatValue: val => Number(val).toFixed(1), labelCount: 7 },
    temp: { label: "Temperatura", unit: "°C", palette: tempColors, getValue: pt => pt.temp !== null ? pt.temp + 273.15 : null, formatValue: val => (val - 273.15).toFixed(1), labelCount: 7 },
    dew: { label: "Punkt rosy", unit: "°C", palette: dewColors, getValue: pt => pt.dewPoint !== null ? pt.dewPoint + 273.15 : null, formatValue: val => (val - 273.15).toFixed(1), labelCount: 7 },
    wave_h: { label: "Fale", unit: "m", palette: waveColors, getValue: pt => pt.waveHeight, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    wave_p: { label: "Okres fal", unit: "s", palette: wavePeriodColors, getValue: pt => pt.wavePeriod, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    rain: { label: "Deszcz", unit: "mm", palette: rainColors, getValue: pt => pt.rain, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    snow: { label: "Śnieg", unit: "cm", palette: snowColors, getValue: pt => pt.snowfall, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    humidity: { label: "Wilgotność", unit: "%", palette: humidityColors, getValue: pt => pt.humidity, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    pressure: { label: "Ciśnienie", unit: "hPa", palette: pressureColors, getValue: pt => pt.pressure !== null ? pt.pressure * 100 : null, formatValue: val => (val / 100).toFixed(1), labelCount: 5 },
    clouds: { label: "Zachmurzenie", unit: "%", palette: cloudsColors, getValue: pt => pt.cloudCover, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_low: { label: "Chmury niskie", unit: "%", palette: cloudsLowColors, getValue: pt => pt.cloudLow, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_mid: { label: "Chmury średnie", unit: "%", palette: cloudsMidColors, getValue: pt => pt.cloudMid, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    clouds_high: { label: "Chmury wysokie", unit: "%", palette: cloudsHighColors, getValue: pt => pt.cloudHigh, formatValue: val => Number(val).toFixed(1), labelCount: 5 },
    wind_wave_h: { label: "Fale wiatr.", unit: "m", palette: waveColors, getValue: pt => pt.windWaveH, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    wind_wave_p: { label: "Okres fal wiatr.", unit: "s", palette: wavePeriodColors, getValue: pt => pt.windWaveP, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    swell_wave_h: { label: "Fale martwe", unit: "m", palette: waveColors, getValue: pt => pt.swellWaveH, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    swell_wave_p: { label: "Okres fal martwych", unit: "s", palette: wavePeriodColors, getValue: pt => pt.swellWaveP, formatValue: val => Number(val).toFixed(1), labelCount: 6 },
    ocean_current_velocity: {
        label: "Prędkość prądów", unit: "m/s", palette: oceanCurrentColors,
        getValue: pt => pt.oceanCurrentVel !== null ? pt.oceanCurrentVel / 3.6 : null,
        formatValue: val => Number(val).toFixed(2), labelCount: 6
    },
    sea_temperature: { label: "Temperatura morza", unit: "°C", palette: seaTempColors, getValue: pt => pt.seaTemp !== null ? pt.seaTemp + 273.15 : null, formatValue: val => (val - 273.15).toFixed(1), labelCount: 7 },
};