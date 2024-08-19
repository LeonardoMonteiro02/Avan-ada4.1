
package com.automacao.avancada41;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class RouteCalculator {

    private static final String TAG = RouteCalculator.class.getSimpleName();

    private GoogleMap map;
    private LatLng startLatLng;
    private LatLng destinationLatLng;
    private Context context;
    private OnGeofencesCreatedListener listener;
    private  List<Planta> listaPlantabanco = new ArrayList<>();
    private int indexRota = 0;
    private int geofenceCount = 8;



    public RouteCalculator(GoogleMap map, LatLng startLatLng, LatLng destinationLatLng, Context context, OnGeofencesCreatedListener listener, FirebaseDataSaver firebaseDataSaver) {
        this.map = map;
        this.startLatLng = startLatLng;
        this.destinationLatLng = destinationLatLng;
        this.context = context;
        this.listener = listener;


    }



    public void calculateRoute() {
        AsyncTask<Void, Integer, Boolean> task = new AsyncTask<Void, Integer, Boolean>() {
            private static final String TOAST_ERR_MSG = "Unable to calculate route";

            private final List<ArrayList<LatLng>> routes = new ArrayList<>();
            private final List<Float> routeDistances = new ArrayList<>();
            private final List<Long> routeTimes = new ArrayList<>();

            @Override
            protected Boolean doInBackground(Void... params) {
                HttpURLConnection urlConnection = null;
                try {
                    String apiKey = "AIzaSyBCyVwhUeBZrcFLX8-PqsjYzvYMaVQvS_4";
                    String urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                            startLatLng.latitude + "," + startLatLng.longitude +
                            "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                            "&alternatives=true" +
                            "&key=" + apiKey;

                    URL url = new URL(urlStr);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    String response = new Scanner(inputStream).useDelimiter("\\A").next();

                    JSONObject jsonResponse = new JSONObject(response);
                    String status = jsonResponse.getString("status");
                    if (!"OK".equals(status)) {
                        return false;
                    }

                    JSONArray routesArray = jsonResponse.getJSONArray("routes");
                    for (int r = 0; r < routesArray.length(); r++) {
                        JSONObject route = routesArray.getJSONObject(r);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPoints = overviewPolyline.getString("points");

                        ArrayList<LatLng> lstLatLng = new ArrayList<>();
                        decodePolylines(encodedPoints, lstLatLng);
                        routes.add(lstLatLng);

                        JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                        String distanceText = leg.getJSONObject("distance").getString("text");
                        String durationText = leg.getJSONObject("duration").getString("text");

                        routeDistances.add(getDistanceFromText(distanceText)); // Convert to km
                        routeTimes.add(getDurationFromText(durationText)); // Convert to MIN
                    }

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating route", e);
                    return false;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            public void recuperarDados(FirebaseDataSaver firebaseDataSaver) {
                firebaseDataSaver.retrieveData(new OnDataRetrievedListener() {
                    @Override
                    public void onDataRetrieved(List<Planta> plantas) {
                        if (plantas != null) {

                            listaPlantabanco = plantas;
                            if(listaPlantabanco.size() == geofenceCount || listaPlantabanco.size() == 0){
                                addGeofences(routes.get(indexRota-1));
                            }

                        } else {
                            Log.e("MainActivity", "Erro ao recuperar os dados ou lista vazia.");

                        }
                    }
                },0,indexRota);


            }
            private float getDistanceFromText(String text) {
                String numericText = text.replaceAll("[^\\d.]", "");
                return Float.parseFloat(numericText);
            }

            private long getDurationFromText(String text) {
                long totalMinutes = 0;
                String[] parts = text.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].endsWith("hour") || parts[i].endsWith("hours")) {
                        totalMinutes += Long.parseLong(parts[i - 1]) * 60;
                    } else if (parts[i].endsWith("min") || parts[i].endsWith("mins")) {
                        totalMinutes += Long.parseLong(parts[i - 1]);
                    }
                }
                return totalMinutes;
            }

            private void decodePolylines(String encodedPoints, ArrayList<LatLng> lstLatLng) {
                int index = 0;
                int lat = 0, lng = 0;

                while (index < encodedPoints.length()) {
                    int b, shift = 0, result = 0;
                    do {
                        b = encodedPoints.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);

                    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += dlat;
                    shift = 0;
                    result = 0;

                    do {
                        b = encodedPoints.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);

                    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lng += dlng;

                    lstLatLng.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
                }
            }


            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) {
                    Toast.makeText(context, TOAST_ERR_MSG, Toast.LENGTH_SHORT).show();
                } else {
                    int[] colors = {Color.BLUE, Color.GREEN, Color.RED};

                    String[] routeOptions = new String[routes.size()];
                    for (int i = 0; i < routes.size(); i++) {
                        String distanceText = String.format(Locale.getDefault(), "%.2f km", routeDistances.get(i));
                        String durationText = String.format(Locale.getDefault(), "%d min", routeTimes.get(i));
                        routeOptions[i] = "Rota " + (i + 1) + ": " + distanceText + ", Tempo: " + durationText;
                    }

                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Escolha uma rota")
                            .setItems(routeOptions, (dialog, which) -> {
                                indexRota = which + 1;
                                map.clear();  // Limpa o mapa antes de desenhar a nova rota

                                // Desenha a rota selecionada
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.color(colors[which % colors.length]);
                                for (LatLng latLng : routes.get(which)) {
                                    polylineOptions.add(latLng);
                                }
                                map.addPolyline(polylineOptions);

                                // Ajusta a câmera para mostrar a rota completa
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng latLng : routes.get(which)) {
                                    builder.include(latLng);
                                }
                                builder.include(startLatLng);
                                builder.include(destinationLatLng);
                                LatLngBounds bounds = builder.build();
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

                                String distanceMessage = String.format(Locale.getDefault(), "Distância total: %.2f km", routeDistances.get(which));
                                String timeMessage = String.format(Locale.getDefault(), "Tempo total: %d minutos", routeTimes.get(which));
                                String combinedMessage = distanceMessage + "\n" + timeMessage;

                                Toast.makeText(context, combinedMessage, Toast.LENGTH_SHORT).show();
                                recuperarDados(new FirebaseDataSaver());

                            })
                            .show();
                }
            }

            // Método para adicionar uma geocerca e um marcador com rótulo no mapa
            // Método para adicionar uma geocerca e um marcador com rótulo no mapa
            private void addGeofences(List<LatLng> route) {
                Planta.resetIdCounter();
                List<Planta> plantas = new ArrayList<>();
                float totalDistance = 0;
                LatLng prevLatLng = route.get(0);

                // Primeiro, calcula a distância total ao longo da rota
                for (int i = 1; i < route.size(); i++) {
                    LatLng currentLatLng = route.get(i);
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(
                            prevLatLng.latitude, prevLatLng.longitude,
                            currentLatLng.latitude, currentLatLng.longitude,
                            results);
                    totalDistance += results[0];
                    prevLatLng = currentLatLng;
                }

                float interval = totalDistance / (geofenceCount - 1);
                prevLatLng = route.get(0);
                float accumulatedDistance = 0;
                float pointDistance = 0;
                float nextGeofenceDistance = interval;



                if ((listaPlantabanco != null && !listaPlantabanco.isEmpty()) &&
                        (checkDistances(listaPlantabanco.get(0).getCentro(), listaPlantabanco.get(listaPlantabanco.size() - 1).getCentro())) &&
                        compactibilidade(listaPlantabanco)) {

                    // Adiciona geocerca para o ponto de início
                    plantas = listaPlantabanco;
                    Planta plantaInicio = plantas.get(0);
                    LatLng pontoInicio = plantaInicio.getGeocerca().getCentro();
                    map.addMarker(new MarkerOptions()
                            .position(pontoInicio)
                            .title("Início")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    plantaInicio.adicionarGeocercaNoMapa(map);
                    // Adiciona geocercas e marcadores para os pontos intermediários
                    for (int i = 1; i < plantas.size()-1; i++) {
                        Planta planta = plantas.get(i);
                        LatLng pontoAtual = planta.getGeocerca().getCentro();

                        // Adiciona a geocerca no mapa
                        planta.adicionarGeocercaNoMapa(map);

                        // Adiciona um marcador com rótulo no mapa
                        map.addMarker(new MarkerOptions()
                                .position(pontoAtual)
                                .title("Geocerca " + (i + 1))  // Nome da geocerca
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));  // Cor do marcador
                    }

                    // Adiciona geocerca para o ponto final
                    Planta plantaFim = plantas.get(plantas.size() - 1);
                    LatLng pontoFinal = plantaFim.getGeocerca().getCentro();
                    map.addMarker(new MarkerOptions()
                            .position(pontoFinal)
                            .title("Fim")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    plantaFim.adicionarGeocercaNoMapa(map);

                }
                else {
                    // Caso contrário, cria as geocercas normalmente
                    // Adiciona geocerca para o ponto de partida
                    if (indexPlanta() != 0) {
                        Planta.setNextIdCounter(indexPlanta());
                        Planta plantaInicio = new Planta(route.get(0), 0f,0f);
                        plantaInicio.calcularTempoMedio();
                        plantaInicio.imprimirInformacoes(context);
                        plantas.add(plantaInicio);
                        plantaInicio.adicionarGeocercaNoMapa(map);
                        map.addMarker(new MarkerOptions()
                                .position(route.get(0))
                                .title("Início")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                        for (int i = 1; i < route.size(); i++) {
                            LatLng currentLatLng = route.get(i);
                            float[] results = new float[1];
                            android.location.Location.distanceBetween(
                                    prevLatLng.latitude, prevLatLng.longitude,
                                    currentLatLng.latitude, currentLatLng.longitude,
                                    results);

                            accumulatedDistance += results[0];

                            if (accumulatedDistance >= nextGeofenceDistance && plantas.size() < geofenceCount - 1) {

                               float velocidadeMedia =((routeDistances.get(indexRota-1)*1000)/(routeTimes.get(indexRota-1)*60))*3.6f;
                                Planta planta = new Planta(currentLatLng, accumulatedDistance - pointDistance, velocidadeMedia);
                                planta.calcularTempoMedio();
                                planta.imprimirInformacoes(context);
                                plantas.add(planta);
                                pointDistance = accumulatedDistance;

                                // Adiciona o círculo da geocerca no mapa
                                planta.adicionarGeocercaNoMapa(map);

                                // Adiciona um marcador com rótulo no mapa
                                map.addMarker(new MarkerOptions()
                                        .position(currentLatLng)
                                        .title("Geocerca " + (plantas.size()))  // Nome da geocerca
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));  // Cor do marcador

                                nextGeofenceDistance += interval;
                            }

                            prevLatLng = currentLatLng;
                        }

                        // Adiciona geocerca para o ponto final
                       float velocidadeMedia = ((routeDistances.get(indexRota-1)*1000)/(routeTimes.get(indexRota-1)*60))*3.6f;
                        LatLng finalLatLng = route.get(route.size() - 1);
                        Planta plantaFim = new Planta(finalLatLng, accumulatedDistance - pointDistance,velocidadeMedia);  // Adiciona a distância final acumulada
                        plantaFim.calcularTempoMedio();
                        plantaFim.imprimirInformacoes(context);
                        plantas.add(plantaFim);
                        plantaFim.adicionarGeocercaNoMapa(map);
                        map.addMarker(new MarkerOptions()
                                .position(finalLatLng)
                                .title("Fim")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    }
                }

                if (listener != null) {
                    listener.onGeofencesCreated(plantas);
                } else {
                    Log.d("Geofence", "Listener é null.");
                }
            }


            // Função para calcular a distância entre dois pontos
            private double calculateDistance(LatLng pointA, LatLng pointB) {
                final double R = 6371000.0; // Raio da Terra em metros
                double lat1 = pointA.latitude;
                double lon1 = pointA.longitude;
                double lat2 = pointB.latitude;
                double lon2 = pointB.longitude;

                double dLat = Math.toRadians(lat2 - lat1);
                double dLon = Math.toRadians(lon2 - lon1);
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return R * c;
            }

            // Função para verificar as distâncias
            public Boolean checkDistances(LatLng partida, LatLng destino) {
                LatLng point1 = startLatLng;
                LatLng point2 = destinationLatLng;
                // Calcula as distâncias necessárias
                double distancePartidaPoint1 = calculateDistance(partida, point1);
                double distanceDestinoPoint2 = calculateDistance(destino, point2);


                // Verifica se ambas as distâncias são menores ou iguais a 5 metros
                return distancePartidaPoint1 <= 25 && distanceDestinoPoint2 <= 25;
            }
            public  Boolean compactibilidade (List <Planta> plantas) {
                if (indexRota == 1 & plantas.get(0).getId() == 1) {
                    return true;
                } else if (indexRota == 2 & plantas.get(0).getId() == 9) {
                    return true;
                } else if (indexRota == 3 & plantas.get(0).getId() == 17) {
                    return true;
                } else {
                    return false;
                }
            }
            public int indexPlanta( ) {
                if (indexRota < 1) {
                    return 0; // Para indexRota inválido
                }

                int a1 = 1; // Primeiro termo da progressão
                int d = geofenceCount;  // Diferença comum

                return a1 + (indexRota - 1) * d;
            }

        };
        task.execute();
    }
}
