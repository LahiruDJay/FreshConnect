package com.s23010691.freshconnect.ui.item;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.appcompat.app.AppCompatActivity;

import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Product;
import com.s23010691.freshconnect.network.SupabaseClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Add Item Activity
 * Provides a form for users to list a new agricultural product for sale, including image uploads and location tagging.
 */
public class AddItemActivity extends AppCompatActivity {

    private LinearLayout llImageContainer;
    private LinearLayout btnImagePicker;
    
    private EditText etProductName, etPrice, etQuantity, etDescription;
    private Spinner spinnerCategory, spinnerUnit;
    private LinearLayout llLocationSelector;
    private TextView tvLocation;
    private Button btnPostItem;
    
    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedLocationName = "";

    private final List<Bitmap> selectedBitmaps = new ArrayList<>();
    private SupabaseClient supabaseClient;

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
     * Initializes the activity, views, and sets up UI listeners.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        
        supabaseClient = new SupabaseClient();

        initViews();
        setupSpinners();
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
        
        btnPostItem = findViewById(R.id.btnPostItem);
    }

    /*
     * Configures the dropdown spinners for product category and unit selection.
     */
    private void setupSpinners() {
        String[] categories = {"Select Category", "Vegetables", "Fruits", "Herbs", "Seeds", "Dairy", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);
        
        String[] units = {"per kg", "per g", "per piece", "per bundle", "per pack", "per dozen"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerUnit.setAdapter(unitAdapter);
    }
    
    /*
     * Sets up click listeners for the image picker, location selector, and form submission.
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
            mapLauncher.launch(intent);
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnPostItem.setOnClickListener(v -> postItem());
    }
    
    /*
     * Adds the selected image bitmap to the horizontal preview list on the screen.
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
     * Hides the image picker button if the user has reached the maximum of 5 images.
     */
    private void updateImagePickerVisibility() {
        if (selectedBitmaps.size() >= 5) {
            btnImagePicker.setVisibility(View.GONE);
        } else {
            btnImagePicker.setVisibility(View.VISIBLE);
        }
    }
    
    /*
     * Validates form inputs and initiates the product uploading sequence.
     */
    private void postItem() {
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
        String userId = SupabaseClient.currentUserId != null ? SupabaseClient.currentUserId : "guest";
        
        btnPostItem.setEnabled(false);
        btnPostItem.setText("Uploading...");
        uploadImagesAndPost(name, category, price, unit, quantity, description, userId);
    }
    
    /*
     * Initiates the recursive image upload process before saving the product data.
     * Parameters:
     *   - name: Product name.
     *   - category: Product category.
     *   - price: Product price.
     *   - unit: Measurement unit.
     *   - quantity: Available quantity.
     *   - description: Product description.
     *   - userId: ID of the user listing the item.
     */
    private void uploadImagesAndPost(String name, String category, double price, String unit, String quantity, String description, String userId) {
        List<String> uploadedUrls = new ArrayList<>();
        uploadNextImage(0, uploadedUrls, name, category, price, unit, quantity, description, userId);
    }

    /*
     * Recursively uploads selected images to Supabase storage and then saves the product details.
     * Parameters:
     *   - index: Current image index to upload.
     *   - uploadedUrls: List accumulating the public URLs of uploaded images.
     *   - name: Product name.
     *   - category: Product category.
     *   - price: Product price.
     *   - unit: Measurement unit.
     *   - quantity: Available quantity.
     *   - description: Product description.
     *   - userId: ID of the user listing the item.
     */
    private void uploadNextImage(int index, List<String> uploadedUrls, String name, String category, double price, String unit, String quantity, String description, String userId) {
        if (index >= selectedBitmaps.size()) {
            // All images uploaded, proceed to save product
            String[] urlsArray = uploadedUrls.toArray(new String[0]);
            Product product = new Product(
                    name, category, price, unit, quantity,
                    selectedLocationName, selectedLat, selectedLng,
                    description, urlsArray, userId
            );

            supabaseClient.insertProduct(product, new SupabaseClient.InsertCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddItemActivity.this, "Item posted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnPostItem.setEnabled(true);
                    btnPostItem.setText("Post Item");
                    Toast.makeText(AddItemActivity.this, "Error saving: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        String fileName = System.currentTimeMillis() + "_" + index + ".jpg";

        supabaseClient.uploadProductImage(selectedBitmaps.get(index), fileName, new SupabaseClient.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                uploadedUrls.add(publicUrl);
                uploadNextImage(index + 1, uploadedUrls, name, category, price, unit, quantity, description, userId);
            }

            @Override
            public void onError(String error) {
                btnPostItem.setEnabled(true);
                btnPostItem.setText("Post Item");
                Toast.makeText(AddItemActivity.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
