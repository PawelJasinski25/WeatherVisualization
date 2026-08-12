import React, { createContext, useState, useEffect, useContext } from 'react';

const UnitContext = createContext();

export const UnitProvider = ({ children }) => {
    const [units, setUnits] = useState(() => {
        const saved = localStorage.getItem('app_units');
        return saved ? JSON.parse(saved) : {
            timezone: 'UTC',
            wind: 'kt',
            currents: 'kt',
            speed: 'km/h',
            distance: 'km',
            temp: '°C',
            wave: 'm',
            pressure: 'hPa',
            rain: 'mm',
            snow: 'cm'
        };
        if (saved) {
            const parsed = JSON.parse(saved);
            return {...defaultUnits, ...parsed};
        }
    });

    useEffect(() => {
        localStorage.setItem('app_units', JSON.stringify(units));
    }, [units]);

    const updateUnit = (category, value) => {
        setUnits(prev => ({ ...prev, [category]: value }));
    };

    return (
        <UnitContext.Provider value={{ units, updateUnit }}>
            {children}
        </UnitContext.Provider>
    );
};



export const useUnits = () => useContext(UnitContext);

