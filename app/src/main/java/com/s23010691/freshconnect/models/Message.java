package com.s23010691.freshconnect.models;

/*
 * Message Model
 * Represents an individual message sent within a chat session.
 */
public class Message {
    public Integer id;
    public String sender_id;
    public String receiver_id;
    public String message;
    public String created_at;

    /*
     * Default constructor required for JSON serialization/deserialization.
     */
    public Message() {}

    /*
     * Constructor to initialize a new Message with specific details.
     * Parameters:
     *   - sender_id: The ID of the user sending the message.
     *   - receiver_id: The ID of the user receiving the message.
     *   - message: The actual text content of the message.
     */
    public Message(String sender_id, String receiver_id, String message) {
        // Assign parameters to class fields
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.message = message;
    }
}
