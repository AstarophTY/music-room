package com.musicroom.app.common

/** Asks the browser where the user is: [onFound] with the latitude and longitude, or [onFailed]. */
@JsFun(
    """(onFound, onFailed) => {
        if (!navigator.geolocation) { onFailed(); return; }
        navigator.geolocation.getCurrentPosition(
            (p) => onFound(p.coords.latitude, p.coords.longitude),
            () => onFailed(),
            { enableHighAccuracy: true, timeout: 15000, maximumAge: 60000 },
        );
    }""",
)
external fun requestBrowserPosition(onFound: (Double, Double) -> Unit, onFailed: () -> Unit)
