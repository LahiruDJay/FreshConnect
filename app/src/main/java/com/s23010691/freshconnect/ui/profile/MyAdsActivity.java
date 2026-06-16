package com.s23010691.freshconnect.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;
import com.s23010691.freshconnect.ui.home.ProductAdapter;

import java.util.ArrayList;
import java.util.List;

/*
 * My Ads Activity
 * Displays a grid of products that the currently logged-in user has listed for sale.
 */
public class MyAdsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvMyAds;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;
    private ProductAdapter adapter;
    private final List<Product> myProductsList = new ArrayList<>();

    /*
     * Initializes the activity, sets up the RecyclerView, and loads the user's products.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_ads);

        supabaseClient = new SupabaseClient();

        initViews();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());
    }

    /*
     * Binds XML layout elements to Java variables.
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvMyAds = findViewById(R.id.rvMyAds);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    /*
     * Configures the RecyclerView and attaches the ProductAdapter.
     */
    private void setupRecyclerView() {
        adapter = new ProductAdapter(myProductsList, product -> {
            Intent intent = new Intent(MyAdsActivity.this, EditAdActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });

        rvMyAds.setLayoutManager(new GridLayoutManager(this, 2));
        rvMyAds.setAdapter(adapter);
    }

    /*
     * Refreshes the list of products whenever the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadMyProducts();
    }

    /*
     * Fetches products created by the current user from the Supabase backend.
     */
    private void loadMyProducts() {
        progressBar.setVisibility(View.VISIBLE);
        rvMyAds.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        String currentUserId = SupabaseClient.currentUserId != null ? SupabaseClient.currentUserId : "guest";

        supabaseClient.fetchProductsByUser(currentUserId, new SupabaseClient.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                progressBar.setVisibility(View.GONE);
                myProductsList.clear();
                if (products != null && !products.isEmpty()) {
                    myProductsList.addAll(products);
                    adapter.notifyDataSetChanged();
                    rvMyAds.setVisibility(View.VISIBLE);
                    llEmptyState.setVisibility(View.GONE);
                } else {
                    rvMyAds.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyAdsActivity.this, "Failed to load listings: " + error, Toast.LENGTH_LONG).show();
                if (myProductsList.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
