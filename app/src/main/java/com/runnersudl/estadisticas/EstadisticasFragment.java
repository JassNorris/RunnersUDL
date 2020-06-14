package com.runnersudl.estadisticas;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.runnersudl.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstadisticasFragment extends Fragment implements SensorEventListener {

    public EstadisticasFragment(){}

    private SensorManager sensorManager;

    private ProgressBar _progressBar;

    private Button historial_button;

    boolean running = false;

    private final float MULTIPLICADOR_DISTANCIA_POR_PASO = 0.415f;
    private final float MULTIPLICADOR_CALORIA_POR_PASO = 1.036f;

    private int milestoneStep;

    // Diccionario que guardará la relación de pasos por día. <String=Fecha, Integer=pasos>
    private Map<String, Integer> pasos_por_dia = new HashMap<>();

    // Root View
    private View rootView;

    private TextView pasos_tv, kilometros_tv, calorias_tv;

    private float objetivo = 10000;
    private float pasos = 0;
    private float kilometros = 0;
    private float calorias = 0;
    private float progress = 0;

    // SharedPreferences ref
    SharedPreferences sharedPref;

    List<Estadistica> estadisticas = new ArrayList<>();

    //FloatingActionButton button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_estadisticas, container, false);

        pasos_tv = rootView.findViewById(R.id.pasos);
        kilometros_tv = rootView.findViewById(R.id.kilometros);
        calorias_tv = rootView.findViewById(R.id.calorias);
        historial_button = rootView.findViewById(R.id.historial);

        // AlertDialog para cambiar el historial
        historial_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_historial, null);

                // RecyclerView del historial
                RecyclerView recyclerView;
                final RecyclerView.Adapter mAdapter;
                RecyclerView.LayoutManager layoutManager;

                recyclerView = (RecyclerView) dialogView.findViewById(R.id.recyclerView);
                recyclerView.setHasFixedSize(true);

                View view = dialogView.findViewById(R.id.loading);
                view.setVisibility(View.VISIBLE);

                // use a linear layout manager
                layoutManager = new LinearLayoutManager(getActivity());
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                        DividerItemDecoration.VERTICAL));

                mAdapter = new HistorialRecyclerView(getContext(), estadisticas);
                recyclerView.setAdapter(mAdapter);

                // GET ESTADISTICAS FIREBASE
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users data").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Estadisticas");
                ref.addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //Get map of stats in datasnapshot
                                Map<String, Object> stats = (Map<String, Object>) dataSnapshot.getValue();

                                if (stats != null){
                                    estadisticas.clear();
                                    view.setVisibility(View.VISIBLE);
                                    for (Map.Entry<String, Object> entry : stats.entrySet()){
                                        Map estadistica = (Map) entry.getValue();
                                        estadisticas.add(new Estadistica(String.format("%.0f",Float.valueOf(estadistica.get("Pasos").toString())), String.format("%.0f",Float.valueOf(estadistica.get("Calorias").toString())), estadistica.get("Kilometros").toString(), estadistica.get("Porcentaje").toString(), entry.getKey()));
                                    }

                                    estadisticas.sort(Comparator.comparing(Estadistica::getDate).reversed());
                                    mAdapter.notifyDataSetChanged();
                                    view.setVisibility(View.INVISIBLE);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                //handle databaseError
                            }
                        });

                builder.setView(dialogView);
                final AlertDialog dialog = builder.create();

                Button cancelar = (Button) dialogView.findViewById(R.id.cancelar);

                cancelar.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        return rootView;
    }

    // Actualiza los pasos
    private void setPasos(int pasosDeHoy){
        pasos = pasosDeHoy;
        pasos_tv.setText(String.valueOf(Math.round(pasos)));
        setKilometros();
        setCalorias();
    }

    // Actualiza los kilometros recorridos
    private void setKilometros(){
        int altura = Integer.valueOf(sharedPref.getString(getString(R.string.keyAltura), "170"));

        float longitud_paso_en_m = (altura * MULTIPLICADOR_DISTANCIA_POR_PASO) / 100;
        kilometros = (longitud_paso_en_m * pasos)/1000;
        kilometros_tv.setText(getString(R.string.kilometros, kilometros));
    }

    // Actualiza las calorías quemadas.
    private void setCalorias(){
        int peso = Integer.valueOf(sharedPref.getString(getString(R.string.keyPeso), "75"));

        calorias = kilometros * peso * MULTIPLICADOR_CALORIA_POR_PASO;

        calorias_tv.setText(getString(R.string.kilometros, calorias));
    }

    public void actualizar() {
        //setProgress();
        setKilometros();
        setCalorias();
    }

    /**
     *
     * Control de sensores
     * @param sensorEvent
     */

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) { // Good practice -> Don't block the onSensorChanged() method
        getSteps(sensorEvent);
    }

    // sensorEvent.values[0] devuelve los pasos dados por el usuario desde el último reboot
    // pero nosotros queremos saber los pasos que ha dado en un día.
    private void getSteps(SensorEvent sensorEvent){
        int totalStepSinceReboot = (int) sensorEvent.values[0];

        if(!pasos_por_dia.containsKey(today()))     // Todavía no se han registrado los pasos de hoy
        {
            milestoneStep = totalStepSinceReboot;
            pasos_por_dia.put(today(), 0);
        }
        else                                        // Los pasos de hoy ya estan siendo registrados
        {
            int todayStep = pasos_por_dia.get(today());

            pasos_por_dia.put(today(), todayStep + totalStepSinceReboot - milestoneStep);

            milestoneStep = totalStepSinceReboot;
        }

        // Actualizar pasos
        setPasos(pasos_por_dia.get(today()));
    }

    // Devuelve que dia es hoy - Para realizar un historico
    public String today() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(Calendar.getInstance().getTime());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Coger pasos_por_dia de SharedPreferences
        String json = sharedPref.getString("pasos_por_dia", "");
        java.lang.reflect.Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
        milestoneStep = sharedPref.getInt("milestoneStep", 0);
        if (!json.isEmpty()) pasos_por_dia = new Gson().fromJson(json, type);

        // Coger objetivo de SharedPreferences
        int objetivo_rec = sharedPref.getInt("objetivo", -1);

        // TYPE_STEP_COUNTER Sensor
        running = true;
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) { // Good practice -> Verify sensors before you use them
            sensorManager.registerListener(this,                        // Good practice -> Register sensor appropiately
                    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                    SensorManager.SENSOR_DELAY_UI);
        }
        else // El dispositivo no tiene el sensor TYPE_STEP_COUNTER
        {
            Toast.makeText(getActivity(), getString(R.string.no_sensor), Toast.LENGTH_LONG).show();
            setPasos(0); actualizar();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onPause() {
        super.onPause();
        running = false;
        sensorManager.unregisterListener(this); // Good practice -> Unregister sensor appropiately

        SharedPreferences.Editor editor = sharedPref.edit();

        // Guardar pasos en SharedPreferences
        String hashMapString = new Gson().toJson(pasos_por_dia);
        editor.putString("pasos_por_dia", hashMapString);
        editor.putInt("milestoneStep", milestoneStep);

        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference mRef =  database.getReference().child("Users data").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Estadisticas").child(today());

            mRef.child("Pasos").setValue(String.valueOf(pasos));
            mRef.child("Calorias").setValue(String.valueOf(calorias));
            mRef.child("Kilometros").setValue(String.valueOf(kilometros));
            mRef.child("Porcentaje").setValue(String.valueOf(progress));

            mRef.push();
        }

        // Guardar objetivo en SharedPreferences
        editor.putInt("objetivo", Math.round(objetivo));

        editor.commit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // SharedPreferences reference
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
    }
}
