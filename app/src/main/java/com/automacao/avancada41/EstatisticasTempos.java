package com.automacao.avancada41;

import android.util.Log;

import java.util.List;

public class EstatisticasTempos {
    private List<Planta> plantas;

    // Construtor com lista de plantas
    public EstatisticasTempos(List<Planta> plantas) {
        this.plantas = plantas;
    }

    // Construtor padrão
    public EstatisticasTempos() {}

    // Setter para a lista de plantas
    public void setPlantas(List<Planta> plantas) {
        this.plantas = plantas;
    }

    // Método para calcular a média dos tempos
    private double calcularMedia(List<Long> tempos) {
        if (tempos == null || tempos.isEmpty()) {
            throw new IllegalArgumentException("A lista de tempos está vazia ou é nula.");
        }
        return tempos.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(Double.NaN);
    }

    // Método para calcular o desvio padrão dos tempos
    private double calcularDesvioPadrao(List<Long> tempos) {
        double media = calcularMedia(tempos);
        double variancia = tempos.stream()
                .mapToDouble(t -> Math.pow(t - media, 2))
                .average()
                .orElse(Double.NaN);
        return Math.sqrt(variancia);
    }

    // Método para calcular a polarização (bias) dos tempos
    private double calcularPolarizacao(List<Long> tempos, double tempoEsperado) {
        double media = calcularMedia(tempos);
        return media - tempoEsperado;
    }

    // Método para calcular a precisão (2 vezes o desvio padrão) dos tempos
    private double calcularPrecisao(List<Long> tempos) {
        double desvioPadrao = calcularDesvioPadrao(tempos);
        return desvioPadrao * 2;
    }
    public double[] calcularVetorDesvio() {
        int numPlantas = plantas.size();
        double[] medias = new double[numPlantas - 1];

        // Calcula a média dos tempos para cada planta, excluindo a primeira e a última
        for (int i = 1; i < numPlantas; i++) {
            List<Long> tempos = plantas.get(i).getTemposMedidos();
            medias[i - 1] = calcularDesvioPadrao(tempos);
        }

        return medias;
    }


    // Método para calcular a incerteza dos tempos (combinando polarização e precisão)
    private double calcularIncerteza(List<Long> tempos, double tempoEsperado) {
        double polarizacao = calcularPolarizacao(tempos, tempoEsperado);
        double precisao = calcularPrecisao(tempos);
        return Math.sqrt(Math.pow(polarizacao, 2) + Math.pow(precisao, 2));
    }

    // Método para calcular o vetor de médias
    public double[] calcularVetorMedias() {
        int numPlantas = plantas.size();
        double[] medias = new double[numPlantas - 1];

        // Calcula a média dos tempos para cada planta, excluindo a primeira e a última
        for (int i = 1; i < numPlantas; i++) {
            List<Long> tempos = plantas.get(i).getTemposMedidos();
            medias[i - 1] = calcularMedia(tempos);
        }

        return medias;
    }

    // Método para imprimir o vetor de médias
    public void imprimirVetorMedias() {
        double[] medias = calcularVetorMedias();
        System.out.println("Vetor de Médias:");
        for (double media : medias) {
            System.out.printf("%.2f ", media);
        }
        System.out.println();
    }

    // Método para criar a matriz de incidência
    public double[][] criarMatrizIncidencia() {
        int numFluxos = plantas.size() - 1;
        int numPlanta = plantas.size() - 2;
        double[][] matriz = new double[numPlanta][numFluxos];

        // Configuração da matriz de incidência
        for (int i = 0; i < numPlanta; i++) {
            matriz[i][i] = 1;    // saída do fluxo i
            matriz[i][i + 1] = -1; // entrada no próximo fluxo i+1
        }

        return matriz;
    }

    // Método para imprimir a matriz de incidência
    public void imprimirMatrizIncidencia() {
        double[][] matriz = criarMatrizIncidencia();
        System.out.println("Matriz de Incidência:");
        for (double[] linha : matriz) {
            for (double elemento : linha) {
                System.out.print(elemento + " ");
            }
            System.out.println();
        }
    }

    // Método para calcular a matriz de variâncias (diagonal de variâncias)
    public double[][] calcularMatrizVariancias() {
        int numPlantas = plantas.size() - 1;
        double[][] matrizVariancias = new double[numPlantas][numPlantas];
        double varianciaMinima = 1e-5; // Valor mínimo para evitar variâncias zero

        for (int i = 0; i < numPlantas; i++) {
            List<Long> tempos = plantas.get(i + 1).getTemposMedidos();
            double desvioPadrao = calcularDesvioPadrao(tempos);
            double variancia = Math.pow(desvioPadrao, 2);

            if (variancia == 0) {
                variancia = varianciaMinima;
            }

            matrizVariancias[i][i] = variancia;  // Inserir a variância na diagonal
        }

        return matrizVariancias;
    }

    // Método para imprimir a matriz de variâncias
    public void imprimirMatrizVariancias() {
        double[][] matrizVariancias = calcularMatrizVariancias();
        System.out.println("Matriz de Variâncias:");
        for (double[] linha : matrizVariancias) {
            for (double elemento : linha) {
                System.out.printf("%.2f ", elemento);
            }
            System.out.println();
        }
    }

