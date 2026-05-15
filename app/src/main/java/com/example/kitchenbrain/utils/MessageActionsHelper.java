package com.example.kitchenbrain.utils;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;

public class MessageActionsHelper {
    
    public interface OnMessageActionListener {
        void onEdit(ChatMessage message);
        void onDelete(ChatMessage message);
        void onCopy(ChatMessage message);
    }
    
    public static void showMessageActionsMenu(Context context, ChatMessage message, 
                                           OnMessageActionListener listener) {
        PopupMenu popup = new PopupMenu(context, null);
        popup.getMenuInflater().inflate(R.menu.message_actions_menu, popup.getMenu());
        
        // Only show edit/delete for own messages
        if (!message.getSenderId().equals(getCurrentUserId(context))) {
            Menu menu = popup.getMenu();
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                if (listener != null) listener.onEdit(message);
                return true;
            } else if (itemId == R.id.action_delete) {
                if (listener != null) listener.onDelete(message);
                return true;
            } else if (itemId == R.id.action_copy) {
                if (listener != null) listener.onCopy(message);
                return true;
            } else {
                return false;
            }
        });
        
        popup.show();
    }
    
    private static String getCurrentUserId(Context context) {
        // This should be implemented based on your auth system
        // For now, return empty string - you'll need to integrate with your Firebase Auth
        return "";
    }
    
    public static void copyMessageToClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Message", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show();
    }
}
