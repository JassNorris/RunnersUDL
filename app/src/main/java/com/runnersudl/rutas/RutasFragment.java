package com.runnersudl.rutas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.runnersudl.R;
import com.runnersudl.services.ActivityIntentService;
import com.runnersudl.services.NetworkChangeReceiver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class RutasFragment extends Fragment implements OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener{

    // Root View
    private View rootView;

    // Maps
    GoogleMap myMap;
    static MapView mMapView;
    static TextView no_connection;

    // CREAR RUTA
    List<LatLng> points = new ArrayList<>();
    Polyline polyline;
    Marker startPoint;

    // LISTADO DE RUTASS
    List<Ruta> listadoRutas = new ArrayList<>();

    // Floating Buttons Menu
    FloatingActionMenu materialDesignFAM;
    FloatingActionButton FAM_crearRuta, FAM_verRutas;

    // Actual ruta
    Ruta actualRuta;
    Polyline actualReto_polyline;
    private TextView actualReto_descripcion, info, activityText;
    private Button comenzarBoton;

    private Marker marker;
    private Marker poly_marker;

    int actualPoint = 1;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private Boolean mRequestingLocationUpdates;


    // SharedPreferences
    SharedPreferences prefs;

    boolean rutaEmpezada;

    private static CountDownTimer cd;
    long timeLeft, initialTime;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            rootView = inflater.inflate(R.layout.fragment_rutas, container, false);
            MapsInitializer.initialize(Objects.requireNonNull(this.getActivity()));
            mMapView = (MapView) rootView.findViewById(R.id.map);
            no_connection = (TextView) rootView.findViewById(R.id.no_connection);

            //actualReto_titulo = (TextView) rootView.findViewById(R.id.titulo);
            actualReto_descripcion = (TextView) rootView.findViewById(R.id.descripcion);
            info = (TextView) rootView.findViewById(R.id.info);
            activityText = (TextView) rootView.findViewById(R.id.activity_recognition);

            comenzarBoton = (Button) rootView.findViewById(R.id.button);

            comenzarBoton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startUpTimer();
                    info.setText("¡Corre!");
                    rutaEmpezada = true;
                    CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(actualRuta.points.get(0))
                                    .bearing(45)
                                    .tilt(90)
                                    .zoom(myMap.getCameraPosition().zoom)
                                    .build();

                    setPolyline(false, true);

                    myMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            1,
                            new GoogleMap.CancelableCallback() {

                                @Override
                                public void onFinish() {
                                }

                                @Override
                                public void onCancel() {
                                }
                            }
                    );

                    Log.d("", "Click");
                }
            });

            mMapView.onCreate(savedInstanceState);
            mMapView.getMapAsync(this);

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            mSettingsClient = LocationServices.getSettingsClient(getActivity());

            mRequestingLocationUpdates = false;

            // Kick off the process of building the LocationCallback, LocationRequest, and
            // LocationSettingsRequest objects.
            createLocationCallback();
            createLocationRequest();
            buildLocationSettingsRequest();

        } catch (InflateException ignored) {
        }

        setRetainInstance(true);

        materialDesignFAM = (FloatingActionMenu) rootView.findViewById(R.id.material_design_android_floating_action_menu);
        FAM_crearRuta = (FloatingActionButton) rootView.findViewById(R.id.material_design_floating_action_menu_item1);
        FAM_verRutas = (FloatingActionButton) rootView.findViewById(R.id.material_design_floating_action_menu_item2);

        // AlertDialog crear reto
        FAM_crearRuta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                materialDesignFAM.close(true);

                final Dialog dialog = new Dialog(getActivity());

                dialog.setContentView(R.layout.dialog_crear_ruta); //your custom content

                MapView mMapView = (MapView) dialog.findViewById(R.id.map);
                final EditText titulo = (EditText) dialog.findViewById(R.id.titulo);
                final EditText desc = (EditText) dialog.findViewById(R.id.descripcion);

                MapsInitializer.initialize(getActivity());
                mMapView.onCreate(dialog.onSaveInstanceState());
                mMapView.onResume();

                // Permitir al usuario crear una ruta haciendo click en diferentes puntos del mapa.
                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                            @Override
                            public void onMapClick(LatLng point) {
                                points.add(point);

                                if (polyline != null) polyline.remove();
                                if (startPoint != null) startPoint.remove();

                                if (points.size() != 1) {
                                    polyline = googleMap.addPolyline(new PolylineOptions()
                                            .color(Color.BLUE)
                                            .width(20)
                                            .clickable(false)
                                            .addAll(points));

                                    polyline.setStartCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start_cap), 20));
                                    polyline.setEndCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.end_cap), 300));
                                    polyline.setJointType(JointType.DEFAULT);
                                    polyline.setPattern(null);
                                } else {
                                    startPoint = googleMap.addMarker(new MarkerOptions().position(points.get(0)).title("Start"));
                                    startPoint.showInfoWindow();
                                }
                            }
                        });

                        if (mCurrentLocation != null)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())));
                        googleMap.getUiSettings().setZoomControlsEnabled(true);
                        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                    }
                });

                LottieAnimationView cancelButton = dialog.findViewById(R.id.cancelar);
                LottieAnimationView guardarButton = dialog.findViewById(R.id.guardar);

                // Boton cancelar, cerrar AlertDialog
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        points.clear();
                        dialog.dismiss();
                    }
                });

                // Boton guardar, guardar reto
                guardarButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add a new document with a generated ID
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> ruta = new Ruta(titulo.getText().toString(), desc.getText().toString(), points).toHashMap();

                        db.collection("Retos")
                                .add(ruta)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding document", e);
                                    }
                                });
                        Toast.makeText(getContext(), getString(R.string.ruta_creada), Toast.LENGTH_LONG).show();
                        points.clear();
                        dialog.dismiss();
                    }
                });
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        // AlertDialog listar retos
        FAM_verRutas.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                materialDesignFAM.close(true);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_ver_rutas, null);

                RecyclerView recyclerView;
                RecyclerView.Adapter mAdapter;
                RecyclerView.LayoutManager layoutManager;

                recyclerView = (RecyclerView) dialogView.findViewById(R.id.recyclerView);
                recyclerView.setHasFixedSize(true);

                View loading = dialogView.findViewById(R.id.loading);
                loading.setVisibility(View.VISIBLE);

                builder.setView(dialogView);
                final AlertDialog dialog = builder.create();

                // use a linear layout manager
                layoutManager = new LinearLayoutManager(getActivity());
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

                mAdapter = new VerRutasRecyclerView(listadoRutas, new VerRutasRecyclerView.MyAdapterListener() {
                    @Override
                    public void retoOnClick(View v, int position) {
                        setReto(listadoRutas.get(position));
                        dialog.cancel();
                    }
                });

                // Add a new document with a generated ID
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("Retos")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        listadoRutas.add(Ruta.fromHashMapToRuta(document.getData(), document.getId().toString()));
                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                    }
                                    loading.setVisibility(View.INVISIBLE);
                                    mAdapter.notifyDataSetChanged();
                                } else {
                                    Log.w(TAG, "Error getting documents.", task.getException());
                                }
                            }
                        });

                recyclerView.setAdapter(mAdapter);

                LottieAnimationView cancelar = dialogView.findViewById(R.id.cancelar);

                cancelar.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        initActivityRecognition();

        return rootView;
    }

    // Crear nuevo reto y añadirlo a la lista de retos
    private void setReto(Ruta ruta) {
        //actualReto_titulo.setText(reto.name);
        actualReto_descripcion.setText(ruta.descripcion);
        actualRuta = ruta;
        if (rutaEmpezada) {
            cd.cancel();
            comenzarBoton.setVisibility(View.INVISIBLE);
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(actualRuta.getMiddlePoint(), 15));
        }

        setPolyline(true, false);
    }

    private void setPolyline(boolean animate, boolean onRace) {
        if (actualReto_polyline != null) actualReto_polyline.remove();
        if (poly_marker != null) poly_marker.remove();

        actualReto_polyline = myMap.addPolyline(new PolylineOptions()
                .color(Color.BLUE)
                .width(20)
                .clickable(false)
                .addAll(actualRuta.points));

        actualReto_polyline.setStartCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start_cap), 20));
        if (onRace) {
            actualReto_polyline.setEndCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start_cap), 20));
            if (poly_marker != null) poly_marker.remove();
            int height = 100;
            int width = 100;
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.end_cap);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
            poly_marker = myMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).position(actualRuta.points.get(actualRuta.points.size() - 1)));
        } else
            actualReto_polyline.setEndCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.end_cap), 300));
        actualReto_polyline.setJointType(JointType.DEFAULT);
        actualReto_polyline.setPattern(null);

        if (animate)
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(actualRuta.getMiddlePoint(), 15));
    }


    // OnPause -> Guardar SharedPreferences
    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("listadoRutas", new Gson().toJson(listadoRutas));
        editor.putString("actualRuta", new Gson().toJson(actualRuta));
        editor.apply();

        // Activity recognition
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(this);

        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        UiSettings uiSettings = myMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(false);

        myMap.setPadding(0, 0, this.getResources().getDisplayMetrics().widthPixels - 150, 150);

        String json_actualReto = prefs.getString("actualReto", "");
        Type type = new TypeToken<Ruta>() {
        }.getType();
        if (!json_actualReto.isEmpty()) actualRuta = new Gson().fromJson(json_actualReto, type);

        if (actualRuta != null) setReto(actualRuta);
    }



    /*----------------------------------------------------------------------------------------------
     *                                          LOCATION
     *              Get user's actual location and show it on the map using a Marker
     *--------------------------------------------------------------------------------------------*/


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        // Here, we resume receiving location updates if the user has requested them.
        if (checkPermissions()) {
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        // Activity Recognition
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sPref = sharedPrefs.getString("listPref", "Wi-Fi");

        //SharedPreferences
        String json = prefs.getString("listadoRutas","");
        Type type = new TypeToken<List<Ruta>>() {}.getType();
        if (!json.isEmpty()) listadoRutas = new Gson().fromJson(json, type);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

     // Creates a callback for receiving location events.
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateUI();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                mRequestingLocationUpdates = false;
                                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                        updateUI();
                    }
                });
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(getString(R.string.permisos_necesarios),
                    "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    //Callback received when a permissions request has been completed.

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(getString(R.string.permiso_denegado),
                        "Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void showSnackbar(String mainTextStringId, String actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                getActivity().findViewById(android.R.id.content),
                mainTextStringId,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(actionStringId, listener).show();
    }

    private void updateUI() {
        if (marker != null) marker.remove();
        if (mCurrentLocation != null && myMap != null)
            marker = myMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.pointer)).position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())).title("Localización actual"));
        if (mCurrentLocation != null && myMap != null && actualRuta != null) {
            Log.d("", rutaEmpezada + " " + " Actual: " + mCurrentLocation.getLatitude() + " " + mCurrentLocation.getLongitude() + " Reto: " + actualRuta.points.get(actualPoint).latitude + " " + actualRuta.points.get(actualPoint).longitude);

            // Ha llegado al punto inicial
            if (!rutaEmpezada && Math.abs(mCurrentLocation.getLatitude() - actualRuta.points.get(0).latitude) < 0.0002 && Math.abs(mCurrentLocation.getLongitude() - actualRuta.points.get(0).longitude) < 0.0002) {
                info.setText("Haz click en 'Comenzar reto'");
                comenzarBoton.setVisibility(View.VISIBLE);
            } else if (!rutaEmpezada) {
                comenzarBoton.setVisibility(View.INVISIBLE);
                info.setText("Muévete hacia el punto inicial");
            }
            if (rutaEmpezada && Math.abs(mCurrentLocation.getLatitude() - actualRuta.points.get(actualPoint).latitude) < 0.0002 && Math.abs(mCurrentLocation.getLongitude() - actualRuta.points.get(actualPoint).longitude) < 0.0002) {

                // Ha llegado a la meta
                if (actualPoint + 1 == actualRuta.points.size()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create(); //Read Update
                    alertDialog.setTitle("¡Enhorabuena!");
                    alertDialog.setMessage("Has finalizado el reto en " + comenzarBoton.getText() + " minutos.");
                    cd.cancel();
                    comenzarBoton.setVisibility(View.INVISIBLE);
                    actualReto_polyline.remove();
                    //
                    // actualReto_titulo.setText("No hay ningun reto seleccionado");
                    actualReto_descripcion.setText("Selecciona un reto en el menu");
                    alertDialog.setButton("Vale", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // here you can add functions
                        }
                    });

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference mRef =  database.getReference().child("Users data").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Retos Completados");
                    mRef.child(String.valueOf(System.currentTimeMillis())).child(actualRuta.id).setValue(timeLeft);
                    mRef.push();

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("Retos").document(actualRuta.id).collection("CompletadoPor").add(FirebaseAuth.getInstance().getUid());
                    actualRuta = null;
                    alertDialog.show();

                    // Ha llegado a un punto intermedio
                } else {
                    Log.d("", String.valueOf(actualPoint + 1) + " " + actualRuta.points.size());
                    CameraPosition cameraPosition =
                            new CameraPosition.Builder()
                                    .target(actualRuta.points.get(actualPoint + 1))
                                    .bearing(45)
                                    .tilt(90)
                                    .zoom(myMap.getCameraPosition().zoom)
                                    .build();
                    Log.d("", cameraPosition.target.toString());
                    myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1, null);

                    actualPoint++;
                }
            }
        }
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    // Cronometro
    // Cuando el jugador no activa el control de tiempo, el tiempo va hacia arriba
    public void startUpTimer() {
        //pendiente
    }

    // ACtivity recognition
    private Context mContext;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    //Define an ActivityRecognitionClient//

    private ActivityRecognitionClient mActivityRecognitionClient;

    void initActivityRecognition() {
        mContext = getContext();

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
                        DETECTED_ACTIVITY, ""));
        mActivityRecognitionClient = new ActivityRecognitionClient(getContext());
        Log.d("Activity Recognition", detectedActivities.toString());
        requestUpdatesHandler();
    }

    public void requestUpdatesHandler() {
        //Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }

    //Get a PendingIntent
    private PendingIntent getActivityDetectionPendingIntent() {
        //Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(getContext(), ActivityIntentService.class);
        return PendingIntent.getService(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        Log.d("Activity Recognition", detectedActivities.toString());
        activityText.setText(getMostConfidentActivity(detectedActivities));

        if (rutaEmpezada && (activityText.getText() == "Estás en coche" || activityText.getText() == "Estás en bici")) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create(); //Read Update
            alertDialog.setTitle("¡Estás haciendo trampas! >:( ");
            alertDialog.setMessage("Mientras estás realizando un reto, no puedes ir en bici o en cualquier otro vehículo. El reto ha sido desactivado.");
            cd.cancel();
            comenzarBoton.setVisibility(View.INVISIBLE);
            actualReto_polyline.remove();
            //actualReto_titulo.setText("No hay ningun reto seleccionado");
            actualReto_descripcion.setText("Selecciona un reto en el menu");
            actualRuta = null;
            alertDialog.setButton("Vale", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // here you can add functions
                }
            });

            alertDialog.show();
        }
    }

    String getMostConfidentActivity(ArrayList<DetectedActivity> detectedActivities) {
        String mostConfidentActivity = "";
        int maxConfidence = 0;
        for (DetectedActivity activity : detectedActivities) {
            if (activity.getConfidence() > maxConfidence) {
                mostConfidentActivity = ActivityIntentService.getActivityString(getContext(), activity.getType());
                maxConfidence = activity.getConfidence();
            }
        }
        return mostConfidentActivity;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }


    // Comprobando la conexion
    // The user's current network preference setting.
    public static String sPref = null;

    public static void checkNetwork(String value){

        if (((sPref.equals("Cualquiera")) && (value.equals("WiFi") || value.equals("Mobl"))) || ((sPref.equals("Wi-Fi")) && (value.equals("WiFi")))) {
            mMapView.setVisibility(View.VISIBLE);
            no_connection.setVisibility(View.INVISIBLE);

            Handler handler = new Handler();
            Runnable delayrunnable = new Runnable() {
                @Override
                public void run() {
                    mMapView.setVisibility(View.VISIBLE);
                    no_connection.setVisibility(View.GONE);
                }
            };
            handler.postDelayed(delayrunnable, 3000);
        }else {
            mMapView.setVisibility(View.INVISIBLE);
            no_connection.setVisibility(View.VISIBLE);
        }
    }
}
