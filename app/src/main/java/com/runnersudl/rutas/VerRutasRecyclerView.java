package com.runnersudl.rutas;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.runnersudl.R;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class VerRutasRecyclerView extends RecyclerView.Adapter<VerRutasRecyclerView.ViewHolder> {

    private List<Ruta> rutas;

    private Context context;

    public VerRutasRecyclerView(List<Ruta> locations, MyAdapterListener listener) {
        super();
        rutas = locations;
        onClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_ver_rutas, parent, false);
        v.setOnClickListener(new RetoClickListener());
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder == null) {
            return;
        }
        final ViewHolder itemHolder = (ViewHolder) holder;
        itemHolder.view_to_hide.setVisibility(View.GONE);


        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mRef = database.getReference().child("Users data").child(rutas.get(position).userUID);
        mRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of stats in datasnapshot

                        //TODO: Corregir los errores del click sobre ver rutas.
                        Object objectNombre = dataSnapshot.child("Nombre").getValue();
                        if (objectNombre != null)
                            itemHolder.creado_por.setText(objectNombre.toString());
                        else
                            itemHolder.creado_por.setText("---");


                        Object objectPhotoURL = dataSnapshot.child("PhotoURL").getValue();

                        if(objectPhotoURL!=null){
                            if (!objectPhotoURL.toString().equals("")) {
                                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(Uri.parse(dataSnapshot.child("PhotoURL").getValue().toString()).getLastPathSegment());

                                // Load the image using Glide
                                Glide.with(context /* context */)
                                        .using(new FirebaseImageLoader())
                                        .load(storageReference)
                                        .into(itemHolder.imageView);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

        holder.bindView(position);
    }

    @Override
    public int getItemCount() {
        return rutas.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {
        MapView mapView;
        TextView title;
        GoogleMap map;
        View layout;
        Button button;
        TextView description, kilometros, creado_por;
        View view_to_hide;
        CircleImageView imageView;

        private ViewHolder(View itemView) {
            super(itemView);
            layout = itemView;
            view_to_hide = layout.findViewById(R.id.view_to_expand);
            mapView = layout.findViewById(R.id.lite_listrow_map);
            title = layout.findViewById(R.id.lite_listrow_text);
            button = layout.findViewById(R.id.realizar_reto);
            description = layout.findViewById(R.id.lite_listrow_description);
            kilometros = layout.findViewById(R.id.kilometros_polyline);
            creado_por = layout.findViewById(R.id.creado_por);
            imageView = layout.findViewById(R.id.image_profile_reto);

            if (mapView != null) {
                // Initialise the MapView
                mapView.onCreate(null);
                // Set the map ready callback to receive the GoogleMap object
                mapView.getMapAsync(this);
                mapView.setClickable(false);
            }

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.retoOnClick(v, getAdapterPosition());
                }
            });
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            setMapLocation();
        }

        private void setMapLocation() {
            if (map == null) return;

            Ruta data = (Ruta) mapView.getTag();
            if (data == null) return;

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i = 0; i < data.points.size(); i++) {
                builder.include(data.points.get(i));
            }

            //TODO: Corregir los errores del click sobre ver rutas.
            LatLngBounds bounds;
            double km = 0;

            if (!data.points.isEmpty()) {

                Polyline polyline = map.addPolyline(new PolylineOptions()
                        .color(Color.HSVToColor(255, new float[]{1, 1, 1}))
                        .width(20)
                        .clickable(false)
                        .addAll(data.points));

                polyline.setStartCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start_cap), 50));
                polyline.setEndCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.end_cap), 300));
                polyline.setJointType(JointType.DEFAULT);
                polyline.setPattern(null);

                bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
                map.animateCamera(cu);
                km = calculateDistanceOfPolyline(data.points);
            }

            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map.getUiSettings().setMapToolbarEnabled(false);

            kilometros.setText(String.format("%.2f Km", km));


        }

        private void bindView(int pos) {
            Ruta item = rutas.get(pos);
            layout.setTag(this);
            mapView.setTag(item);
            setMapLocation();
            title.setText(item.name);
            description.setText(item.descripcion);
        }
    }

    class RetoClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            View someView = v.findViewById(R.id.view_to_expand);

            if (someView.getVisibility() == View.GONE) {
                someView.setVisibility(View.VISIBLE);
            } else if (someView.getVisibility() == View.VISIBLE) {
                someView.setVisibility(View.GONE);
            }

        }
    }

    public MyAdapterListener onClickListener;

    public interface MyAdapterListener {
        void retoOnClick(View v, int position);
    }

    private double calculateDistanceOfPolyline(List<LatLng> points) {
        double distance = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            Location locationA = new Location("A");
            locationA.setLatitude(points.get(i).latitude);
            locationA.setLongitude(points.get(i).longitude);
            if (i + 1 < points.size()) {
                Location locationB = new Location("B");
                locationB.setLatitude(points.get(i + 1).latitude);
                locationB.setLongitude(points.get(i + 1).longitude);
                distance += locationA.distanceTo(locationB);
            }
        }
        return distance / 1000;
    }
}
