import React from 'react';
import { Popup } from "react-map-gl/maplibre";
import { getEventDotColor } from "../config/mapColors.js";

const AstroPopup = ({ marker, onClose }) => {
    if (!marker) return null;

    const [datePart, timePart] = marker.time.includes(', ')
        ? marker.time.split(', ')
        : ['', marker.time];

    return (
        <Popup
            longitude={marker.lng}
            latitude={marker.lat}
            anchor="bottom"
            offset={[marker.offsetX, -10]}
            onClose={onClose}
            closeOnClick={false}
            maxWidth="220px"
            className="custom-astro-popup"
        >
            <div className="astro-popup-accent" style={{ background: getEventDotColor(marker.type) }} />
            <div className="astro-popup-content">
                <div className="astro-type">{marker.type}</div>
                <div className="astro-time-main">{timePart}</div>
                <div className="astro-date-sub">{datePart}</div>
            </div>
        </Popup>
    );
};

export default AstroPopup;