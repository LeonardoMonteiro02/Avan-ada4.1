package com.automacao.avancada41;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import android.graphics.Color;

public class Geocerca {

    private LatLng centro;
    private float raio;
    private int corContorno;
    private int corPreenchimento;

    // Construtor
    public Geocerca(LatLng centro, float raio) {
        this.centro = centro;
        this.raio = raio;
        this.corContorno = Color.MAGENTA; // Cor padrão para o contorno
        this.corPreenchimento = Color.argb(64, 128, 0, 128); // Cor padrão roxa com transparência
    }

    // Getters e Setters
    public LatLng getCentro() {
        return centro;
    }

    public void setCentro(LatLng centro) {
        this.centro = centro;
    }

    public float getRaio() {
        return raio;
    }

    public void setRaio(float raio) {
        this.raio = raio;
    }

    public int getCorContorno() {
        return corContorno;
    }

    public void setCorContorno(int corContorno) {
        this.corContorno = corContorno;
    }

    public int getCorPreenchimento() {
        return corPreenchimento;
    }

    public void setCorPreenchimento(int corPreenchimento) {
        this.corPreenchimento = corPreenchimento;
    }

    // Método para criar uma geocerca no mapa
    public CircleOptions criarGeocerca() {
        return new CircleOptions()
                .center(centro)
                .radius(raio)
                .strokeColor(corContorno)
                .fillColor(corPreenchimento);
    }

    // Método para adicionar a geocerca no mapa
    public void adicionarGeocercaNoMapa(GoogleMap map) {
        map.addCircle(criarGeocerca());
    }
}
