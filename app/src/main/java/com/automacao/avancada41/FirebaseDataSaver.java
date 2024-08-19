package com.automacao.avancada41;

import android.content.Context;
import android.util.Log;

import com.example.biblioteca.Region;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson; // Certifique-se de adicionar a dependência do Gson

public class FirebaseDataSaver extends Thread {

    private static final String TAG = "FirebaseDataSaver";
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();
    private Context context;
    private List<Planta> copy;
    private Semaphore semaphore;
    private int i = 0;
    private volatile boolean running = true; // Flag para controlar a execução do loop

    public FirebaseDataSaver(Context context, List<Planta> plantas, Semaphore semaphore) {
        this.context = context;
        this.copy = plantas;
        this.semaphore = semaphore;
    }

   public FirebaseDataSaver (){}

    public Context getContexto() {
        return context;
    }

    public void setContexto(Context context) {
        this.context = context;
    }

    public List<Planta> getPlanta() {
        return copy;
    }

    public void setPlanta(List<Planta> plantas) {
        this.copy = plantas;
    }

    public Semaphore getsemaphore() {
        return semaphore;
    }

    public void setsemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;

    }

    @Override
    public void run() {
        while (running) { // Executar o loop enquanto a flag running for true
            try {
                semaphore.acquire(); // Acquire semaphore before saving

                if (copy.isEmpty()) {
                    semaphore.release(); // Release semaphore after saving
                    synchronized (copy) {

                        copy.wait(); // Aguardar até que a lista não esteja mais vazia
                    }
                } else {
                    saveData();
                    semaphore.release(); // Release semaphore after saving

                }

            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopThread() {
        running = false; // Método para parar o loop

    }
    public String obterNomePlanta(int id, int indexroute) {
        String nome;
        if (id != 0 && indexroute == 0) {
            switch (id) {
                case 1:
                    nome = "Rota1";
                    break;
                case 9:
                    nome = "Rota2";
                    break;
                case 17:
                    nome = "Rota3";
                    break;
                default:
                    nome = "Planta desconhecido";
                    break;
            }
        }else {
            switch (indexroute) {
                case 1:
                    nome = "Rota1";
                    break;
                case 2:
                    nome = "Rota2";
                    break;
                case 3:
                    nome = "Rota3";
                    break;
                default:
                    nome = "Planta desconhecido";
                    break;

            }
        }
        return nome;
    }

    private void saveData() {
        long startTime = System.nanoTime();

        DatabaseReference regiao = referencia.child(obterNomePlanta(copy.get(0).getId(),0));

        for (Planta planta : copy) {
            // Convertendo a região para JSON diretamente, sem criptografia
            String json = convertRegionToJson(planta);
            regiao.child(String.valueOf(i)).setValue(planta);
            i++;
        }
        i = 0;
        copy.clear(); // Clear list after successful saving
        stopThread();

        Log.d(TAG, "Dados Salvos no Servidor!");


    }

    private String convertRegionToJson(Planta planta) {
        // Implementação do método de conversão da região para JSON
        Gson gson = new Gson(); // Usando Gson para converter para JSON
        return gson.toJson(planta);
    }
    // Novo método para recuperar dados do banco
    public void retrieveData(OnDataRetrievedListener listener,int id,  int indexroute) {

        DatabaseReference ref = referencia.child(obterNomePlanta(id,indexroute));

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
               if (copy == null) {
                   copy = new ArrayList<>(); // Limpa a lista antes de adicionar novos dados
               }
               copy.clear();
                // Usando CountDownLatch para aguardar todas as operações serem concluídas
                int childrenCount = (int) dataSnapshot.getChildrenCount();
                CountDownLatch latch = new CountDownLatch(childrenCount);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    new Thread(() -> {
                        Planta planta = new Planta();

                        // Preencher ID
                        Integer id = snapshot.child("id").getValue(Integer.class);
                        if (id != null) {
                            planta.setId(id);
                        }


                        // Preencher Velocidade Média
                        Float velocidadeMedia = snapshot.child("velocidadeMedia").getValue(Float.class);
                        if (velocidadeMedia != null) {
                            planta.setVelocidadeMedia(velocidadeMedia);
                        }

                        // Preencher Tempo médio
                        Long tempoMedio = snapshot.child("tempoEsperado").getValue(Long.class);
                        if (tempoMedio != null) {
                            planta.setTempoEsperado(tempoMedio);
                        }

                        // Preencher tempos
                        for (DataSnapshot tempoSnapshot : snapshot.child("temposMedidos").getChildren()) {
                            Long tempo = tempoSnapshot.getValue(Long.class);
                            if (tempo != null) {
                                planta.addTempoMedido(tempo);
                            }
                        }

                        // Preencher velocidades
                        for (DataSnapshot velSnapshot : snapshot.child("velocidades").getChildren()) {
                            Float velocidade = velSnapshot.getValue(Float.class);
                            if (velocidade != null) {
                                planta.setVelocidade(velocidade);
                            }
                        }

                        // Preencher atualizado
                        Boolean atualizado = snapshot.child("atualizado").getValue(Boolean.class);
                        if (atualizado != null) {
                            planta.setAtualizado(atualizado);
                        }

                        // Preencher distância
                        Float distancia = snapshot.child("distancia").getValue(Float.class);
                        if (distancia != null) {
                            planta.setDistancia(distancia);
                        }

                        // Preencher Geocerca
                        DataSnapshot geocercaSnapshot = snapshot.child("geocerca");
                        if (geocercaSnapshot.exists()) {
                            Geocerca geocerca = new Geocerca();

                            // Preencher centro da geocerca
                            DataSnapshot centroSnapshot = geocercaSnapshot.child("centro");
                            if (centroSnapshot.exists()) {
                                Double latitude = centroSnapshot.child("latitude").getValue(Double.class);
                                Double longitude = centroSnapshot.child("longitude").getValue(Double.class);

                                // Tratamento para evitar null em latitude e longitude
                                if (latitude == null || longitude == null) {
                                    String latitudeStr = centroSnapshot.child("latitude").getValue(String.class);
                                    String longitudeStr = centroSnapshot.child("longitude").getValue(String.class);

                                    if (latitudeStr != null && longitudeStr != null) {
                                        try {
                                            latitude = Double.parseDouble(latitudeStr);
                                            longitude = Double.parseDouble(longitudeStr);
                                        } catch (NumberFormatException e) {
                                            Log.e("Geocerca", "Erro ao converter latitude ou longitude", e);
                                        }
                                    }
                                }

                                if (latitude != null && longitude != null) {
                                    LatLng centro = new LatLng(latitude, longitude);
                                    geocerca.setCentro(centro);
                                    planta.setCentro(centro); // Atualize o centro da planta também
                                } else {
                                    Log.d("Geocerca", "Latitude ou longitude é nulo");
                                }
                            } else {
                                Log.d("Geocerca", "Centro não encontrado no snapshot");
                            }

                            // Preencher raio da geocerca
                            Float raio = geocercaSnapshot.child("raio").getValue(Float.class);
                            if (raio != null) {
                                geocerca.setRaio(raio);
                            } else {
                                Log.d("Geocerca", "Raio é nulo");
                            }

                            // Preencher cor de contorno e preenchimento
                            Integer corContorno = geocercaSnapshot.child("corContorno").getValue(Integer.class);
                            if (corContorno != null) {
                                geocerca.setCorContorno(corContorno);
                            } else {
                                Log.d("Geocerca", "Cor de contorno é nulo");
                            }

                            Integer corPreenchimento = geocercaSnapshot.child("corPreenchimento").getValue(Integer.class);
                            if (corPreenchimento != null) {
                                geocerca.setCorPreenchimento(corPreenchimento);
                            } else {
                                Log.d("Geocerca", "Cor de preenchimento é nulo");
                            }

                            // Definir a geocerca da planta
                            planta.setGeocerca(geocerca);
                        } else {
                            Log.d("Geocerca", "Geocerca não encontrada no snapshot");
                        }

                        // Adiciona a planta à lista e decrementa o latch
                        copy.add(planta);
                        latch.countDown();
                    }).start();
                }


                try {
                    latch.await();

                    // Ordena a lista com base no ID (ou outro campo relevante)
                    Collections.sort(copy, new Comparator<Planta>() {
                        @Override
                        public int compare(Planta p1, Planta p2) {
                            return Integer.compare(p1.getId(), p2.getId());
                        }
                    });

                    if (listener != null) {
                        listener.onDataRetrieved(copy);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Erro ao aguardar a finalização da recuperação dos dados: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Erro ao recuperar dados: " + databaseError.getMessage());
            }
        });

    }
    public void contarReferenciasDiretas(final FirebaseCallback callback) {
        // Obtenha a referência à raiz do banco de dados (ou a qualquer nó específico)
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

        // Adiciona um listener para ler os dados uma única vez
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Conta o número de referências diretas (filhos) no nó atual
                long directReferences = dataSnapshot.getChildrenCount();

                // Passa o valor obtido para o callback
                callback.onCallback(directReferences);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Erro ao ler os dados: " + databaseError.getMessage());
            }
        });
    }









}