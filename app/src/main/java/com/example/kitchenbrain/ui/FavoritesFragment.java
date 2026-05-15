package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.adapter.FavoritesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {
    private RecyclerView favoritesRecyclerView;
    private TextView emptyFavoritesText;
    private FavoritesAdapter favoritesAdapter;
    private List<SocialRecipe> favoritesList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        initViews(view);
        setupRecyclerView();
        loadFavorites();
        
        return view;
    }

    private void initViews(View view) {
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView);
        emptyFavoritesText = view.findViewById(R.id.emptyFavoritesText);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        favoritesList = new ArrayList<>();
        favoritesAdapter = new FavoritesAdapter(favoritesList, getContext());
    }

    private void setupRecyclerView() {
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritesRecyclerView.setAdapter(favoritesAdapter);
    }

    private void loadFavorites() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Query favoritesQuery = db.collection("recipes")
                .whereEqualTo("savedBy." + userId, true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING);

        favoritesQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }

            favoritesList.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                if (recipe != null) {
                    recipe.setId(doc.getId());
                    favoritesList.add(recipe);
                }
            }

            favoritesAdapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void showEmptyState() {
        if (emptyFavoritesText != null) {
            emptyFavoritesText.setVisibility(View.VISIBLE);
            emptyFavoritesText.setText("No favorite recipes yet");
        }
        if (favoritesRecyclerView != null) {
            favoritesRecyclerView.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (favoritesList.isEmpty()) {
            showEmptyState();
        } else {
            if (emptyFavoritesText != null) {
                emptyFavoritesText.setVisibility(View.GONE);
            }
            if (favoritesRecyclerView != null) {
                favoritesRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}
