package com.s23010691.freshconnect.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

/*
 * Home Fragment
 * Acts as the main landing screen, displaying product categories, a search bar, and a grid of available agricultural products.
 */
public class HomeFragment extends Fragment {

    private RecyclerView rvProducts;
    private ProductAdapter productAdapter;
    private TextView tvUserName;
    
    // Lists to store fetched and filtered products
    private final List<Product> allProductsList = new ArrayList<>();
    private final List<Product> displayedProductsList = new ArrayList<>();
    
    private SupabaseClient supabaseClient;
    private String currentCategory = "All";

    // Category Frame layouts for styling active selections
    private FrameLayout frameAll, frameVegetables, frameFruits, frameHerbs, frameSeeds;
    private List<FrameLayout> categoryFrames;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long mShakeTimestamp;
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;

    /*
     * Inflates the layout for the Home screen, initializes views, and fetches initial data.
     * Parameters:
     *   - inflater: The LayoutInflater to inflate the view.
     *   - container: The parent view to attach the fragment's UI.
     *   - savedInstanceState: Previous saved state.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the home fragment layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        supabaseClient = new SupabaseClient();

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        initViews(view);
        setupRecyclerView();
        setupCategoryFilters(view);
        setupSearch(view);
        fetchLatestUserName();

        // Load all products on startup
        selectCategory("All");

        return view;
    }

    /*
     * Maps UI elements from the layout XML to Java objects for manipulation.
     * Parameters:
     *   - view: The inflated root view of the fragment.
     */
    private void initViews(View view) {
        rvProducts = view.findViewById(R.id.rvProducts);
        tvUserName = view.findViewById(R.id.tvUserName);
        
        frameAll = view.findViewById(R.id.frameAll);
        frameVegetables = view.findViewById(R.id.frameVegetables);
        frameFruits = view.findViewById(R.id.frameFruits);
        frameHerbs = view.findViewById(R.id.frameHerbs);
        frameSeeds = view.findViewById(R.id.frameSeeds);

        categoryFrames = new ArrayList<>();
        categoryFrames.add(frameAll);
        categoryFrames.add(frameVegetables);
        categoryFrames.add(frameFruits);
        categoryFrames.add(frameHerbs);
        categoryFrames.add(frameSeeds);
    }

