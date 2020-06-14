package com.runnersudl.estadisticas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.runnersudl.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class HistorialRecyclerView extends RecyclerView.Adapter<HistorialRecyclerView.EstadisticasHistorialHolder> {

    private List<Estadistica> estadisticasDiarias;

    private Context context;

    public HistorialRecyclerView(Context context, List<Estadistica> estadisticasDiarias) {
        this.context = context;
        this.estadisticasDiarias = estadisticasDiarias;
    }

    @Override
    public int getItemCount() {
        return estadisticasDiarias.size();
    }

    @Override
    public void onBindViewHolder(@NonNull EstadisticasHistorialHolder itemsViewHolder, int i) {
        itemsViewHolder.pasos.setText(estadisticasDiarias.get(i).pasos + " pasos");
        itemsViewHolder.porcentaje.setText(estadisticasDiarias.get(i).porcentaje + "%");
        itemsViewHolder.calorias.setText(estadisticasDiarias.get(i).calorias + " Kcal");
        itemsViewHolder.kilometros.setText(estadisticasDiarias.get(i).kilometros + " km");
        if (estadisticasDiarias.get(i).dia.equals(today()))
            itemsViewHolder.fecha.setText(context.getString(R.string.hoy));
        else
            itemsViewHolder.fecha.setText(estadisticasDiarias.get(i).dia);
        itemsViewHolder.circularProgressBar.setProgress(Math.round(Float.valueOf(estadisticasDiarias.get(i).porcentaje)));

    }

    @NonNull
    @Override
    public EstadisticasHistorialHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_historial, viewGroup, false);

        return new EstadisticasHistorialHolder(itemView);
    }

    public static class EstadisticasHistorialHolder extends RecyclerView.ViewHolder {

        protected ProgressBar circularProgressBar;
        protected TextView porcentaje, pasos, calorias, kilometros, fecha;

        EstadisticasHistorialHolder(View v) {
            super(v);
            pasos = (TextView) v.findViewById(R.id.pasos);
            calorias = (TextView) v.findViewById(R.id.calorias);
            kilometros = (TextView) v.findViewById(R.id.kilometros);
            fecha = (TextView) v.findViewById(R.id.fecha);
            porcentaje = (TextView) v.findViewById(R.id.porcentaje);
            circularProgressBar = (ProgressBar) v.findViewById(R.id.circularProgressBar);
        }
    }

    public String today() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(Calendar.getInstance().getTime());
    }
}
