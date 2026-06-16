package com.s23010691.freshconnect.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.network.SupabaseClient;
import com.s23010691.freshconnect.ui.login.LoginActivity;
import com.s23010691.freshconnect.ui.item.LocationPickerActivity;

import java.io.IOException;
import java.io.InputStream;

/*
 * Profile Fragment
 * Displays user profile information, handles profile picture uploads, and allows editing of personal details.
 */
public class ProfileFragment extends Fragment {

    private ImageView ivProfileImage;
    private TextView tvProfileName, tvJoinedYear;
    private TextView tvValName, tvValPhone, tvValLocation;
    
    private SupabaseClient supabaseClient;
    private SharedPreferences sharedPreferences;
    private String userEmail;

    // Gallery Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        InputStream imageStream = requireActivity().getContentResolver().openInputStream(uri);
                        Bitmap selectedBitmap = BitmapFactory.decodeStream(imageStream);
                        if (selectedBitmap != null) {
                            ivProfileImage.setImageBitmap(selectedBitmap);
                            uploadAndSyncProfilePic(selectedBitmap);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to read image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Map Picker Launcher
    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    String address = result.getData().getStringExtra("address");
                    if (address == null || address.isEmpty()) {
                        address = "Location Selected";
                    }
                    saveAndSyncLocation(address, lat, lng);
                }
            }
    );

    /*
     * Inflates the layout, initializes views, and loads cached or fresh user data.
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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        supabaseClient = new SupabaseClient();
        sharedPreferences = requireActivity().getSharedPreferences("FreshConnectProfile", Context.MODE_PRIVATE);

        // Fetch logged in email, fallback to a general value if session details are empty
        userEmail = SupabaseClient.currentUserEmail != null ? SupabaseClient.currentUserEmail : "guest@freshconnect.com";

        initViews(view);
        loadLocalCachedProfile();
        fetchLatestProfileFromSupabase();
        setupListeners();

        return view;
    }

    /*
     * Binds XML layout elements to Java variables.
     * Parameters:
     *   - view: The inflated root view.
     */
    private void initViews(View view) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvJoinedYear = view.findViewById(R.id.tvJoinedYear);
        
        tvValName = view.findViewById(R.id.tvValName);
        tvValPhone = view.findViewById(R.id.tvValPhone);
        tvValLocation = view.findViewById(R.id.tvValLocation);
    }

    /*
     * Loads the user's profile details from SharedPreferences to render UI instantly.
     */
    private void loadLocalCachedProfile() {
        String cachedName = sharedPreferences.getString("name_" + userEmail, "Guest User");
        String cachedPhone = sharedPreferences.getString("phone_" + userEmail, "Not Set");
        String cachedLocation = sharedPreferences.getString("location_" + userEmail, "Not Set");
        String cachedJoined = sharedPreferences.getString("joined_" + userEmail, "Joined in 2026");
        String cachedPicUrl = sharedPreferences.getString("pic_url_" + userEmail, null);

        tvProfileName.setText(cachedName);
        tvValName.setText(cachedName);
        tvValPhone.setText(cachedPhone);
        tvValLocation.setText(cachedLocation);
        tvJoinedYear.setText(cachedJoined);

        if (cachedPicUrl != null && !cachedPicUrl.isEmpty()) {
            loadImageAsync(cachedPicUrl);
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_person);
        }
    }

    /*
     * Fetches the latest profile data from Supabase and updates the UI and local cache.
     */
    private void fetchLatestProfileFromSupabase() {
        if (SupabaseClient.currentUserEmail == null) {
            // Unauthenticated user
            return;
        }

        supabaseClient.fetchUserDetails(userEmail, new SupabaseClient.UserCallback() {
            @Override
            public void onSuccess(JsonObject userJson) {
                if (!isAdded()) return;

                String dbName = userJson.has("name") && !userJson.get("name").isJsonNull() 
                        ? userJson.get("name").getAsString() : "Guest User";
                
                String dbPhone = "Not Set";
                if (userJson.has("mobile_num") && !userJson.get("mobile_num").isJsonNull()) {
                    dbPhone = userJson.get("mobile_num").getAsString();
                }

                String dbLocation = userJson.has("location_name") && !userJson.get("location_name").isJsonNull() 
                        ? userJson.get("location_name").getAsString() : "Not Set";

                String dbPicUrl = userJson.has("profile_pic") && !userJson.get("profile_pic").isJsonNull() 
                        ? userJson.get("profile_pic").getAsString() : null;

                String dbCreatedAt = userJson.has("created_at") && !userJson.get("created_at").isJsonNull() 
                        ? userJson.get("created_at").getAsString() : null;

                String displayJoined = "Joined in 2026";
                if (dbCreatedAt != null) {
                    try {
                        String year = dbCreatedAt.split("-")[0];
                        displayJoined = "Joined in " + year;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Cache values locally
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name_" + userEmail, dbName);
                editor.putString("phone_" + userEmail, dbPhone);
                editor.putString("location_" + userEmail, dbLocation);
                editor.putString("joined_" + userEmail, displayJoined);
                if (dbPicUrl != null) {
                    editor.putString("pic_url_" + userEmail, dbPicUrl);
                }
                editor.apply();

                // Refresh UI
                tvProfileName.setText(dbName);
                tvValName.setText(dbName);
                tvValPhone.setText(dbPhone);
                tvValLocation.setText(dbLocation);
                tvJoinedYear.setText(displayJoined);

                if (dbPicUrl != null) {
                    loadImageAsync(dbPicUrl);
                }
            }

            @Override
            public void onError(String error) {
                // Supabase query failed, gracefully fallback to local cache (already rendered)
            }
        });
    }

    /*
     * Sets up click listeners for editing fields, changing photo, and navigating to other screens.
     */
    private void setupListeners() {
        // Change photo triggers gallery
        View btnChangePhoto = getView() != null ? getView().findViewById(R.id.btnChangePhoto) : null;
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        } else {
            // Also link the circular frame itself
            ivProfileImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        // Edit row Name
        View rowName = getView() != null ? getView().findViewById(R.id.rowName) : null;
        if (rowName != null) {
            rowName.setOnClickListener(v -> showEditNameDialog());
        }

        // Edit row Phone
        View rowPhone = getView() != null ? getView().findViewById(R.id.rowPhone) : null;
        if (rowPhone != null) {
            rowPhone.setOnClickListener(v -> showEditPhoneDialog());
        }

        // Edit row Location
        View rowLocation = getView() != null ? getView().findViewById(R.id.rowLocation) : null;
        if (rowLocation != null) {
            rowLocation.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LocationPickerActivity.class);
                mapLauncher.launch(intent);
            });
        }

        // Saved Items Card
        View cardSavedItems = getView() != null ? getView().findViewById(R.id.cardSavedItems) : null;
        if (cardSavedItems != null) {
            cardSavedItems.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SavedItemsActivity.class);
                startActivity(intent);
            });
        }

        // My Ads Card
        View cardMyAds = getView() != null ? getView().findViewById(R.id.cardMyAds) : null;
        if (cardMyAds != null) {
            cardMyAds.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MyAdsActivity.class);
                startActivity(intent);
            });
        }

        // Log Out Button
        View btnLogOut = getView() != null ? getView().findViewById(R.id.btnLogOut) : null;
        if (btnLogOut != null) {
            btnLogOut.setOnClickListener(v -> handleLogOut());
        }
    }

    /*
     * Displays a dialog to edit the user's name.
     */
    private void showEditNameDialog() {
        String currentName = tvValName.getText().toString();
        if (currentName.equals("Not Set")) currentName = "";

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Name");

        final EditText input = new EditText(getContext());
        input.setText(currentName);
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            saveAndSyncName(newName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /*
     * Displays a dialog to edit the user's phone number.
     */
    private void showEditPhoneDialog() {
        String currentPhone = tvValPhone.getText().toString();
        if (currentPhone.equals("Not Set")) currentPhone = "";

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Phone");

        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        input.setText(currentPhone);
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();
            saveAndSyncPhone(newPhone);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /*
     * Saves the updated name locally and syncs it with the Supabase database.
     * Parameters:
     *   - newName: The new name string.
     */
    private void saveAndSyncName(String newName) {
        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name_" + userEmail, newName);
        editor.apply();

        // Update UI
        tvProfileName.setText(newName);
        tvValName.setText(newName);

        if (SupabaseClient.currentUserEmail != null) {
            JsonObject updates = new JsonObject();
            updates.addProperty("name", newName);

            supabaseClient.updateUserDetails(userEmail, updates, new SupabaseClient.InsertCallback() {
                @Override
                public void onSuccess() {
                    // DB Sync successful
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Sync error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /*
     * Saves the updated phone locally and syncs it with the Supabase database.
     * Parameters:
     *   - newPhone: The new phone string.
     */
    private void saveAndSyncPhone(String newPhone) {
        // Save locally to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phone_" + userEmail, newPhone);
        editor.apply();

        // Update UI
        tvValPhone.setText(newPhone);

        if (SupabaseClient.currentUserEmail != null) {
            JsonObject updates = new JsonObject();
            updates.addProperty("mobile_num", newPhone);

            supabaseClient.updateUserDetails(userEmail, updates, new SupabaseClient.InsertCallback() {
                @Override
                public void onSuccess() {
                    // DB Sync successful
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Failed to save phone in DB: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /*
     * Saves the selected location locally and syncs it with the Supabase database.
     * Parameters:
     *   - address: The human-readable address.
     *   - lat: Latitude.
     *   - lng: Longitude.
     */
    private void saveAndSyncLocation(String address, double lat, double lng) {
        // Save locally to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("location_" + userEmail, address);
        editor.apply();

        // Update UI
        tvValLocation.setText(address);

        if (SupabaseClient.currentUserEmail != null) {
            JsonObject updates = new JsonObject();
            updates.addProperty("location_name", address);
            updates.addProperty("lat", lat);
            updates.addProperty("long", lng);

            supabaseClient.updateUserDetails(userEmail, updates, new SupabaseClient.InsertCallback() {
                @Override
                public void onSuccess() {
                    // DB Sync successful
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Sync location error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /*
     * Uploads the new profile picture to Supabase Storage and updates the user's profile URL.
     * Parameters:
     *   - bitmap: The image data.
     */
    private void uploadAndSyncProfilePic(Bitmap bitmap) {
        if (SupabaseClient.currentUserEmail == null) {
            return;
        }

        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        Toast.makeText(getContext(), "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        supabaseClient.uploadProfilePic(bitmap, fileName, new SupabaseClient.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                // Save locally to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("pic_url_" + userEmail, publicUrl);
                editor.apply();

                // Update in database user details
                JsonObject updates = new JsonObject();
                updates.addProperty("profile_pic", publicUrl);

                supabaseClient.updateUserDetails(userEmail, updates, new SupabaseClient.InsertCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed to sync photo to profile: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * Clears local authentication state and redirects to the Login screen.
     */
    private void handleLogOut() {
        // Clear login credentials
        SupabaseClient.currentAccessToken = null;
        SupabaseClient.currentUserId = null;
        SupabaseClient.currentUserEmail = null;

        // Redirect back to LoginActivity
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /*
     * Asynchronously loads the profile image from a URL and sets it to the ImageView.
     * Parameters:
     *   - url: The image URL to load.
     */
    private void loadImageAsync(String url) {
        if (url == null || url.isEmpty()) {
            ivProfileImage.setImageResource(R.drawable.ic_person);
            return;
        }

        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
        new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> ivProfileImage.setImageResource(R.drawable.ic_person));
                }
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (bitmap != null) {
                                ivProfileImage.setImageBitmap(bitmap);
                            } else {
                                ivProfileImage.setImageResource(R.drawable.ic_person);
                            }
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> ivProfileImage.setImageResource(R.drawable.ic_person));
                    }
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
    }
}