    /*
     * Configures the RecyclerView with a GridLayoutManager and sets its adapter for products.
     */
    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(displayedProductsList, product -> {
            android.content.Intent intent = new android.content.Intent(getContext(), ProductDetailsActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        
        // Use Grid style span count 2 for product cards
        rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvProducts.setAdapter(productAdapter);
    }

    /*
     * Assigns click listeners to category filter buttons to load respective products.
     * Parameters:
     *   - view: The root view containing the category buttons.
     */
    private void setupCategoryFilters(View view) {
        view.findViewById(R.id.catAll).setOnClickListener(v -> selectCategory("All"));
        view.findViewById(R.id.catVegetables).setOnClickListener(v -> selectCategory("Vegetables"));
        view.findViewById(R.id.catFruits).setOnClickListener(v -> selectCategory("Fruits"));
        view.findViewById(R.id.catHerbs).setOnClickListener(v -> selectCategory("Herbs"));
        view.findViewById(R.id.catSeeds).setOnClickListener(v -> selectCategory("Seeds & Plants"));
    }

    /*
     * Updates the currently selected category, highlights it, and fetches matching products.
     * Parameters:
     *   - category: The name of the category to select.
     */
    private void selectCategory(String category) {
        currentCategory = category;
        highlightSelectedCategory(category);
        fetchProductsFromSupabase(category);
    }

    /*
     * Visually highlights the selected category's background and resets the others.
     * Parameters:
     *   - category: The name of the selected category.
     */
    private void highlightSelectedCategory(String category) {
        // Reset all backgrounds to white
        for (FrameLayout frame : categoryFrames) {
            if (frame != null) {
                frame.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            }
        }

        // Highlight selected category circle with theme light green tint (#E8F5E9)
        FrameLayout selectedFrame = null;
        switch (category) {
            case "All":
                selectedFrame = frameAll;
                break;
            case "Vegetables":
                selectedFrame = frameVegetables;
                break;
            case "Fruits":
                selectedFrame = frameFruits;
                break;
            case "Herbs":
                selectedFrame = frameHerbs;
                break;
            case "Seeds & Plants":
                selectedFrame = frameSeeds;
                break;
        }

        if (selectedFrame != null) {
            selectedFrame.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
        }
    }

    /*
     * Fetches products belonging to a specific category from the Supabase backend.
     * Parameters:
     *   - category: The category string to query for.
     */
    private void fetchProductsFromSupabase(String category) {
        // Clear search input on category change to prevent confusion
        EditText searchBar = getView() != null ? getView().findViewById(R.id.searchBar) : null;
        if (searchBar != null) {
            searchBar.setText("");
        }

        // Fetch filtered items
        // Translate category to database match
        String queryCategory = category;
        if (category.equals("Seeds & Plants")) {
            // Check if user stored Seeds & Plants or just Seeds in spinner
            queryCategory = "Seeds";
            // Wait, we can fetch matching Seeds as category, or if the DB has seeds, we fetch by Seeds
        }

        supabaseClient.fetchProducts(queryCategory, new SupabaseClient.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                if (!isAdded()) return;

                allProductsList.clear();
                allProductsList.addAll(products);

                displayedProductsList.clear();
                displayedProductsList.addAll(products);
                
                productAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load products: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /*
     * Initializes the search bar functionality and input listeners.
     * Parameters:
     *   - view: The root view containing the search elements.
     */
    private void setupSearch(View view) {
        EditText searchBar = view.findViewById(R.id.searchBar);
        View btnSearch = view.findViewById(R.id.btnSearch);
        
        if (searchBar != null && btnSearch != null) {
            btnSearch.setOnClickListener(v -> triggerSearch(searchBar.getText().toString()));
            
            searchBar.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    triggerSearch(searchBar.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    /*
     * Validates the search query and launches the SearchResultsActivity if valid.
     * Parameters:
     *   - query: The text entered by the user to search for.
     */
    private void triggerSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent intent = new android.content.Intent(getContext(), SearchResultsActivity.class);
        intent.putExtra("QUERY", query.trim());
        startActivity(intent);
    }

    /*
     * Fetches the current user's profile details to display their name on the home screen.
     */
    private void fetchLatestUserName() {
        if (getContext() == null || getActivity() == null) return;
        String userEmail = SupabaseClient.currentUserEmail != null ? SupabaseClient.currentUserEmail : "guest@freshconnect.com";
        
        // Load local SharedPreferences cache first
        android.content.SharedPreferences sharedPreferences = getActivity().getSharedPreferences("FreshConnectProfile", android.content.Context.MODE_PRIVATE);
        String cachedName = sharedPreferences.getString("name_" + userEmail, "Guest User");
        if (tvUserName != null) {
            tvUserName.setText(cachedName);
        }

        if (SupabaseClient.currentUserEmail != null) {
            supabaseClient.fetchUserDetails(userEmail, new SupabaseClient.UserCallback() {
                @Override
                public void onSuccess(com.google.gson.JsonObject userJson) {
                    if (!isAdded()) return;
                    String dbName = userJson.has("name") && !userJson.get("name").isJsonNull() 
                            ? userJson.get("name").getAsString() : "Guest User";
                    
                    // Cache values locally
                    sharedPreferences.edit().putString("name_" + userEmail, dbName).apply();

                    // Update UI name
                    if (tvUserName != null) {
                        tvUserName.setText(dbName);
                    }
                }

                @Override
                public void onError(String error) {
                    // Ignore, fallback to local cached value (already rendered)
                }
            });
        }
    }

    /*
     * Called when fragment comes to foreground; refreshes user data and registers shake sensor.
     */
    @Override
    public void onResume() {
        super.onResume();
        fetchLatestUserName();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /*
     * Called when fragment leaves foreground; unregisters the shake sensor to save battery.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                float gX = x / SensorManager.GRAVITY_EARTH;
                float gY = y / SensorManager.GRAVITY_EARTH;
                float gZ = z / SensorManager.GRAVITY_EARTH;
                
                float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
                
                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    final long now = System.currentTimeMillis();
                    if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                        return;
                    }
                    mShakeTimestamp = now;
                    
                    refreshProductList();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No-op
        }
    };

    /*
     * Refreshes the product list when a shake event is successfully detected.
     */
    private void refreshProductList() {
        if (isAdded()) {
            Toast.makeText(getContext(), "Products refreshed!", Toast.LENGTH_SHORT).show();
            selectCategory(currentCategory);
        }
    }
}
