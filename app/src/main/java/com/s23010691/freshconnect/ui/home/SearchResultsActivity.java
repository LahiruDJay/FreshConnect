package com.s23010691.freshconnect.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

/*
 * Search Results Activity
 * Displays a grid of products that match a specific search query entered by the user.
 */
public class SearchResultsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvToolbarTitle;
    private RecyclerView rvSearchResults;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;
    private TextView tvEmptyDesc;

    private SupabaseClient supabaseClient;
    private ProductAdapter adapter;
    private final List<Product> searchProductsList = new ArrayList<>();
    private String searchQuery = "";

    /*
     * Initializes the activity, extracts the search query, and triggers the search process.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        supabaseClient = new SupabaseClient();

        searchQuery = getIntent().getStringExtra("QUERY");
        if (searchQuery == null) {
            searchQuery = "";
        }

        initViews();
        setupRecyclerView();

        tvToolbarTitle.setText(searchQuery);
        btnBack.setOnClickListener(v -> finish());

        performSearch();
    }

    /*
     * Binds XML layout elements to Java variables.
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);
    }

    /*
     * Configures the RecyclerView and attaches the ProductAdapter.
     */
    private void setupRecyclerView() {
        adapter = new ProductAdapter(searchProductsList, product -> {
            Intent intent = new Intent(SearchResultsActivity.this, ProductDetailsActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });

        rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
        rvSearchResults.setAdapter(adapter);
    }

    /*
     * Executes the search query against the Supabase backend and updates the UI.
     */
    private void performSearch() {
        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        supabaseClient.searchProductsByName(searchQuery, new SupabaseClient.ProductsCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                progressBar.setVisibility(View.GONE);
                searchProductsList.clear();
                if (products != null && !products.isEmpty()) {
                    searchProductsList.addAll(products);
                    adapter.notifyDataSetChanged();
                    rvSearchResults.setVisibility(View.VISIBLE);
                    llEmptyState.setVisibility(View.GONE);
                } else {
                    rvSearchResults.setVisibility(View.GONE);
                    if (tvEmptyDesc != null) {
                        tvEmptyDesc.setText("No items match \"" + searchQuery + "\". Check the spelling and try again.");
                    }
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SearchResultsActivity.this, "Search failed: " + error, Toast.LENGTH_LONG).show();
                if (searchProductsList.isEmpty()) {
                    if (tvEmptyDesc != null) {
                        tvEmptyDesc.setText("Search failed. Please check your connection.");
                    }
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
