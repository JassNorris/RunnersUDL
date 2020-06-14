package com.runnersudl.estadisticas;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.runnersudl.HomeActivity;
import com.runnersudl.R;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HistorialSteps extends AppCompatActivity {

    Button cancelar;
    List<Estadistica> estadisticas = new ArrayList<>();
    ValueEventListener otro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_steps);

        cancelar = (Button) findViewById(R.id.cancelar);
        Estats();
    }

    public void Estats(){
        RecyclerView recyclerView;
        final RecyclerView.Adapter mAdapter;
        RecyclerView.LayoutManager layoutManager;

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        View view = findViewById(R.id.loading);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mAdapter = new HistorialRecyclerView(this, estadisticas);
        recyclerView.setAdapter(mAdapter);

        // get User data
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users data").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Estadisticas");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of stats in datasnapshot
                        Map<String, Object> stats = (Map<String, Object>) dataSnapshot.getValue();

                        if (stats != null) {
                            estadisticas.clear();
                            view.setVisibility(View.VISIBLE);
                            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                                Map estadistica = (Map) entry.getValue();
                                estadisticas.add(new Estadistica(String.format("%.0f", Float.valueOf(estadistica.get("Pasos").toString())), String.format("%.0f", Float.valueOf(estadistica.get("Calorias").toString())), estadistica.get("Kilometros").toString(), estadistica.get("Porcentaje").toString(), entry.getKey()));
                            }

                            estadisticas.sort(Comparator.comparing(Estadistica::getDate).reversed());
                            //mAdapter.notifyDataSetChanged();
                            view.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //database Error
                    }
            }
        );
    }



    public void Back(){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}
