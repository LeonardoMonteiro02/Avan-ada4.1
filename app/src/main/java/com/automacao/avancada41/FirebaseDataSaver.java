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
    private List<Fluxo> copy;
    private Semaphore semaphore;
    private int i = 0;
    private volatile boolean running = true; // Flag para controlar a execução do loop

    public FirebaseDataSaver(Context context, List<Fluxo> fluxos, Semaphore semaphore) {
        this.context = context;
        this.copy = fluxos;
        this.semaphore = semaphore;
    }


    public Context getContexto() {
        return context;
    }

    public void setContexto(Context context) {
        this.context = context;
    }

    public List<Fluxo> getFluxos() {
        return copy;
    }

    public void setFluxos(List<Fluxo> fluxos) {
        this.copy = fluxos;
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
                        Log.d(TAG, "Aguardando lista");
                        copy.wait(); // Aguardar até que a lista não esteja mais vazia
                    }
                } else {
                    saveData();
                    semaphore.release(); // Release semaphore after saving
                    Log.d(TAG, "Semafaro Liberado.");
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void stopThread() {
        running = false; // Método para parar o loop
        //retrieveData();
    }

    private void saveData() {
        long startTime = System.nanoTime();
        DatabaseReference regiao = referencia.child("Fluxos");

        for (Fluxo fluxo : copy) {
            // Convertendo a região para JSON diretamente, sem criptografia
            String json = convertRegionToJson(fluxo);
            regiao.child(String.valueOf(i)).setValue(fluxo);
            i++;
        }
        i = 0;
        copy.clear(); // Clear list after successful saving
        stopThread();

        Log.d(TAG, "Dados Salvos no Servidor!");

        // Registra o tempo de término para salvar no banco
        long endTime = System.nanoTime();
        // Calcula o tempo decorrido em milissegundos
        long elapsedTime = endTime - startTime;
        // Use o tempo decorrido conforme necessário, como registrá-lo em logs ou realizar outras ações
        Log.d(TAG, "Tempo decorrido para salvar no banco: " + elapsedTime + " nanosegundos");
    }

    private String convertRegionToJson(Fluxo fluxo) {
        // Implementação do método de conversão da região para JSON
        Gson gson = new Gson(); // Usando Gson para converter para JSON
        return gson.toJson(fluxo);
    }
    // Novo método para recuperar dados do banco
    public void retrieveData(OnDataRetrievedListener listener) {
        DatabaseReference ref = referencia.child("Fluxos");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                copy.clear(); // Limpa a lista antes de adicionar novos dados

                // Usando CountDownLatch para aguardar todas as operações serem concluídas
                int childrenCount = (int) dataSnapshot.getChildrenCount();
                CountDownLatch latch = new CountDownLatch(childrenCount);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    new Thread(() -> {
                        Fluxo fluxo = new Fluxo();

                        Integer id = snapshot.child("id").getValue(Integer.class);
                        if (id != null) {
                            fluxo.setId(id);
                        }

                        Long tempo = snapshot.child("tempo").getValue(Long.class);
                        if (tempo != null) {
                            fluxo.setTempo(tempo);
                        }


                        for (DataSnapshot velSnapshot : snapshot.child("velocidade").getChildren()) {
                            Float velocidade = velSnapshot.getValue(Float.class);
                            if (velocidade != null) {
                                fluxo.setVelocidade(velocidade);
                            }
                        }

                        Boolean atualizado = snapshot.child("atualizado").getValue(Boolean.class);
                        if (atualizado != null) {
                            fluxo.setAtualizado(atualizado);
                        }

                        DataSnapshot geocercaSnapshot = snapshot.child("geocerca");
                        if (geocercaSnapshot.exists()) {
                            Geocerca geocerca = new Geocerca();

                            LatLng centro = null;
                            Double latitude = geocercaSnapshot.child("latitude").getValue(Double.class);
                            Double longitude = geocercaSnapshot.child("longitude").getValue(Double.class);
                            if (latitude != null && longitude != null) {
                                centro = new LatLng(latitude, longitude);
                            }

                            Integer raio = geocercaSnapshot.child("raio").getValue(Integer.class);
                            if (centro != null && raio != null) {
                                geocerca.setCentro(centro);
                                geocerca.setRaio(raio);
                            }

                            fluxo.setGeocerca(geocerca);
                        }

                        copy.add(fluxo);
                        latch.countDown();
                    }).start();
                }

                try {
                    latch.await();

                    // Ordena a lista com base no ID (ou outro campo relevante)
                    Collections.sort(copy, new Comparator<Fluxo>() {
                        @Override
                        public int compare(Fluxo f1, Fluxo f2) {
                            return Integer.compare(f1.getId(), f2.getId());
                        }
                    });

                    Log.d(TAG, "Dados recuperados com sucesso!" + copy.size());
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





}