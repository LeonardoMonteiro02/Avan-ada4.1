package com.automacao.avancada41;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Fluxo {
    // Atributos da classe
    private Geocerca geocerca;
    private List<Float> velocidades;
    private List<Long> temposMedidos;
    private Long tempoEsperado;
    private float velocidadeMedia;
    private Float distancia;
    private LatLng centro;
    private boolean atualizado;
    private static int nextId = 1;
    private int id;

    // Construtores
    public Fluxo() {
        this.velocidades = new ArrayList<>();
        this.temposMedidos = new ArrayList<>();
        this.id = nextId++;
    }

    public Fluxo(LatLng position, Float distancia, float velocidadeMedia) {
        this();
        this.geocerca = new Geocerca(position, 30);
        this.distancia = distancia;
        this.centro = position;
        this.velocidadeMedia = velocidadeMedia;
    }

    // Métodos estáticos
    public static void resetIdCounter() {
        nextId = 1;
    }

    public static void setNextIdCounter(int index) {
        nextId = index;
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public List<Float> getVelocidades() {
        return velocidades;
    }

    public void setVelocidade(Float velocidade) {
        this.velocidades.add(velocidade);
    }

    public Float getVelocidadeMedia() {
        return velocidadeMedia;
    }

    public void setVelocidadeMedia(float velocidadeMedia) {
        this.velocidadeMedia = velocidadeMedia;

    }

    public Float getDistancia() {
        return distancia;
    }

    public void setDistancia(Float distancia) {

        this.distancia = distancia;
    }

    public LatLng getCentro() {
        return centro;
    }

    public void setCentro(LatLng centro) {
        this.centro = centro;
    }

    public List<Long> getTemposMedidos() {
        return temposMedidos;
    }

    public Long getTempoEsperado() {
        return tempoEsperado;
    }

    public void setTempoEsperado(Long tempo) {
        this.tempoEsperado = tempo;
    }
    public void addTempoMedido(Long tempo) {
        this.temposMedidos.add(tempo);
    }

    // Métodos de funcionalidade
    public void adicionarGeocercaNoMapa(GoogleMap map) {
        geocerca.adicionarGeocercaNoMapa(map);
    }

    public void atualizarInformacoes(float velocidade, long tempoAtual, Context context) {
        if (!atualizado) {
            setVelocidade(velocidade);
            addTempoMedido(tempoAtual);
            setAtualizado(true);
            imprimirInformacoes(context);
        }
    }

    public boolean dentroDaGeocerca(LatLng ponto) {
        float[] results = new float[1];
        Location.distanceBetween(centro.latitude, centro.longitude, ponto.latitude, ponto.longitude, results);
        return results[0] <= geocerca.getRaio();
    }

    public void calcularTempoMedio() {
        if (distancia != null && velocidadeMedia != 0) {
            float tempo = distancia / (velocidadeMedia/3.6f);
            this.tempoEsperado = (long) tempo;
        } else {
            float tempo = 0;
            this.tempoEsperado = (long) tempo; // Ou um valor padrão
            // Trate a situação de erro conforme a lógica da sua aplicação
            System.out.println("Distância ou velocidade média não definida.");
        }
    }


    // Método para imprimir informações do fluxo
    public void imprimirInformacoes(Context context) {
        System.out.println("Fluxo ID: " + getId());
        System.out.println("Coordenadas: " + centro);
        System.out.println("Distância: " + (distancia != null ? distancia : "não definida") + " m");
        System.out.println("Velocidade Média: " + getVelocidadeMedia() + " Km/h");
        System.out.println("Tempo Esperado: " + getTempoEsperado() + " s");

        if (velocidades != null && !velocidades.isEmpty()) {
            StringBuilder sb = new StringBuilder("Simulação (total: ").append(velocidades.size()).append("):\n");
            for (int i = 0; i < velocidades.size(); i++) {
                Float vel = velocidades.get(i);
                sb.append("  Velocidade ").append(i + 1).append(": ").append(vel).append(" km/h\n");
            }
            System.out.println(sb);
        } else {
            System.out.println("Velocidade: não definida");
        }

        if (temposMedidos != null && !temposMedidos.isEmpty()) {
            StringBuilder temposInfo = new StringBuilder();
            for (int i = 0; i < temposMedidos.size(); i++) {
                Long tempo = temposMedidos.get(i);
                temposInfo.append("  Tempo ").append(i + 1).append(": ").append(tempo).append(" s\n");
            }
            System.out.println(temposInfo);
        } else {
            System.out.println("Tempos: não definidos");
        }

        // Envio de notificação (se aplicável)
        String notificationMessage = "Fluxo ID: " + getId() + "\n     Speed: " + velocidades;
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.sendNotification("Atualização de Localização", notificationMessage);
    }
}
