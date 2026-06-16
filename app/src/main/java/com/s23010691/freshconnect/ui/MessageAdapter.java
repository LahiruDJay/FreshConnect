package com.s23010691.freshconnect.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.s23010691.freshconnect.R;
import com.s23010691.freshconnect.models.Message;
import com.s23010691.freshconnect.network.SupabaseClient;
import java.util.List;

/*
 * Message Adapter
 * Manages the display of individual chat bubbles (sent or received) within the ChatRoomActivity.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;

    /*
     * Constructor for MessageAdapter.
     * Parameters:
     *   - messageList: The initial list of messages to display.
     */
    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    /*
     * Determines whether a message was sent by the current user or received from another.
     * Parameters:
     *   - position: The index of the message.
     */
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        String myId = SupabaseClient.currentUserId;
        if (myId != null && message.sender_id != null && message.sender_id.equals(myId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    /*
     * Inflates the appropriate layout (sent vs received) based on the viewType.
     * Parameters:
     *   - parent: The parent view group.
     *   - viewType: The type determining which layout to inflate.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    /*
     * Binds message content to the appropriate ViewHolder.
     * Parameters:
     *   - holder: The ViewHolder to bind data to.
     *   - position: Index of the message.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).tvMessage.setText(message.message);
        } else {
            ((ReceivedMessageViewHolder) holder).tvMessage.setText(message.message);
        }
    }

    /*
     * Returns the total number of messages in the chat.
     */
    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    /*
     * Updates the internal list of messages and refreshes the RecyclerView.
     * Parameters:
     *   - messages: The new list of messages.
     */
    public void setMessages(List<Message> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        /*
         * Constructor for the SentMessageViewHolder.
         */
        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        /*
         * Constructor for the ReceivedMessageViewHolder.
         */
        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
