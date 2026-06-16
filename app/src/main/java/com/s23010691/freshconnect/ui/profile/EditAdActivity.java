package com.s23010691.freshconnect.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;
import com.s23010691.freshconnect.ui.item.LocationPickerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Edit Ad Activity
 * Allows users to modify details, add/remove images, or delete their existing product listings.
 */
public class EditAdActivity extends AppCompatActivity {

    private LinearLayout llImageContainer;
    private LinearLayout btnImagePicker;

    private EditText etProductName, etPrice, etQuantity, etDescription;
    private Spinner spinnerCategory, spinnerUnit;
    private LinearLayout llLocationSelector;
    private TextView tvLocation;
    private Button btnUpdateItem, btnDeleteItem;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedLocationName = "";

    private final List<Bitmap> selectedBitmaps = new ArrayList<>();
    private SupabaseClient supabaseClient;
    private OkHttpClient httpClient;
    private Product product;

    private final String[] categories = {"Select Category", "Vegetables", "Fruits", "Herbs", "Seeds", "Dairy", "Other"};
    private final String[] units = {"per kg", "per g", "per piece", "per bundle", "per pack", "per dozen"};

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        addImageToPreview(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    addImageToPreview(bitmap);
                }
            }
    );

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    selectedLocationName = result.getData().getStringExtra("address");
                    if (selectedLocationName == null || selectedLocationName.isEmpty()) {
                        selectedLocationName = "Location Selected";
                    }
                    tvLocation.setText(selectedLocationName);
                }
            }
    );

    /*
     * Initializes the activity, views, and pre-fills the form with existing product details.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ad);

        product = (Product) getIntent().getSerializableExtra("PRODUCT");
        if (product == null) {
            Toast.makeText(this, "Listing details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        supabaseClient = new SupabaseClient();
        httpClient = new OkHttpClient();

        initViews();
        setupSpinners();
        prefillFields();
        setupListeners();
    }

    /*
     * Binds XML layout elements to Java variables.
     */
    private void initViews() {
        llImageContainer = findViewById(R.id.llImageContainer);
        btnImagePicker = findViewById(R.id.btnImagePicker);

        etProductName = findViewById(R.id.etProductName);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);
        etDescription = findViewById(R.id.etDescription);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);

        llLocationSelector = findViewById(R.id.llLocationSelector);
        tvLocation = findViewById(R.id.tvLocation);

        btnUpdateItem = findViewById(R.id.btnUpdateItem);
        btnDeleteItem = findViewById(R.id.btnDeleteItem);
    }

    /*
     * Configures the dropdown spinners for category and unit selection.
     */
    private void setupSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerUnit.setAdapter(unitAdapter);
    }

    /*
     * Populates the form fields with the existing data of the product being edited.
     */
    private void prefillFields() {
        etProductName.setText(product.name);
        
        // Price prefill formatting
        if (product.price == (long) product.price) {
            etPrice.setText(String.valueOf((long) product.price));
        } else {
            etPrice.setText(String.valueOf(product.price));
        }

        etQuantity.setText(product.quantity);
        etDescription.setText(product.description);
        tvLocation.setText(product.location_name);

        selectedLat = product.latitude;
        selectedLng = product.longitude;
        selectedLocationName = product.location_name;

        // Set Category selection
        if (product.category != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(product.category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Set Unit selection
        if (product.unit != null) {
            for (int i = 0; i < units.length; i++) {
                if (units[i].equalsIgnoreCase(product.unit)) {
                    spinnerUnit.setSelection(i);
                    break;
                }
            }
        }

        // Load existing images
        if (product.image_urls != null) {
            for (String url : product.image_urls) {
                if (url != null && !url.isEmpty()) {
                    downloadImageAsBitmap(url);
                }
            }
        }
    }

    /*
     * Asynchronously downloads existing product images from URLs and adds them to the preview.
     * Parameters:
     *   - url: The image URL to download.
     */
    private void downloadImageAsBitmap(String url) {
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Fail silently
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    if (bitmap != null) {
                        runOnUiThread(() -> addImageToPreview(bitmap));
                    }
                }
            }
        });
    }

    /*
     * Configures click listeners for the image picker, location selector, and action buttons.
     */
    private void setupListeners() {
        btnImagePicker.setOnClickListener(v -> {
            String[] options = {"Camera", "Gallery"};
            new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        cameraLauncher.launch(null);
                    } else if (which == 1) {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
        });

        llLocationSelector.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            // Pass existing coordinates to map picker if desired
            mapLauncher.launch(intent);
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnUpdateItem.setOnClickListener(v -> updateItem());
        btnDeleteItem.setOnClickListener(v -> confirmDeleteListing());
    }

    /*
     * Adds the selected image bitmap to the horizontal preview list.
     * Parameters:
     *   - bitmap: The image data to display.
     */
    private void addImageToPreview(Bitmap bitmap) {
        if (selectedBitmaps.size() >= 5) {
            Toast.makeText(this, "You can add up to 5 images", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedBitmaps.add(bitmap);

        View previewView = getLayoutInflater().inflate(R.layout.item_image_preview, llImageContainer, false);
        ImageView ivPreview = previewView.findViewById(R.id.ivPreview);
        ImageView btnClosePreview = previewView.findViewById(R.id.btnClosePreview);

        ivPreview.setImageBitmap(bitmap);

        btnClosePreview.setOnClickListener(v -> {
            llImageContainer.removeView(previewView);
            selectedBitmaps.remove(bitmap);
            updateImagePickerVisibility();
        });

        llImageContainer.addView(previewView, llImageContainer.getChildCount() - 1);
        updateImagePickerVisibility();
    }

    /*
     * Hides the image picker button if the maximum of 5 images has been reached.
     */
    private void updateImagePickerVisibility() {
        if (selectedBitmaps.size() >= 5) {
            btnImagePicker.setVisibility(View.GONE);
        } else {
            btnImagePicker.setVisibility(View.VISIBLE);
        }
    }

    /*
     * Validates updated form inputs and initiates the update sequence.
     */
    private void updateItem() {
        String name = etProductName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String unit = spinnerUnit.getSelectedItem().toString();

        if (name.isEmpty() || priceStr.isEmpty() || quantity.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBitmaps.isEmpty()) {
            Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLat == 0 && selectedLng == 0) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);

        btnUpdateItem.setEnabled(false);
        btnUpdateItem.setText("Uploading...");
        btnDeleteItem.setEnabled(false);
        
        uploadImagesAndUpdate(name, category, price, unit, quantity, description);
    }

    /*
     * Initiates the recursive image upload process before updating the product data.
     */
    private void uploadImagesAndUpdate(String name, String category, double price, String unit, String quantity, String description) {
        List<String> uploadedUrls = new ArrayList<>();
        uploadNextImage(0, uploadedUrls, name, category, price, unit, quantity, description);
    }

    /*
     * Recursively uploads selected images to Supabase storage and then updates the product details.
     */
    private void uploadNextImage(int index, List<String> uploadedUrls, String name, String category, double price, String unit, String quantity, String description) {
        if (index >= selectedBitmaps.size()) {
            // All images uploaded, construct PATCH JsonObject
            JsonObject updates = new JsonObject();
            updates.addProperty("name", name);
            updates.addProperty("category", category);
            updates.addProperty("price", price);
            updates.addProperty("unit", unit);
            updates.addProperty("quantity", quantity);
            updates.addProperty("location_name", selectedLocationName);
            updates.addProperty("latitude", selectedLat);
            updates.addProperty("longitude", selectedLng);
            updates.addProperty("description", description);

            JsonArray urlsArray = new JsonArray();
            for (String url : uploadedUrls) {
                urlsArray.add(url);
            }
            updates.add("image_urls", urlsArray);

            supabaseClient.updateProduct(product.id, updates, new SupabaseClient.InsertCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EditAdActivity.this, "Listing updated successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnUpdateItem.setEnabled(true);
                    btnUpdateItem.setText("Save Changes");
                    btnDeleteItem.setEnabled(true);
                    Toast.makeText(EditAdActivity.this, "Failed to update: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + index + ".jpg";

        supabaseClient.uploadProductImage(selectedBitmaps.get(index), fileName, new SupabaseClient.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                uploadedUrls.add(publicUrl);
                uploadNextImage(index + 1, uploadedUrls, name, category, price, unit, quantity, description);
            }

            @Override
            public void onError(String error) {
                btnUpdateItem.setEnabled(true);
                btnUpdateItem.setText("Save Changes");
                btnDeleteItem.setEnabled(true);
                Toast.makeText(EditAdActivity.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /*
     * Displays a confirmation dialog before permanently deleting the listing.
     */
    private void confirmDeleteListing() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to permanently delete this listing?")
                .setPositiveButton("Delete", (dialog, which) -> deleteListing())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /*
     * Calls the Supabase client to delete the product from the database.
     */
    private void deleteListing() {
        btnUpdateItem.setEnabled(false);
        btnDeleteItem.setEnabled(false);
        btnDeleteItem.setText("Deleting...");

        supabaseClient.deleteProduct(product.id, new SupabaseClient.InsertCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditAdActivity.this, "Listing deleted successfully!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                btnUpdateItem.setEnabled(true);
                btnDeleteItem.setEnabled(true);
                btnDeleteItem.setText("Delete Listing");
                Toast.makeText(EditAdActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
