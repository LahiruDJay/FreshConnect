package com.s23010691.freshconnect.ui.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Product Adapter
 * Binds product data to the views in the RecyclerView for displaying a grid/list of products.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;
    private final OnProductClickListener clickListener;
    private final OkHttpClient httpClient;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    /*
     * Constructor for ProductAdapter.
     * Parameters:
     *   - products: The list of products to display.
     *   - clickListener: Callback for when a product item is clicked.
     */
    public ProductAdapter(List<Product> products, OnProductClickListener clickListener) {
        this.products = products;
        this.clickListener = clickListener;
        this.httpClient = new OkHttpClient();
    }

    /*
     * Inflates the layout for an individual product item.
     * Parameters:
     *   - parent: The parent view group.
     *   - viewType: The type of the view.
     */
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    /*
     * Binds the specific product data to the view holder.
     * Parameters:
     *   - holder: The view holder to bind data to.
     *   - position: The index of the product in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, clickListener, httpClient);
    }

    /*
     * Returns the total number of products.
     */
    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivProductImage;
        private final TextView tvProductName;
        private final TextView tvProductPrice;
        private final TextView tvProductUnit;

        /*
         * Constructor for the ProductViewHolder.
         * Parameters:
         *   - itemView: The root view of the individual product item.
         */
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductUnit = itemView.findViewById(R.id.tvProductUnit);
        }

        /*
         * Binds product properties to the UI elements and fetches the product image.
         * Parameters:
         *   - product: The product to display.
         *   - listener: Click listener for the item.
         *   - client: OkHttpClient instance to fetch the image.
         */
        public void bind(Product product, OnProductClickListener listener, OkHttpClient client) {
            tvProductName.setText(product.name);
            tvProductPrice.setText(String.format("Rs. %.2f", product.price));
            tvProductUnit.setText(product.unit != null ? " / " + product.unit : "");

            // Reset image view to default first
            ivProductImage.setImageResource(R.drawable.food);

            // Fetch and render product image asynchronously
            if (product.image_urls != null && product.image_urls.length > 0 && product.image_urls[0] != null && !product.image_urls[0].isEmpty()) {
                String imageUrl = product.image_urls[0];
                
                Request request = new Request.Builder().url(imageUrl).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        // Keep fallback image
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                            if (bitmap != null) {
                                // Run update on the main thread
                                ivProductImage.post(() -> ivProductImage.setImageBitmap(bitmap));
                            }
                        }
                    }
                });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}
