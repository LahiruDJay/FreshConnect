package com.s23010691.freshconnect.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.View;

import com.google.gson.JsonObject;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Product Details Activity
 * Displays comprehensive details about a selected product, including images, seller info, and options to bookmark or contact the seller.
 */
public class ProductDetailsActivity extends AppCompatActivity {

    private ViewPager2 vpProductImages;
    private TextView tvImageIndicator;
    private ImageView ivDetailImage;
    private TextView tvDetailCategory;
    private TextView tvDetailName;
    private TextView tvDetailPrice;
    private TextView tvDetailUnit;
    private TextView tvDetailQty;
    private TextView tvDetailPostedOn;
    private TextView tvDetailDescription;
    
    // Seller Info
    private ImageView ivSellerAvatar;
    private TextView tvSellerName;
    private TextView tvSellerJoined;
    
    private ImageButton btnBack;
    private ImageButton btnBookmark;
    private Button btnViewOnMap;
    private Button btnContactSeller;

    private OkHttpClient httpClient;
    private SupabaseClient supabaseClient;

    /*
     * Initializes the activity, sets the layout, and loads the product details passed via intent.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        httpClient = new OkHttpClient();
        supabaseClient = new SupabaseClient();

        // Retrieve product from Intent
        Product product = (Product) getIntent().getSerializableExtra("PRODUCT");
        if (product == null) {
            Toast.makeText(this, "Product details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindProductData(product);
        setupListeners(product);
        fetchSellerDetails(product.user_id);
    }

    /*
     * Binds XML layout elements to Java variables.
     */
    private void initViews() {
        vpProductImages = findViewById(R.id.vpProductImages);
        tvImageIndicator = findViewById(R.id.tvImageIndicator);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailUnit = findViewById(R.id.tvDetailUnit);
        tvDetailQty = findViewById(R.id.tvDetailQty);
        tvDetailPostedOn = findViewById(R.id.tvDetailPostedOn);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        
        ivSellerAvatar = findViewById(R.id.ivSellerAvatar);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerJoined = findViewById(R.id.tvSellerJoined);
        
        btnBack = findViewById(R.id.btnBack);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        btnContactSeller = findViewById(R.id.btnContactSeller);
    }

