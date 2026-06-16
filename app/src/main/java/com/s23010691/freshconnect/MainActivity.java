package com.s23010691.freshconnect;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.s23010691.freshconnect.ui.home.HomeFragment;
import com.s23010691.freshconnect.ui.post.PostFragment;
import com.s23010691.freshconnect.ui.chat.ChatFragment;
import com.s23010691.freshconnect.ui.profile.ProfileFragment;

/*
 * Main Activity
 * This class serves as the main entry point of the FreshConnect application after logging in.
 * It hosts the Bottom Navigation bar and manages the switching between different main fragments.
 */
public class MainActivity extends AppCompatActivity {

    /*
     * Initializes the activity, sets the layout, and configures the bottom navigation bar.
     * Parameters:
     *   - savedInstanceState: A Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the main layout file for this activity
        setContentView(R.layout.activity_main);

        // Initialize bottom navigation view
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load Home Fragment as default when activity is first created
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Set up listener for bottom navigation item selection
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            // Determine which fragment to load based on the selected menu item
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_post) {
                // Open AddItemActivity for adding new items
                android.content.Intent intent = new android.content.Intent(MainActivity.this, com.s23010691.freshconnect.ui.item.AddItemActivity.class);
                startActivity(intent);
                return false; // Do not select the item, just open the activity
            } else if (id == R.id.nav_chat) {
                fragment = new ChatFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            // Replace the current fragment if a valid fragment was selected
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    /*
     * Replaces the currently displayed fragment in the container with the provided new fragment.
     * Parameters:
     *   - fragment: The Fragment instance that needs to be displayed.
     */
    private void loadFragment(Fragment fragment) {
        // Begin fragment transaction to replace fragment_container with the new fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}