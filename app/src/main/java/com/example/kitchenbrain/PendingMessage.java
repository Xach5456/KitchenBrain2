package com.example.kitchenbrain;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.UUID;

/**
 * PendingMessage - Represents a message waiting to be sent or being retried
 * 
 * This class ensures messages are never lost, even if the app crashes or fragment is destroyed.
 * Messages persist in the queue until successfully acknowledged by Firestore.
 */
public class PendingMessage implements Parcelable {
    
    private final String queueId;           // Unique queue ID for tracking
    private final String messageId;         // Firestore message ID
    private final String chatId;            // Chat room ID
    private final String senderId;          // Current user ID
    private final String receiverId;        // Recipient ID
    private final String text;              // Message content
    private final String messageType;       // text, image, file
    private final long timestamp;           // Creation timestamp (System.currentTimeMillis)
    private final int retryCount;           // Number of send attempts
    private final long lastAttemptTime;     // Last send attempt timestamp
    private final MessageStatus status;     // Current status
    
    /**
     * Create a new pending message
     */
    public PendingMessage(String chatId, String senderId, String receiverId, 
                         String text, String messageType) {
        this.queueId = UUID.randomUUID().toString();
        this.messageId = UUID.randomUUID().toString();
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.messageType = messageType != null ? messageType : "text";
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.lastAttemptTime = 0;
        this.status = MessageStatus.SENDING;
    }
    
    /**
     * Create a retry instance with incremented retry count
     */
    public PendingMessage withRetry(long attemptTime) {
        return new PendingMessage(
            this.queueId,
            this.messageId,
            this.chatId,
            this.senderId,
            this.receiverId,
            this.text,
            this.messageType,
            this.timestamp,
            this.retryCount + 1,
            attemptTime,
            MessageStatus.SENDING
        );
    }
    
    /**
     * Create a failed instance
     */
    public PendingMessage withFailure() {
        return new PendingMessage(
            this.queueId,
            this.messageId,
            this.chatId,
            this.senderId,
            this.receiverId,
            this.text,
            this.messageType,
            this.timestamp,
            this.retryCount,
            this.lastAttemptTime,
            MessageStatus.FAILED
        );
    }
    
    /**
     * Constructor with all fields (for Parcelable and withRetry/withFailure)
     */
    private PendingMessage(String queueId, String messageId, String chatId,
                          String senderId, String receiverId, String text,
                          String messageType, long timestamp, int retryCount,
                          long lastAttemptTime, MessageStatus status) {
        this.queueId = queueId;
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.retryCount = retryCount;
        this.lastAttemptTime = lastAttemptTime;
        this.status = status;
    }
    
    // Getters
    public String getQueueId() { return queueId; }
    public String getMessageId() { return messageId; }
    public String getChatId() { return chatId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getText() { return text; }
    public String getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
    public int getRetryCount() { return retryCount; }
    public long getLastAttemptTime() { return lastAttemptTime; }
    public MessageStatus getStatus() { return status; }
    
    /**
     * Check if this message should be retried based on exponential backoff
     */
    public boolean shouldRetry() {
        // Max retries: 10 attempts
        if (retryCount >= 10) return false;
        
        // Exponential backoff: wait time = 2^retryCount seconds (max 30 minutes)
        long waitTimeMs = (long) Math.pow(2, retryCount) * 1000;
        waitTimeMs = Math.min(waitTimeMs, 30 * 60 * 1000); // Cap at 30 minutes
        
        return System.currentTimeMillis() - lastAttemptTime >= waitTimeMs;
    }
    
    /**
     * Convert to ChatMessage for UI display
     */
    public ChatMessage toChatMessage() {
        ChatMessage message = new ChatMessage(
            messageId,
            senderId,
            receiverId,
            text,
            new Timestamp(new java.util.Date(timestamp)),
            status.getValue()
        );
        message.setMessageType(messageType);
        return message;
    }
    
    // Parcelable implementation
    protected PendingMessage(Parcel in) {
        queueId = in.readString();
        messageId = in.readString();
        chatId = in.readString();
        senderId = in.readString();
        receiverId = in.readString();
        text = in.readString();
        messageType = in.readString();
        timestamp = in.readLong();
        retryCount = in.readInt();
        lastAttemptTime = in.readLong();
        status = MessageStatus.fromString(in.readString());
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(queueId);
        dest.writeString(messageId);
        dest.writeString(chatId);
        dest.writeString(senderId);
        dest.writeString(receiverId);
        dest.writeString(text);
        dest.writeString(messageType);
        dest.writeLong(timestamp);
        dest.writeInt(retryCount);
        dest.writeLong(lastAttemptTime);
        dest.writeString(status.getValue());
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<PendingMessage> CREATOR = new Creator<PendingMessage>() {
        @Override
        public PendingMessage createFromParcel(Parcel in) {
            return new PendingMessage(in);
        }
        
        @Override
        public PendingMessage[] newArray(int size) {
            return new PendingMessage[size];
        }
    };
}
