package com.s23010691.freshconnect.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Product Direction Activity
 * Shows a map with a route drawn from the user's current location to the selected product's location.
 */
public class ProductDirectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvDirectionProductName;
    private TextView tvDirectionDistance;
    private TextView tvDirectionAddress;
    private ImageButton btnBack;

    private Product product;
    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || 
                    (coarseLocationGranted != null && coarseLocationGranted)) {
                    drawRouteWithUserLocation();
                } else {
                    Toast.makeText(this, "Location permission denied. Map shown without current location.", Toast.LENGTH_LONG).show();
                    drawProductMarkerOnly();
                }
            });

    /*
     * Initializes the activity, sets up the MapFragment, and requests location services.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_direction);

        product = (Product) getIntent().getSerializableExtra("PRODUCT");
        if (product == null) {
            Toast.makeText(this, "Product details missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();

        initViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.directionMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /*
     * Binds XML layout elements to Java variables.
     */
    private void initViews() {
        tvDirectionProductName = findViewById(R.id.tvDirectionProductName);
        tvDirectionDistance = findViewById(R.id.tvDirectionDistance);
        tvDirectionAddress = findViewById(R.id.tvDirectionAddress);
        btnBack = findViewById(R.id.btnBack);

        tvDirectionProductName.setText(product.name);
        tvDirectionAddress.setText("Address: " + (product.location_name != null ? product.location_name : "N/A"));

        btnBack.setOnClickListener(v -> finish());
    }

    /*
     * Callback invoked when the Google Map is ready for manipulation.
     * Parameters:
     *   - googleMap: The ready GoogleMap instance.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkPermissionsAndLoadMap();
    }

    /*
     * Checks if location permissions are granted; if so, draws route, else requests them.
     */
    private void checkPermissionsAndLoadMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            drawRouteWithUserLocation();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /*
     * Retrieves user location, plots marker for the product, and initiates route drawing.
     */
    private void drawRouteWithUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            LatLng productLatLng = new LatLng(product.latitude, product.longitude);

            // Add Product Marker
            mMap.addMarker(new MarkerOptions()
                    .position(productLatLng)
                    .title(product.name)
                    .snippet(product.location_name));

            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Calculate distance
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                        product.latitude, product.longitude, results);
                float distanceInMeters = results[0];
                float distanceInKm = distanceInMeters / 1000f;
                
                tvDirectionDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km away", distanceInKm));

                // Fetch road directions path
                fetchRoadDirections(userLatLng, productLatLng);
            } else {
                // User location is null (GPS off), zoom to product
                tvDirectionDistance.setText("GPS signal weak. Enable location services.");
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(productLatLng, 15));
            }
        }).addOnFailureListener(e -> {
            drawProductMarkerOnly();
        });
    }

    /*
     * Calls Google Directions API to fetch the road route between two points.
     * Parameters:
     *   - origin: The starting LatLng coordinates.
     *   - destination: The ending LatLng coordinates.
     */
    private void fetchRoadDirections(LatLng origin, LatLng destination) {
        String apiKey = "";
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            apiKey = bundle.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            apiKey = "AIzaSyBlS8bTR9Q-olpg6wQ0kgeHN8FUwAMjaig"; // fallback
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    drawStraightLine(origin, destination);
                    zoomToFitMarkers(origin, destination);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                        String status = jsonObject.get("status").getAsString();
                        
                        if ("OK".equals(status)) {
                            JsonArray routes = jsonObject.getAsJsonArray("routes");
                            JsonObject route = routes.get(0).getAsJsonObject();
                            JsonObject overviewPolyline = route.getAsJsonObject("overview_polyline");
                            String encodedPoints = overviewPolyline.get("points").getAsString();
                            
                            List<LatLng> points = decodePoly(encodedPoints);
                            runOnUiThread(() -> {
                                drawRoadRoute(points);
                                zoomToFitMarkers(origin, destination);
                            });
                        } else {
                            runOnUiThread(() -> {
                                drawStraightLine(origin, destination);
                                zoomToFitMarkers(origin, destination);
                            });
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            drawStraightLine(origin, destination);
                            zoomToFitMarkers(origin, destination);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        drawStraightLine(origin, destination);
                        zoomToFitMarkers(origin, destination);
                    });
                }
            }
        });
    }

    /*
     * Draws a polyline on the map representing the road route.
     * Parameters:
     *   - points: The list of LatLng points forming the route.
     */
    private void drawRoadRoute(List<LatLng> points) {
        if (mMap == null || points == null || points.isEmpty()) return;
        mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(12)
                .color(Color.parseColor("#10B981")) // Emerald green line matching design
                .geodesic(true));
    }

    /*
     * Draws a direct straight line between origin and destination as a fallback.
     * Parameters:
     *   - origin: Starting point.
     *   - destination: Ending point.
     */
    private void drawStraightLine(LatLng origin, LatLng destination) {
        if (mMap == null) return;
        mMap.addPolyline(new PolylineOptions()
                .add(origin, destination)
                .width(10)
                .color(Color.parseColor("#10B981"))
                .geodesic(true));
    }

    /*
     * Adjusts the map camera to make sure both the user location and product are visible.
     * Parameters:
     *   - origin: Starting point.
     *   - destination: Ending point.
     */
    private void zoomToFitMarkers(LatLng origin, LatLng destination) {
        if (mMap == null) return;
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(origin);
        boundsBuilder.include(destination);
        LatLngBounds bounds = boundsBuilder.build();
        int padding = 200; // pixels padding from edges of the map
        
        mMap.setOnMapLoadedCallback(() -> {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cameraUpdate);
        });
    }

    /*
     * Decodes the encoded polyline string from the Directions API into LatLng coordinates.
     * Parameters:
     *   - encoded: The polyline string.
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    /*
     * Fallback to just show the product marker without a route if location is unavailable.
     */
    private void drawProductMarkerOnly() {
        LatLng productLatLng = new LatLng(product.latitude, product.longitude);
        mMap.addMarker(new MarkerOptions()
                .position(productLatLng)
                .title(product.name)
                .snippet(product.location_name));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(productLatLng, 15));
        tvDirectionDistance.setText("Enable GPS to calculate distance.");
    }
}
