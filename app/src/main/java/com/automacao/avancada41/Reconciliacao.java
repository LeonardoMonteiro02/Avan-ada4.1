package com.automacao.avancada41;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

public class Reconciliacao {

    private double[] fluxoReconciliado;
    private SimpleMatrix fluxoReconciliadoMatriz;
    private SimpleMatrix ajuste;
    private SimpleMatrix medicaoBruta;
    private SimpleMatrix desvioPadrao;
    private SimpleMatrix matrizVariancia;
    private SimpleMatrix matrizIncidencia;
    private SimpleMatrix matrizDiagonal;
    private SimpleMatrix arrayPesos;

    // Construtor 1
    public Reconciliacao(double[] _medicaoBruta, double[] _desvioPadrao, double[][] _matrizIncidencia) {
        if ((_medicaoBruta != null) && (_desvioPadrao != null) && (_matrizIncidencia != null)) {
            if ((_medicaoBruta.length == _desvioPadrao.length)
                    && (_desvioPadrao.length == _matrizIncidencia[0].length)) {
                this.matrizIncidencia = new SimpleMatrix(_matrizIncidencia);
                this.medicaoBruta = new SimpleMatrix(_medicaoBruta.length, 1, true, _medicaoBruta);
                this.desvioPadrao = new SimpleMatrix(_desvioPadrao.length, 1, true, _desvioPadrao);
                double[] auxDesvioPadrao = _desvioPadrao.clone();
                double[][] auxMatrizVariancia = new double[auxDesvioPadrao.length][auxDesvioPadrao.length];
                for (int i = 0; i < auxMatrizVariancia.length; i++) {
                    for (int j = 0; j < auxMatrizVariancia[0].length; j++) {
                        if (i == j) {
                            auxMatrizVariancia[i][i] = auxDesvioPadrao[i];
                        } else {
                            auxMatrizVariancia[i][j] = 0;
                        }
                    }
                }

                this.matrizVariancia = new SimpleMatrix(auxMatrizVariancia);

                SimpleMatrix aux1 = this.matrizVariancia.mult(this.matrizIncidencia.transpose());
                aux1 = aux1.mult(this.matrizIncidencia.mult(aux1).invert());
                this.ajuste = aux1.mult(this.matrizIncidencia.mult(this.medicaoBruta));
                this.fluxoReconciliadoMatriz = this.medicaoBruta.minus(this.ajuste);
                DMatrixRMaj temp = this.fluxoReconciliadoMatriz.getMatrix();
                this.fluxoReconciliado = temp.getData();

            } else {
                System.out.println(
                        "A medicaoBruta e/ou desvioPadrao e/ou matrizIncidencia têm dados/tamanhos inconsistentes.");
            }

        } else {
            System.out.println("A medicaoBruta e/ou desvioPadrao e/ou matrizIncidencia têm dados nulos.");
        }
    }

    // Construtor 2
    public Reconciliacao(double[] _medicaoBruta, double[] _desvioPadrao, double[] _matrizIncidencia) {
        if ((_medicaoBruta != null) && (_desvioPadrao != null) && (_matrizIncidencia != null)) {
            if ((_medicaoBruta.length == _desvioPadrao.length)
                    && (_desvioPadrao.length == _matrizIncidencia.length)) {

                this.matrizIncidencia = new SimpleMatrix(_matrizIncidencia.length, 1, true, _matrizIncidencia);
                this.medicaoBruta = new SimpleMatrix(_medicaoBruta.length, 1, true, _medicaoBruta);
                this.desvioPadrao = new SimpleMatrix(_desvioPadrao.length, 1, true, _desvioPadrao);

                double[][] auxMatrizDiagonal = new double[_medicaoBruta.length + 1][_medicaoBruta.length + 1];
                double[] auxArrayPesos = new double[_medicaoBruta.length + 1];
                for (int i = 0; i < _medicaoBruta.length; i++) {
                    double auxMP = Math.pow((_medicaoBruta[i] * _desvioPadrao[i]), 2);
                    auxMatrizDiagonal[i][i] = 2 / auxMP;
                    auxArrayPesos[i] = (2 * _medicaoBruta[i]) / auxMP;
                }

                this.matrizDiagonal = new SimpleMatrix(auxMatrizDiagonal);
                this.matrizDiagonal.setColumn(auxMatrizDiagonal.length - 1, 0, _matrizIncidencia);
                this.matrizDiagonal.setRow(auxMatrizDiagonal.length - 1, 0, _matrizIncidencia);
                this.arrayPesos = new SimpleMatrix(auxArrayPesos.length, 1, true, auxArrayPesos);

                this.fluxoReconciliadoMatriz = this.matrizDiagonal.invert().mult(this.arrayPesos);
                DMatrixRMaj temp = this.fluxoReconciliadoMatriz.getMatrix();
                this.fluxoReconciliado = temp.getData();

            } else {
                System.out.println(
                        "A medicaoBruta e/ou desvioPadrao e/ou matrizIncidencia têm dados/tamanhos inconsistentes.");
            }

        } else {
            System.out.println("A medicaoBruta e/ou desvioPadrao e/ou matrizIncidencia têm dados nulos.");
        }
    }

    // Método para imprimir o fluxo reconciliado
    public void printaFluxoReconciliado() {
        if (fluxoReconciliado != null) {
            for (double valor : fluxoReconciliado) {
                System.out.println("| " + valor + " |");
            }
        } else {
            System.out.println("O fluxo reconciliado está nulo.");
        }
    }

    public double[] getFluxoReconciliado() {
        return this.fluxoReconciliado;
    }

    public SimpleMatrix getAjuste() {
        return this.ajuste;
    }

    public SimpleMatrix getMedicaoBruta() {
        return this.medicaoBruta;
    }

    public SimpleMatrix getDesvioPadrao() {
        return this.desvioPadrao;
    }

    public SimpleMatrix getMatrizVariancia() {
        return this.matrizVariancia;
    }

    public SimpleMatrix getMatrizIncidencia() {
        return this.matrizIncidencia;
    }

    public SimpleMatrix getMatrizDiagonal() {
        return this.matrizDiagonal;
    }

    public SimpleMatrix getArrayPesos() {
        return this.arrayPesos;
    }
}
