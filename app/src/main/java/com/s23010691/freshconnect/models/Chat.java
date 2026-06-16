package com.s23010691.freshconnect.models;

/*
 * Chat Model
 * Represents a chat session between two users, storing the other user's ID and the latest message details.
 */
public class Chat {
    public String other_user_id;
    public String last_message;
    public String created_at;

    /*
     * Constructor to initialize a new Chat instance.
     * Parameters:
     *   - other_user_id: The ID of the user being chatted with.
     *   - last_message: The text of the most recent message in the chat.
     *   - created_at: The timestamp of when the last message was created.
     */
    public Chat(String other_user_id, String last_message, String created_at) {
        // Assign parameters to class fields
        this.other_user_id = other_user_id;
        this.last_message = last_message;
        this.created_at = created_at;
    }
}