    /*
     * Populates the UI fields with the specific product's data.
     * Parameters:
     *   - product: The product object containing data to display.
     */
    private void bindProductData(Product product) {
        tvDetailCategory.setText(product.category != null ? product.category : "Vegetable");
        tvDetailName.setText(product.name);
        
        // Format price without trailing decimals if it's a whole number
        if (product.price == (long) product.price) {
            tvDetailPrice.setText(String.format("Rs. %d", (long) product.price));
        } else {
            tvDetailPrice.setText(String.format("Rs. %.2f", product.price));
        }
        
        tvDetailUnit.setText(product.unit != null ? " / " + product.unit : " / kg");
        
        // Append unit if not already included in quantity
        String qtyText = product.quantity != null ? product.quantity : "N/A";
        String unitText = product.unit != null ? product.unit : "kg";
        if (product.quantity != null && !qtyText.toLowerCase().contains(unitText.toLowerCase())) {
            qtyText += " " + unitText;
        }
        tvDetailQty.setText(qtyText);
        
        tvDetailPostedOn.setText(formatPostedDate(product.created_at));
        String displayDesc = product.description;
        if (displayDesc != null) {
            displayDesc = displayDesc.replaceAll("\\s*\\[SellerInfo:[^\\]]*\\]", "");
        }
        tvDetailDescription.setText(displayDesc != null && !displayDesc.trim().isEmpty() 
                ? displayDesc : "No description provided.");

        // Load images via ViewPager2
        ivDetailImage.setImageResource(R.drawable.food); // fallback
        if (product.image_urls != null && product.image_urls.length > 0) {
            ivDetailImage.setVisibility(View.GONE);
            vpProductImages.setVisibility(View.VISIBLE);
            
            ImageAdapter adapter = new ImageAdapter(product.image_urls, httpClient);
            vpProductImages.setAdapter(adapter);
            
            if (product.image_urls.length > 1) {
                tvImageIndicator.setVisibility(View.VISIBLE);
                tvImageIndicator.setText("1/" + product.image_urls.length);
                vpProductImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        tvImageIndicator.setText((position + 1) + "/" + product.image_urls.length);
                    }
                });
            } else {
                tvImageIndicator.setVisibility(View.GONE);
            }
        } else {
            ivDetailImage.setVisibility(View.VISIBLE);
            vpProductImages.setVisibility(View.GONE);
            tvImageIndicator.setVisibility(View.GONE);
        }
    }

    /*
     * Formats the raw timestamp into a readable date string.
     * Parameters:
     *   - rawDate: The ISO format date string.
     */
    private String formatPostedDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "Recent";
        try {
            // rawDate format: 2026-06-04T12:00:00...
            String datePart = rawDate.split("T")[0];
            String[] parts = datePart.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            String[] months = {"", "January", "February", "March", "April", "May", "June", 
                    "July", "August", "September", "October", "November", "December"};
            return day + " " + months[month] + " " + year;
        } catch (Exception e) {
            return "Recent";
        }
    }

    /*
     * Fetches details of the seller who listed the product and displays them.
     * Parameters:
     *   - sellerId: The UUID of the seller.
     */
    private void fetchSellerDetails(String sellerId) {
        if (sellerId != null && sellerId.equals(SupabaseClient.currentUserId)) {
            // Load current user's profile details from SharedPreferences
            String userEmail = SupabaseClient.currentUserEmail != null ? SupabaseClient.currentUserEmail : "guest@freshconnect.com";
            android.content.SharedPreferences sharedPreferences = getSharedPreferences("FreshConnectProfile", MODE_PRIVATE);
            String cachedName = sharedPreferences.getString("name_" + userEmail, "FreshConnect User");
            String cachedJoined = sharedPreferences.getString("joined_" + userEmail, "Joined in 2026");
            String cachedPicUrl = sharedPreferences.getString("pic_url_" + userEmail, null);

            tvSellerName.setText(cachedName);
            tvSellerJoined.setText(cachedJoined);

            if (cachedPicUrl != null && !cachedPicUrl.isEmpty()) {
                Request request = new Request.Builder().url(cachedPicUrl).build();
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                            if (bitmap != null) {
                                runOnUiThread(() -> ivSellerAvatar.setImageBitmap(bitmap));
                            }
                        }
                    }
                });
            } else {
                ivSellerAvatar.setImageResource(R.drawable.ic_person);
            }
            return;
        }

        if (sellerId == null || sellerId.isEmpty()) {
            tvSellerName.setText("Nimal Jayasinghe");
            tvSellerJoined.setText("Joined in 2023");
            ivSellerAvatar.setImageResource(R.drawable.ic_person);
            return;
        }

        // Query the database to retrieve actual user details by uuid
        supabaseClient.fetchUserDetailsById(sellerId, new SupabaseClient.UserCallback() {
            @Override
            public void onSuccess(JsonObject userJson) {
                String name = userJson.has("name") && !userJson.get("name").isJsonNull() 
                        ? userJson.get("name").getAsString() : "FreshConnect User";
                
                String joined = "Joined in 2026";
                if (userJson.has("created_at") && !userJson.get("created_at").isJsonNull()) {
                    try {
                        String rawDate = userJson.get("created_at").getAsString();
                        String year = rawDate.split("-")[0];
                        joined = "Joined in " + year;
                    } catch (Exception e) {}
                }
                
                final String sellerName = name;
                final String sellerJoined = joined;
                final String picUrl = userJson.has("profile_pic") && !userJson.get("profile_pic").isJsonNull() 
                        ? userJson.get("profile_pic").getAsString() : null;

                runOnUiThread(() -> {
                    tvSellerName.setText(sellerName);
                    tvSellerJoined.setText(sellerJoined);
                    
                    if (picUrl != null && !picUrl.isEmpty()) {
                        Request request = new Request.Builder().url(picUrl).build();
                        httpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> ivSellerAvatar.setImageResource(R.drawable.ic_person));
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful() && response.body() != null) {
                                    final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                                    if (bitmap != null) {
                                        runOnUiThread(() -> ivSellerAvatar.setImageBitmap(bitmap));
                                    } else {
                                        runOnUiThread(() -> ivSellerAvatar.setImageResource(R.drawable.ic_person));
                                    }
                                } else {
                                    runOnUiThread(() -> ivSellerAvatar.setImageResource(R.drawable.ic_person));
                                }
                            }
                        });
                    } else {
                        ivSellerAvatar.setImageResource(R.drawable.ic_person);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Statically fallback to mockup or default if query fails
                    tvSellerName.setText("Nimal Jayasinghe");
                    tvSellerJoined.setText("Joined in 2023");
                    ivSellerAvatar.setImageResource(R.drawable.ic_person);
                });
            }
        });
    }

    private boolean isProductBookmarked = false;

    /*
     * Updates the UI state of the bookmark button based on whether the product is saved.
     * Parameters:
     *   - isBookmarked: True if the product is bookmarked, false otherwise.
     */
    private void updateBookmarkButtonState(boolean isBookmarked) {
        this.isProductBookmarked = isBookmarked;
        if (isBookmarked) {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
        } else {
            btnBookmark.setImageResource(R.drawable.ic_bookmark);
            btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
        }
    }

    /*
     * Sets up click listeners for the interactive buttons on the screen.
     * Parameters:
     *   - product: The product being viewed.
     */
    private void setupListeners(Product product) {
        btnBack.setOnClickListener(v -> finish());
        
        if (product.id != null) {
            supabaseClient.checkBookmarkStatus(product.id, new SupabaseClient.BookmarkCheckCallback() {
                @Override
                public void onSuccess(boolean isBookmarked) {
                    updateBookmarkButtonState(isBookmarked);
                }

                @Override
                public void onError(String error) {
                    updateBookmarkButtonState(false);
                }
            });
        } else {
            updateBookmarkButtonState(false);
        }

        btnBookmark.setOnClickListener(v -> {
            if (product.id == null) {
                Toast.makeText(this, "Cannot bookmark this product (Missing ID)", Toast.LENGTH_SHORT).show();
                return;
            }

            btnBookmark.setEnabled(false);
            if (isProductBookmarked) {
                supabaseClient.removeBookmark(product.id, new SupabaseClient.InsertCallback() {
                    @Override
                    public void onSuccess() {
                        btnBookmark.setEnabled(true);
                        updateBookmarkButtonState(false);
                        Toast.makeText(ProductDetailsActivity.this, "Product removed from bookmarks", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        btnBookmark.setEnabled(true);
                        Toast.makeText(ProductDetailsActivity.this, "Failed to remove bookmark: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                supabaseClient.addBookmark(product.id, new SupabaseClient.InsertCallback() {
                    @Override
                    public void onSuccess() {
                        btnBookmark.setEnabled(true);
                        updateBookmarkButtonState(true);
                        Toast.makeText(ProductDetailsActivity.this, "Product saved to bookmarks", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        btnBookmark.setEnabled(true);
                        Toast.makeText(ProductDetailsActivity.this, "Failed to bookmark product: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnViewOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailsActivity.this, ProductDirectionActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });

        btnContactSeller.setOnClickListener(v -> {
            if (product.user_id == null || product.user_id.equals("guest")) {
                Toast.makeText(this, "Cannot contact a guest seller", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (product.user_id.equals(SupabaseClient.currentUserId)) {
                Toast.makeText(this, "This is your own product", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ProductDetailsActivity.this, com.s23010691.freshconnect.ui.ChatRoomActivity.class);
            intent.putExtra("other_user_id", product.user_id);
            startActivity(intent);
        });
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private final String[] imageUrls;
        private final OkHttpClient httpClient;

        /*
         * Constructor for ImageAdapter.
         * Parameters:
         *   - imageUrls: Array of URLs for the product images.
         *   - httpClient: HTTP client to fetch the images.
         */
        ImageAdapter(String[] imageUrls, OkHttpClient httpClient) {
            this.imageUrls = imageUrls;
            this.httpClient = httpClient;
        }

        /*
         * Inflates an ImageView for the ViewPager to display an image.
         */
        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        /*
         * Binds the image URL to the ViewHolder to load it.
         */
        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            holder.bind(imageUrls[position], httpClient);
        }

        /*
         * Returns the number of images.
         */
        @Override
        public int getItemCount() {
            return imageUrls != null ? imageUrls.length : 0;
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            /*
             * Constructor for ImageViewHolder.
             */
            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            /*
             * Fetches the image from the URL and sets it to the ImageView.
             */
            void bind(String imageUrl, OkHttpClient client) {
                ImageView iv = (ImageView) itemView;
                iv.setImageResource(R.drawable.food); // fallback
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Request request = new Request.Builder().url(imageUrl).build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful() && response.body() != null) {
                                final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                                if (bitmap != null) {
                                    iv.post(() -> iv.setImageBitmap(bitmap));
                                }
                            }
                        }
                    });
                  }
              }
          }
      }
  }
