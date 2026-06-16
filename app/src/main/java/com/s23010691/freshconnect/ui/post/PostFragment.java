package com.s23010691.freshconnect.ui.post;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.s23010691.freshconnect.R;

/*
 * Post Fragment
 * Acts as a placeholder or entry point for posting new items.
 */
public class PostFragment extends Fragment {

    /*
     * Inflates the layout for the Post screen.
     * Parameters:
     *   - inflater: The LayoutInflater.
     *   - container: The parent view group.
     *   - savedInstanceState: Saved state bundle.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post, container, false);
    }
}
