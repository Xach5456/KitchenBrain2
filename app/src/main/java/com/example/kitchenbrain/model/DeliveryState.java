package com.example.kitchenbrain.model;

/**
 * 🔥 DELIVERY STATE ENUM - WhatsApp/Telegram Style
 * Flow: SENDING → SENT → DELIVERED → READ
 */
public enum DeliveryState {
    SENDING,    // ⏳ Message being sent
    SENT,        // ✔ Message sent to server
    DELIVERED,   // ✔✔ Message delivered to device (gray)
    READ         // ✔✔ Message read by recipient (blue)
}
