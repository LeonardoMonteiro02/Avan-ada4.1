package com.automacao.avancada41;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

public class Fluxo {
    private Geocerca geocerca;  // Altere para usar a classe Geocerca
    private Float velocidade;
    private Float distancia;
    private Long tempo = 0L;  // Inicializa com um valor padrão
    private LatLng centro;
    private boolean atualizado;  // Novo campo
    private static int nextId = 1; // Gerador de IDs sequenciais
    private int id; // Identificador único

    // Construtor da classe Fluxo
    public Fluxo(LatLng position, Float distancia, Long tempo) {
        this.geocerca = new Geocerca(position, 30); // Crie a Geocerca com o raio padrão
        this.distancia = distancia;
        this.tempo = tempo;
        this.velocidade = null; // Inicialmente nulo
        this.centro = position;
        this.id = nextId++; // Atribui o próximo ID e incrementa
    }

    // Getters e setters

    public int getId() {
        return id;
    }
    public boolean isAtualizado() {
        return atualizado;
    }

    public void setAtualizado(boolean atualizado) {
        this.atualizado = atualizado;
    }
    public Geocerca getGeocerca() {
        return geocerca;
    }

    public void setGeocerca(Geocerca geocerca) {
        this.geocerca = geocerca;
    }

    public Float getVelocidade() {
        return velocidade;
    }

    public void setVelocidade(Float velocidade) {
        this.velocidade = velocidade;
    }

    public Float getDistancia() {
        return distancia;
    }

    public void setDistancia(Float distancia) {
        this.distancia = distancia;
    }

    public Long getTempo() {
        return tempo;
    }

    public void setTempo(Long tempo) {
        this.tempo = tempo;
    }

    // Método para adicionar a geocerca no mapa
    public void adicionarGeocercaNoMapa(GoogleMap map) {
        geocerca.adicionarGeocercaNoMapa(map);
    }

    // Atualiza a velocidade e o tempo
    public void atualizarInformacoes(float velocidade, long tempoAtual) {
        if (!atualizado) {
            setVelocidade(velocidade);

            Long tempo = getTempo();  // Linha problemática
            if (tempo == null) {
                tempo = 0L;  // Defina um valor padrão para nulo
            }

            if (tempo == 0) {
                setTempo(tempoAtual);
            } else {
                long tempoDentro = tempoAtual - tempo;
                setTempo(tempoDentro);
            }

            setAtualizado(true);
            imprimirInformacoes();
        }
    }

    public boolean dentroDaGeocerca(LatLng ponto) {
        // Método para verificar se o ponto está dentro da geocerca
        float[] results = new float[1];
        Location.distanceBetween(centro.latitude, centro.longitude, ponto.latitude, ponto.longitude, results);
        return results[0] <= geocerca.getRaio();
    }

    // Método para imprimir informações do fluxo
    public void imprimirInformacoes() {
        System.out.println("Fluxo ID: " + getId());
        System.out.println("Coordenadas: " + centro);
        System.out.println("Distância: " + (distancia != null ? distancia : "não definida"));
        System.out.println("Tempo: " + (tempo != null ? tempo + " ms" : "não definido"));
        System.out.println("Velocidade: " + (velocidade != null ? velocidade + " km/h" : "não definida"));
    }
}
