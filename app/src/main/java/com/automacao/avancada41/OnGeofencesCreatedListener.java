package com.automacao.avancada41;

import java.util.List;

public interface OnGeofencesCreatedListener {
    void onGeofencesCreated(List<Fluxo> geofences);
    void onGeofenceError(String error);
}
