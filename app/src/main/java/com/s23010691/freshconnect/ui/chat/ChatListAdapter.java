package com.s23010691.freshconnect.ui.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonObject;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Chat;
import com.s23010691.freshconnect.network.SupabaseClient;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * Chat List Adapter
 * Binds the chat session data to the views in the RecyclerView for the ChatFragment.
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private OnChatClickListener listener;
    private SupabaseClient supabaseClient;
    private OkHttpClient httpClient;
    private Handler mainHandler;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    /*
     * Constructor for the ChatListAdapter.
     * Parameters:
     *   - chatList: The initial list of Chat objects to display.
     *   - listener: A callback interface for click events on chat items.
     */
    public ChatListAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        this.supabaseClient = new SupabaseClient();
        this.httpClient = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /*
     * Inflates the layout for a single chat item view.
     * Parameters:
     *   - parent: The ViewGroup into which the new View will be added.
     *   - viewType: The view type of the new View.
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    /*
     * Binds the chat data to the specified view holder, including fetching user details and profile picture.
     * Parameters:
     *   - holder: The ViewHolder which should be updated to represent the contents of the item.
     *   - position: The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        
        String otherEmail = chat.other_user_id != null ? chat.other_user_id : "Unknown";
        
        // Show ID initially
        holder.tvChatEmail.setText(otherEmail);
        holder.tvInitial.setVisibility(View.VISIBLE);
        holder.ivProfilePic.setVisibility(View.GONE);
        
        if (!otherEmail.isEmpty()) {
            holder.tvInitial.setText(otherEmail.substring(0, 1).toUpperCase());
        }

        // Fetch user details
        supabaseClient.fetchUserDetailsById(otherEmail, new SupabaseClient.UserCallback() {
            @Override
            public void onSuccess(JsonObject userJson) {
                if (userJson.has("name") && !userJson.get("name").isJsonNull()) {
                    String name = userJson.get("name").getAsString();
                    mainHandler.post(() -> {
                        holder.tvChatEmail.setText(name);
                        if (!name.isEmpty()) {
                            holder.tvInitial.setText(name.substring(0, 1).toUpperCase());
                        }
                    });
                }
                
                if (userJson.has("profile_pic") && !userJson.get("profile_pic").isJsonNull()) {
                    String picUrl = userJson.get("profile_pic").getAsString();
                    if (!picUrl.isEmpty()) {
                        Request request = new Request.Builder().url(picUrl).build();
                        httpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                // Ignore
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful() && response.body() != null) {
                                    final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                                    if (bitmap != null) {
                                        mainHandler.post(() -> {
                                            holder.ivProfilePic.setImageBitmap(bitmap);
                                            holder.ivProfilePic.setVisibility(View.VISIBLE);
                                            holder.tvInitial.setVisibility(View.GONE);
                                        });
                                    }
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Keep the default ID display
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    /*
     * Returns the total number of items in the data set held by the adapter.
     */
    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    /*
     * Updates the data set with new chats and notifies the adapter to refresh the view.
     * Parameters:
     *   - chats: The new list of Chat objects.
     */
    public void setChats(List<Chat> chats) {
        this.chatList = chats;
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatEmail;
        TextView tvInitial;
        ImageView ivProfilePic;

        /*
         * Constructor for ChatViewHolder.
         * Parameters:
         *   - itemView: The view for the individual chat list item.
         */
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatEmail = itemView.findViewById(R.id.tvChatEmail);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
        }
    }
}
