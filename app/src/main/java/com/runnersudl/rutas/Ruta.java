package com.runnersudl.rutas;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ruta {
    public String id;
    public final String name;
    public final String descripcion;
    public final List<LatLng> points;
    public String userUID;

    Ruta(String id, String name, String descripcion, List<LatLng> points) {
        this.id = id;
        this.name = name;
        this.descripcion = descripcion;
        this.points = new ArrayList<>(points);
    }

    Ruta(String name, String descripcion, List<LatLng> points) {
        this.name = name;
        this.descripcion = descripcion;
        this.points = new ArrayList<>(points);
    }

    public LatLng getMiddlePoint() {
        double latitude = 0;
        double longitude = 0;
        int n = points.size();

        for (LatLng point : points) {
            latitude += point.latitude;
            longitude += point.longitude;
        }

        return new LatLng(latitude/n, longitude/n);
    }

    Map<String, Object> toHashMap(){
        Map<String, Object> ruta = new HashMap<>();
        ruta.put("CreadoPor", FirebaseAuth.getInstance().getCurrentUser().getUid());
        ruta.put("Name", name);
        ruta.put("Descripcion", descripcion);
        ruta.put("Puntos", new Gson().toJson(points));
        return ruta;
    }

    static Ruta fromHashMapToRuta(Map<String, Object> rutaMap, String id){
        String userUID = (String) rutaMap.get("CreadoPor");
        String name = (String) rutaMap.get("Name");
        String descripcion = (String) rutaMap.get("Descripcion");

        Type type = new TypeToken<List<LatLng>>() {}.getType();
        List<LatLng> points = new Gson().fromJson((String) rutaMap.get("Puntos"), type);

        Ruta ruta = new Ruta(id, name, descripcion, points);
        ruta.setUserUID(userUID);
        return ruta;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }
}

