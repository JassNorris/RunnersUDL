package com.runnersudl.estadisticas;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class Estadistica {
    String pasos, calorias, kilometros, porcentaje, dia;

    Estadistica(String pasos, String calorias, String kilometros, String porcentaje, String dia){
        this.pasos = pasos;
        this.calorias = calorias;
        this.kilometros = kilometros;
        this.porcentaje = porcentaje;
        this.dia = dia;
    }

    Map<String, Object> toHashMap(){
        Map<String, Object> estadistica = new HashMap<>();
        estadistica.put("Pasos", pasos);
        estadistica.put("Calorias", calorias);
        estadistica.put("Kilometros", kilometros);
        estadistica.put("Porcentaje", porcentaje);
        return estadistica;
    }

    static Estadistica fromHashMapToEstadistica(Map<String, Object> estadisticaMap, String dia){
        String pasos = (String) estadisticaMap.get("Pasos");
        String calorias = (String) estadisticaMap.get("Calorias");
        String kilometros = (String) estadisticaMap.get("Kilometros");
        String porcentaje = (String) estadisticaMap.get("Porcentaje");
        return new Estadistica(pasos, calorias, kilometros, porcentaje, dia);
    }

    public Date getDate(){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dia);
        } catch (ParseException ex) {}
        return null;
    }
}
