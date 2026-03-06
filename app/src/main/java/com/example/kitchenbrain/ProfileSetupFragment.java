package com.example.kitchenbrain;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.activity.OnBackPressedCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ProfileSetupFragment extends Fragment {

    private EditText usernameEditText;
    private Button confirmButton;

    public ProfileSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile_setup, container, false);

        // Initialize fields and button
        usernameEditText = view.findViewById(R.id.editTextUsername);
        confirmButton = view.findViewById(R.id.buttonConfirm);

        // Set listener for "Confirm" button
        confirmButton.setOnClickListener(v -> onConfirmClicked());

        // Prevent "Back" button press in this fragment
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Don't perform standard action, just do nothing
            }
        });

        return view;
    }

    private void onConfirmClicked() {
        String username = usernameEditText.getText().toString().trim();

        if (!username.isEmpty()) {
            // Get current user
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Update user profile in Firestore
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("username", username);
            userProfile.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            userProfile.put("avatarUrl", ""); // Default avatar
            userProfile.put("createdAt", Timestamp.now()); // Timestamp
            userProfile.put("isOnline", false); // Default to offline
            userProfile.put("lastSeen", Timestamp.now()); // Current timestamp
            userProfile.put("recipesCount", 0); // Initialize recipe count

            db.collection("users").document(userId)
                    .update(userProfile)
                    .addOnSuccessListener(aVoid -> {
                        // After successful update, show success message and navigate
                        Toast.makeText(getContext(), getString(R.string.username_updated), Toast.LENGTH_SHORT).show();
                        
                        // Navigate to HomeFragment by showing the main screen with navigation visible
                        goToHomeFragment();
                    })
                    .addOnFailureListener(e -> {
                        // If failed to update data, try setting the document
                        db.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener(aVoid -> {
                                    // After successful set, show success message and navigate
                                    Toast.makeText(getContext(), getString(R.string.username_updated), Toast.LENGTH_SHORT).show();
                                    goToHomeFragment();
                                })
                                .addOnFailureListener(setException -> {
                                    // If failed to set data, show error
                                    Toast.makeText(getContext(), getString(R.string.error_saving_profile), Toast.LENGTH_SHORT).show();
                                });
                    });

        } else {
            // If name is not entered, show message
            Toast.makeText(getContext(), getString(R.string.enter_username), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHomeFragment() {
        // Navigate to HomeFragment by showing the main screen with navigation visible
        if (getActivity() instanceof MainActivity) {
            // Show the bottom navigation and load the home fragment
            ((MainActivity) getActivity()).checkProfileSetup(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }
}