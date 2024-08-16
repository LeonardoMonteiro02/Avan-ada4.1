package com.automacao.avancada41;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class EstatisticasTempos {
    private List<Fluxo> fluxos;

    // Construtor
    public EstatisticasTempos(List<Fluxo> fluxos) {
        this.fluxos = fluxos;
    }

    public EstatisticasTempos() {
        // Construtor padrão
    }

    public void setFluxos(List<Fluxo> fluxos) {
        this.fluxos = fluxos;
    }

    // Método para calcular a média
    private double calcularMedia(List<Long> tempos) {
        if (tempos == null || tempos.isEmpty()) {
            throw new IllegalArgumentException("A lista de tempos está vazia ou é nula.");
        }
        return tempos.stream().mapToLong(Long::longValue).average().orElse(Double.NaN);
    }

    // Método para calcular o desvio padrão
    private double calcularDesvioPadrao(List<Long> tempos) {
        double media = calcularMedia(tempos);
        double variancia = tempos.stream()
                .mapToDouble(t -> Math.pow(t - media, 2))
                .average()
                .orElse(Double.NaN);
        return Math.sqrt(variancia);
    }

    // Método para calcular a polarização (bias)
    private double calcularPolarizacao(List<Long> tempos, double tempoEsperado) {
        double media = calcularMedia(tempos);
        return media - tempoEsperado;
    }

    // Método para calcular a precisão (2 vezes o desvio padrão)
    private double calcularPrecisao(List<Long> tempos) {
        double desvioPadrao = calcularDesvioPadrao(tempos);
        return desvioPadrao * 2;
    }

    // Método para calcular a incerteza (combinando polarização e precisão)
    private double calcularIncerteza(List<Long> tempos, double tempoEsperado) {
        double polarizacao = calcularPolarizacao(tempos, tempoEsperado);
        double precisao = calcularPrecisao(tempos);
        return Math.sqrt(Math.pow(polarizacao, 2) + Math.pow(precisao, 2));
    }

    // Método para gerar o relatório completo para todos os fluxos
    public String gerarRelatorio() {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatório Geral de Estatísticas dos Fluxos:\n");
        relatorio.append("-------------------------------------------\n");

        for (int i = 0; i < fluxos.size(); i++) {
            Fluxo fluxo = fluxos.get(i);
            List<Long> tempos = fluxo.getTemposMedidos();
            double tempoEsperado = fluxo.getTempoEsperado();

            relatorio.append(String.format("Fluxo %d:%n", i + 1));
            relatorio.append(String.format("Média: %.2f%n", calcularMedia(tempos)));
            relatorio.append(String.format("Desvio Padrão: %.2f%n", calcularDesvioPadrao(tempos)));
            relatorio.append(String.format("Polarização (Bias): %.2f%n", calcularPolarizacao(tempos, tempoEsperado)));
            relatorio.append(String.format("Precisão: %.2f%n", calcularPrecisao(tempos)));
            relatorio.append(String.format("Incerteza: %.2f%n", calcularIncerteza(tempos, tempoEsperado)));
            relatorio.append("-------------------------------\n");
        }

        return relatorio.toString();
    }

    // Método para imprimir o relatório
    public void imprimirRelatorio() {
        System.out.println(gerarRelatorio());
    }
}
