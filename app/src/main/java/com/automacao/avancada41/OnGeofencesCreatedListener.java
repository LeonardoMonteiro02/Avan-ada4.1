package com.automacao.avancada41;

import java.util.List;

public interface OnGeofencesCreatedListener {
    void onGeofencesCreated(List<Planta> geofences);
    void onGeofenceError(String error);
}
