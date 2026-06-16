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
import com.s23010691.freshconnect.ui.home.ProductDetailsActivity;

import java.util.ArrayList;
import java.util.List;

/*
 * Saved Items Activity
 * Displays a grid of products that the current user has bookmarked or saved for later.
 */
public class SavedItemsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvSavedItems;
    private LinearLayout llEmptyState;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;
    private ProductAdapter adapter;
    private final List<Product> savedProductsList = new ArrayList<>();

    /*
     * Initializes the activity, views, and prepares the RecyclerView.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_items);

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
        rvSavedItems = findViewById(R.id.rvSavedItems);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    /*
     * Configures the RecyclerView and sets up the click listener to open product details.
     */
    private void setupRecyclerView() {
        adapter = new ProductAdapter(savedProductsList, product -> {
            Intent intent = new Intent(SavedItemsActivity.this, ProductDetailsActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });

        rvSavedItems.setLayoutManager(new GridLayoutManager(this, 2));
        rvSavedItems.setAdapter(adapter);
    }

    /*
     * Refreshes the bookmarked products list when the activity comes to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadSavedItems();
    }

    /*
     * Fetches the user's bookmarked products from the Supabase database.
     */
    private void loadSavedItems() {
        progressBar.setVisibility(View.VISIBLE);
        rvSavedItems.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        supabaseClient.fetchBookmarkedProducts(new SupabaseClient.BookmarksCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                progressBar.setVisibility(View.GONE);
                savedProductsList.clear();
                if (products != null && !products.isEmpty()) {
                    savedProductsList.addAll(products);
                    adapter.notifyDataSetChanged();
                    rvSavedItems.setVisibility(View.VISIBLE);
                    llEmptyState.setVisibility(View.GONE);
                } else {
                    rvSavedItems.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SavedItemsActivity.this, "Failed to load saved items: " + error, Toast.LENGTH_LONG).show();
                if (savedProductsList.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
