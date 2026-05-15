package com.example.kitchenbrain.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.DeliveryState;

/**
 * 🔥 DELIVERY STATE HELPER - WhatsApp/Telegram Style
 * Features: Status icons + colors + accessibility
 */
public class DeliveryStateHelper {
    
    /**
     * 📊 GET DELIVERY STATUS TEXT
     */
    public static String getDeliveryStatusText(ChatMessage message, String currentUserId) {
        if (!message.getSenderId().equals(currentUserId)) {
            return "";  // Other messages don't show delivery status
        }
        
        DeliveryState state = message.getDeliveryState();
        if (state == null) {
            state = DeliveryState.SENT;  // Default fallback
        }
        
        switch (state) {
            case SENDING:
                return "⏳";  // Clock emoji for sending
            case SENT:
                return "✔";   // Single check
            case DELIVERED:
                return "✔✔";  // Double check (gray)
            case READ:
                return "✔✔";  // Double check (blue)
            default:
                return "✔";
        }
    }
    
    /**
     * 🎨 GET DELIVERY STATUS COLOR
     */
    public static int getDeliveryStatusColor(ChatMessage message, String currentUserId) {
        if (!message.getSenderId().equals(currentUserId)) {
            return Color.TRANSPARENT;  // Other messages don't show status
        }
        
        DeliveryState state = message.getDeliveryState();
        if (state == null) {
            state = DeliveryState.SENT;
        }
        
        switch (state) {
            case SENDING:
                return Color.parseColor("#888888");  // Gray for sending
            case SENT:
                return Color.parseColor("#B3FFFFFF");  // White for sent
            case DELIVERED:
                return Color.parseColor("#B3FFFFFF");  // White for delivered
            case READ:
                return Color.parseColor("#2196F3");  // Blue for read
            default:
                return Color.parseColor("#B3FFFFFF");
        }
    }
    
    /**
     * 🎨 GET DELIVERY STATUS SPANNABLE (for colored text)
     */
    public static SpannableString getDeliveryStatusSpannable(ChatMessage message, String currentUserId) {
        String text = getDeliveryStatusText(message, currentUserId);
        if (text.isEmpty()) {
            return new SpannableString("");
        }
        
        int color = getDeliveryStatusColor(message, currentUserId);
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return spannable;
    }
    
    /**
     * 📊 GET DELIVERY STATUS DESCRIPTION (for accessibility)
     */
    public static String getDeliveryStatusDescription(ChatMessage message, String currentUserId) {
        if (!message.getSenderId().equals(currentUserId)) {
            return "Message from " + message.getSenderId();
        }
        
        DeliveryState state = message.getDeliveryState();
        if (state == null) {
            state = DeliveryState.SENT;
        }
        
        switch (state) {
            case SENDING:
                return "Message sending";
            case SENT:
                return "Message sent";
            case DELIVERED:
                return "Message delivered";
            case READ:
                return "Message read";
            default:
                return "Message sent";
        }
    }
    
    /**
     * 🔥 UPDATE DELIVERY STATE PROGRESSION
     */
    public static void updateDeliveryState(ChatMessage message, DeliveryState newState) {
        if (message.getDeliveryState() == null || 
            newState.ordinal() > message.getDeliveryState().ordinal()) {
            message.setDeliveryState(newState);
        }
    }
    
    /**
     * 📊 IS DELIVERY STATE FINAL
     */
    public static boolean isFinalDeliveryState(DeliveryState state) {
        return state == DeliveryState.READ || state == DeliveryState.DELIVERED;
    }
    
    /**
     * 📊 SHOULD ANIMATE STATUS CHANGE
     */
    public static boolean shouldAnimateStatusChange(DeliveryState oldState, DeliveryState newState) {
        if (oldState == null) return false;
        
        // Animate when progressing to a higher state
        return newState.ordinal() > oldState.ordinal();
    }
    
    /**
     * 🎨 GET STATUS ICON RESOURCE (if using drawables instead of text)
     */
    public static int getDeliveryStatusIconResource(DeliveryState state) {
        switch (state) {
            case SENDING:
                return android.R.drawable.ic_menu_recent_history;  // Clock
            case SENT:
                return android.R.drawable.ic_menu_save;  // Single check
            case DELIVERED:
                return android.R.drawable.ic_menu_more;  // Double check
            case READ:
                return android.R.drawable.ic_menu_more;  // Double check
            default:
                return android.R.drawable.ic_menu_save;
        }
    }
}
