import {
    tempColors, humidityColors, dewColors, pressureColors, rainColors, snowColors,
    cloudsColors, cloudsLowColors, cloudsMidColors, cloudsHighColors,
    windColors, gustsColors, waveColors, wavePeriodColors, seaTempColors, oceanCurrentColors
} from "./mapColors";

import { useUnits } from '../contexts/UnitContext';
import { formatWind, formatCurrents, formatTemp, formatWave, formatPressure, formatRain, formatSnow } from '../utils/unitConverter';

export const metricConfig = {
    astronomy: { label: "Pory dnia", getValue: pt => pt.dayPhase, palette: [ [0, [8, 12, 25]], [1, [26, 54, 150]], [2, [66, 170, 245]], [3, [245, 80, 180]], [4, [255, 220, 0]], [5, [245, 110, 0]], [6, [210, 30, 30]], [7, [110, 20, 160]], [8, [8, 12, 25]] ], labelCount: 9 },
    wind: { label: "Wiatr", palette: windColors, getValue: pt => pt.windSpeed !== null ? pt.windSpeed / 3.6 : null, labelCount: 7 },
    gusts: { label: "Porywy", palette: gustsColors, getValue: pt => pt.gusts !== null ? pt.gusts / 3.6 : null, labelCount: 7 },
    ocean_current_velocity: { label: "Prędkość prądów", palette: oceanCurrentColors, getValue: pt => pt.oceanCurrentVel !== null ? pt.oceanCurrentVel / 3.6 : null, labelCount: 6 },
    temp: { label: "Temperatura", palette: tempColors, getValue: pt => pt.temp !== null ? pt.temp + 273.15 : null, labelCount: 7 },
    dew: { label: "Punkt rosy", palette: dewColors, getValue: pt => pt.dewPoint !== null ? pt.dewPoint + 273.15 : null, labelCount: 7 },
    sea_temperature: { label: "Temperatura morza", palette: seaTempColors, getValue: pt => pt.seaTemp !== null ? pt.seaTemp + 273.15 : null, labelCount: 7 },
    wave_h: { label: "Fale", palette: waveColors, getValue: pt => pt.waveHeight, labelCount: 6 },
    wind_wave_h: { label: "Fale wiatr.", palette: waveColors, getValue: pt => pt.windWaveH, labelCount: 6 },
    swell_wave_h: { label: "Fale martwe", palette: waveColors, getValue: pt => pt.swellWaveH, labelCount: 6 },
    wave_p: { label: "Okres fal", palette: wavePeriodColors, getValue: pt => pt.wavePeriod, labelCount: 6 },
    wind_wave_p: { label: "Okres fal wiatr.", palette: wavePeriodColors, getValue: pt => pt.windWaveP, labelCount: 6 },
    swell_wave_p: { label: "Okres fal martwych", palette: wavePeriodColors, getValue: pt => pt.swellWaveP, labelCount: 6 },
    rain: { label: "Deszcz", palette: rainColors, getValue: pt => pt.rain, labelCount: 6 },
    snow: { label: "Śnieg", palette: snowColors, getValue: pt => pt.snowfall, labelCount: 6 },
    humidity: { label: "Wilgotność", palette: humidityColors, getValue: pt => pt.humidity, labelCount: 6 },
    clouds: { label: "Zachmurzenie", palette: cloudsColors, getValue: pt => pt.cloudCover, labelCount: 5 },
    clouds_low: { label: "Chmury niskie", palette: cloudsLowColors, getValue: pt => pt.cloudLow, labelCount: 5 },
    clouds_mid: { label: "Chmury średnie", palette: cloudsMidColors, getValue: pt => pt.cloudMid, labelCount: 5 },
    clouds_high: { label: "Chmury wysokie", palette: cloudsHighColors, getValue: pt => pt.cloudHigh, labelCount: 5 },
    pressure: { label: "Ciśnienie", palette: pressureColors, getValue: pt => pt.pressure !== null ? pt.pressure * 100 : null, labelCount: 5 },
};

export const useMetricConfig = () => {
    const { units } = useUnits();

    return {
        ...metricConfig,

        astronomy: {
            ...metricConfig.astronomy,
            unit: "",
            formatValue: val => {
                const labels = ["Noc", "Świt A.", "Świt N.", "Świt C.", "Dzień", "Zmierzch C.", "Zmierzch N.", "Zmierzch A.", "Noc"];
                return labels[Math.round(val)] || val;
            }
        },

        wind: { ...metricConfig.wind, unit: formatWind(0, units.wind).unit, formatValue: val => formatWind(val, units.wind).val },
        gusts: { ...metricConfig.gusts, unit: formatWind(0, units.wind).unit, formatValue: val => formatWind(val, units.wind).val },
        ocean_current_velocity: { ...metricConfig.ocean_current_velocity, unit: formatCurrents(0, units.currents).unit, formatValue: val => formatCurrents(val, units.currents).val },

        temp: { ...metricConfig.temp, unit: formatTemp(273.15, units.temp).unit, formatValue: val => formatTemp(val, units.temp).val },
        dew: { ...metricConfig.dew, unit: formatTemp(273.15, units.temp).unit, formatValue: val => formatTemp(val, units.temp).val },
        sea_temperature: { ...metricConfig.sea_temperature, unit: formatTemp(273.15, units.temp).unit, formatValue: val => formatTemp(val, units.temp).val },

        wave_h: { ...metricConfig.wave_h, unit: formatWave(0, units.wave).unit, formatValue: val => formatWave(val, units.wave).val },
        wind_wave_h: { ...metricConfig.wind_wave_h, unit: formatWave(0, units.wave).unit, formatValue: val => formatWave(val, units.wave).val },
        swell_wave_h: { ...metricConfig.swell_wave_h, unit: formatWave(0, units.wave).unit, formatValue: val => formatWave(val, units.wave).val },

        pressure: { ...metricConfig.pressure, unit: formatPressure(101325, units.pressure).unit, formatValue: val => formatPressure(val, units.pressure).val },
        rain: { ...metricConfig.rain, unit: formatRain(0, units.rain).unit, formatValue: val => formatRain(val, units.rain).val },
        snow: { ...metricConfig.snow, unit: formatSnow(0, units.snow).unit, formatValue: val => formatSnow(val, units.snow).val },

        wave_p: { ...metricConfig.wave_p, unit: "s", formatValue: val => Number(val).toFixed(1) },
        wind_wave_p: { ...metricConfig.wind_wave_p, unit: "s", formatValue: val => Number(val).toFixed(1) },
        swell_wave_p: { ...metricConfig.swell_wave_p, unit: "s", formatValue: val => Number(val).toFixed(1) },
        humidity: { ...metricConfig.humidity, unit: "%", formatValue: val => Number(val).toFixed(1) },
        clouds: { ...metricConfig.clouds, unit: "%", formatValue: val => Number(val).toFixed(1) },
        clouds_low: { ...metricConfig.clouds_low, unit: "%", formatValue: val => Number(val).toFixed(1) },
        clouds_mid: { ...metricConfig.clouds_mid, unit: "%", formatValue: val => Number(val).toFixed(1) },
        clouds_high: { ...metricConfig.clouds_high, unit: "%", formatValue: val => Number(val).toFixed(1) }
    };
};