package com.s23010691.freshconnect.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Message;
import com.s23010691.freshconnect.network.SupabaseClient;
import java.util.ArrayList;
import java.util.List;

/*
 * Chat Room Activity
 * Displays the conversation history between the current user and another user, and allows sending new messages.
 */
public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText etMessage;
    private Button btnSend;
    private SupabaseClient supabaseClient;
    private String otherUserId;
    private Handler pollingHandler;
    private Runnable pollingRunnable;

    /*
     * Initializes the chat room, configures the UI, and starts message polling.
     * Parameters:
     *   - savedInstanceState: Saved state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        otherUserId = getIntent().getStringExtra("other_user_id");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = "Chat";
            if (otherUserId != null && otherUserId.contains("@")) title = otherUserId;
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Fetch name for toolbar
        if (otherUserId != null) {
            new SupabaseClient().fetchUserDetailsById(otherUserId, new SupabaseClient.UserCallback() {
                @Override
                public void onSuccess(com.google.gson.JsonObject userJson) {
                    if (userJson.has("name") && !userJson.get("name").isJsonNull()) {
                        String name = userJson.get("name").getAsString();
                        runOnUiThread(() -> {
                            if (getSupportActionBar() != null) getSupportActionBar().setTitle(name);
                        });
                    }
                }
                @Override
                public void onError(String error) {}
            });
        }

        recyclerView = findViewById(R.id.recyclerViewMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        supabaseClient = new SupabaseClient();

        btnSend.setOnClickListener(v -> sendMessage());

        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                pollingHandler.postDelayed(this, 3000); // Poll every 3 seconds
            }
        };
    }

    /*
     * Validates and sends a new message via Supabase, then triggers an immediate refresh.
     */
    private void sendMessage() {
        String msg = etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        etMessage.setText("");

        supabaseClient.sendMessage(otherUserId, msg, new SupabaseClient.InsertCallback() {
            @Override
            public void onSuccess() {
                loadMessages(); // Refresh immediately
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ChatRoomActivity.this, "Failed to send: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
     * Fetches the latest messages from the server and updates the RecyclerView.
     */
    private void loadMessages() {
        if (otherUserId == null) return;
        
        supabaseClient.fetchMessages(otherUserId, new SupabaseClient.MessagesCallback() {
            @Override
            public void onSuccess(List<Message> messages) {
                adapter.setMessages(messages);
                if (messages.size() > 0) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onError(String error) {
                // Ignore silent errors on polling
            }
        });
    }

    /*
     * Resumes the automatic message polling when the activity is in the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        pollingHandler.post(pollingRunnable);
    }

    /*
     * Stops the automatic message polling when the activity goes to the background.
     */
    @Override
    protected void onPause() {
        super.onPause();
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    /*
     * Handles the toolbar's back button action.
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
