package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class GuestProfileFragment extends Fragment {

    private TextView textViewUsername;
    private TextView textViewEmail;
    private Button buttonLogin;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guest_profile, container, false);

        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        mAuth = FirebaseAuth.getInstance();
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        buttonLogin = view.findViewById(R.id.buttonLogin);

        // Set guest user information
        textViewUsername.setText("Guest User");
        textViewEmail.setText("guest@example.com");
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            // Navigate to login activity
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }
}