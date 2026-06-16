package com.s23010691.freshconnect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Chat;
import com.s23010691.freshconnect.network.SupabaseClient;
import com.s23010691.freshconnect.ui.ChatRoomActivity;
import java.util.ArrayList;
import java.util.List;

/*
 * Chat Fragment
 * Displays the list of recent chats the current user is engaged in.
 */
public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChatListAdapter adapter;
    private SupabaseClient supabaseClient;

    /*
     * Initializes the fragment view, sets up the RecyclerView for chats, and configures the adapter.
     * Parameters:
     *   - inflater: The LayoutInflater object that can be used to inflate any views in the fragment.
     *   - container: If non-null, this is the parent view that the fragment's UI should be attached to.
     *   - savedInstanceState: If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the chat fragment layout
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Set up RecyclerView and ProgressBar
        recyclerView = view.findViewById(R.id.recyclerViewChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        progressBar = view.findViewById(R.id.progressBar);
        
        // Initialize adapter with an empty list and click listener
        adapter = new ChatListAdapter(new ArrayList<>(), chat -> {
            Intent intent = new Intent(getActivity(), ChatRoomActivity.class);
            intent.putExtra("other_user_id", chat.other_user_id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        supabaseClient = new SupabaseClient();

        return view;
    }

    /*
     * Called when the fragment becomes visible; triggers loading of recent chats.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadChats();
    }

    /*
     * Fetches the recent chats from Supabase and updates the RecyclerView.
     */
    private void loadChats() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        supabaseClient.fetchRecentChats(new SupabaseClient.ChatsCallback() {
            @Override
            public void onSuccess(List<Chat> chats) {
                if (getActivity() == null) return;
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setChats(chats);
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