    // Método para realizar a reconciliação dos dados
    public double[] reconciliarDados() {
        double[] y = calcularVetorMedias();
        double[][] V = calcularMatrizVariancias();
        double[][] A = criarMatrizIncidencia();

        // Verificação de entradas na matriz de variância
        for (int i = 0; i < V.length; i++) {
            if (V[i][i] == 0) {
                throw new IllegalArgumentException("A matriz de variância contém entradas zero.");
            }
        }

        double lambda = 1e-5;
        double[][] A_V_A_t = multiplicarMatrices(A, multiplicarMatrices(V, transporMatriz(A)));
        double[][] A_V_A_t_reg = somarMatrices(A_V_A_t, multiplicarPorEscalar(identidade(A_V_A_t.length), lambda));

        double[] yhat = subtrairVetores(y, multiplicarMatrizVetor(V, multiplicarMatrizVetor(transporMatriz(A), resolverSistema(A_V_A_t_reg, multiplicarMatrizVetor(A, y)))));

        return yhat;
    }

    // Método para imprimir as medidas reconciliadas
    public void imprimirMedidasReconciliadas() {
        double[] yhat = reconciliarDados();
        System.out.println("Medidas Reconciliadas:");
        for (double valor : yhat) {
            System.out.printf("%.4f ", valor);
        }
        System.out.println();
    }

    // Método auxiliar para transpor uma matriz
    private double[][] transporMatriz(double[][] matriz) {
        int m = matriz.length;
        int n = matriz[0].length;
        double[][] transposta = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                transposta[j][i] = matriz[i][j];
            }
        }
        return transposta;
    }

    // Método auxiliar para multiplicar duas matrizes
    private double[][] multiplicarMatrices(double[][] A, double[][] B) {
        if (A[0].length != B.length) {
            throw new IllegalArgumentException("Número de colunas de A deve ser igual ao número de linhas de B.");
        }

        int linhasA = A.length;
        int colunasA = A[0].length;
        int colunasB = B[0].length;

        double[][] resultado = new double[linhasA][colunasB];

        // Multiplicação de matrizes
        for (int i = 0; i < linhasA; i++) {
            for (int j = 0; j < colunasB; j++) {
                resultado[i][j] = 0;
                for (int k = 0; k < colunasA; k++) {
                    resultado[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return resultado;
    }

    // Método auxiliar para multiplicar uma matriz por um vetor
    private double[] multiplicarMatrizVetor(double[][] matriz, double[] vetor) {
        int m = matriz.length;
        int n = vetor.length;
        if (matriz[0].length != n) {
            throw new IllegalArgumentException("Número de colunas da matriz deve ser igual ao tamanho do vetor.");
        }
        double[] resultado = new double[m];
        for (int i = 0; i < m; i++) {
            resultado[i] = 0;
            for (int j = 0; j < n; j++) {
                resultado[i] += matriz[i][j] * vetor[j];
            }
        }
        return resultado;
    }

    // Método auxiliar para somar duas matrizes
    private double[][] somarMatrices(double[][] A, double[][] B) {
        int m = A.length;
        int n = A[0].length;
        if (B.length != m || B[0].length != n) {
            throw new IllegalArgumentException("As matrizes devem ter o mesmo tamanho.");
        }
        double[][] resultado = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                resultado[i][j] = A[i][j] + B[i][j];
            }
        }
        return resultado;
    }

    // Método auxiliar para multiplicar uma matriz por um escalar
    private double[][] multiplicarPorEscalar(double[][] matriz, double escalar) {
        int m = matriz.length;
        int n = matriz[0].length;
        double[][] resultado = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                resultado[i][j] = matriz[i][j] * escalar;
            }
        }
        return resultado;
    }

    // Método auxiliar para subtrair dois vetores
    private double[] subtrairVetores(double[] vetorA, double[] vetorB) {
        if (vetorA.length != vetorB.length) {
            throw new IllegalArgumentException("Os vetores devem ter o mesmo tamanho.");
        }
        double[] resultado = new double[vetorA.length];
        for (int i = 0; i < vetorA.length; i++) {
            resultado[i] = vetorA[i] - vetorB[i];
        }
        return resultado;
    }

    // Método auxiliar para resolver sistemas lineares usando eliminação de Gauss
    private double[] resolverSistema(double[][] A, double[] b) {
        int n = b.length;
        double[] x = new double[n];
        double[][] augmentedMatrix = new double[n][n + 1];

        // Monta a matriz aumentada
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, augmentedMatrix[i], 0, n);
            augmentedMatrix[i][n] = b[i];
        }

        // Eliminação de Gauss
        for (int i = 0; i < n; i++) {
            // Pivô
            for (int k = i + 1; k < n; k++) {
                double fator = augmentedMatrix[k][i] / augmentedMatrix[i][i];
                for (int j = 0; j <= n; j++) {
                    augmentedMatrix[k][j] -= fator * augmentedMatrix[i][j];
                }
            }
        }

        // Substituição regressiva
        for (int i = n - 1; i >= 0; i--) {
            x[i] = augmentedMatrix[i][n];
            for (int j = i + 1; j < n; j++) {
                x[i] -= augmentedMatrix[i][j] * x[j];
            }
            x[i] /= augmentedMatrix[i][i];
        }

        return x;
    }

    // Método auxiliar para criar uma matriz identidade
    private double[][] identidade(int tamanho) {
        double[][] identidade = new double[tamanho][tamanho];
        for (int i = 0; i < tamanho; i++) {
            identidade[i][i] = 1.0;
        }
        return identidade;
    }

    // Método para gerar o relatório completo para todas as plantas
    public String gerarRelatorio() {
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("Relatório Geral de Estatísticas das Plantas:\n");
        relatorio.append("-------------------------------------------\n");

        for (int i = 1; i < plantas.size()-1; i++) {
            Planta planta = plantas.get(i);
            List<Long> tempos = planta.getTemposMedidos();
            double tempoEsperado = planta.getTempoEsperado();

            relatorio.append(String.format("Planta %d:%n", i + 1));
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
