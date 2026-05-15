package com.example.kitchenbrain.navigation;

import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.example.kitchenbrain.ChatFragment;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.manager.ActiveScreenManager;
import com.example.kitchenbrain.utils.ChatIdGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 🎯 ANDROID IMPLEMENTATION of ChatNavigator
 *
 * This class handles the ACTUAL Android navigation (FragmentManager),
 * but UI components don't need to know about it.
 */
public class ChatNavigatorImpl implements ChatNavigator {
    
    private static final String TAG = "ChatNavigatorImpl";
    private final FragmentActivity activity;
    
    public ChatNavigatorImpl(FragmentActivity activity) {
        this.activity = activity;
    }
    
    @Override
    public void openChat(String userId, String username) {
        openChat(userId, username, null);
    }
    
    @Override
    public void openChat(String userId, String username, String roomId) {
        Log.d(TAG, "🎯 [NAV_IMPL] Opening chat for user: " + userId + ", Room: " + roomId);
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ [NAV_IMPL] userId is null or empty");
            return;
        }
        
        // 1. Generate/Normalize Room ID
        String tempRoomId = roomId;
        if (tempRoomId == null || tempRoomId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                tempRoomId = ChatIdGenerator.generate(currentUser.getUid(), userId);
            } else {
                tempRoomId = userId;
            }
        }
        final String finalRoomId = tempRoomId;
        
        // 2. Navigation State Management
        String chatKey = userId + ":" + finalRoomId;
        if (ChatNavigationState.INSTANCE.getNavigationInProgress() && 
            chatKey.equals(ChatNavigationState.INSTANCE.getActiveChatId())) {
            Log.d(TAG, "🚫 [NAV_IMPL] Duplicate navigation blocked for key: " + chatKey);
            return;
        }
        
        ChatNavigationState.INSTANCE.startNavigation(chatKey);
        ActiveScreenManager.setActive(ActiveScreenManager.Screen.Chat);
        
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        // 3. Guard: Prevent reloading the same chat
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof ChatFragment) {
            String currentRoomId = ((ChatFragment) currentFragment).getCurrentChatId();
            if (finalRoomId.equals(currentRoomId)) {
                Log.d(TAG, "🚫 [NAV_IMPL] Chat already open for roomId=" + finalRoomId);
                ChatNavigationState.INSTANCE.completeNavigation();
                return;
            }
        }
        
        // 4. Create and Navigate using newInstance pattern
        ChatFragment chatFragment = ChatFragment.newInstance(userId, finalRoomId);
        
        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack("chat_" + userId)
            .commit();
        
        // Reset navigation state once target is reached
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment f = fragmentManager.findFragmentById(R.id.fragment_container);
                if (f instanceof ChatFragment) {
                    String cid = ((ChatFragment) f).getCurrentChatId();
                    if (cid != null && cid.equals(finalRoomId)) {
                        ChatNavigationState.INSTANCE.completeNavigation();
                        fragmentManager.removeOnBackStackChangedListener(this);
                    }
                }
            }
        });
        
        Log.d(TAG, "✅ [NAV_IMPL] Navigation committed for: " + chatKey);
    }
}
