package com.example.kitchenbrain;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OtherUserProfileFragment extends Fragment {

    private ImageView imageViewAvatar;
    private TextView textViewUsername;
    private TextView textViewRecipesCount;
    private Button buttonFollow;
    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String targetUserId;
    private String currentUserId;
    private FriendManager friendManager;

    public static OtherUserProfileFragment newInstance(String userId) {
        OtherUserProfileFragment fragment = new OtherUserProfileFragment();
        Bundle args = new Bundle();
        args.putString("target_user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other_user_profile, container, false);

        initViews(view);
        setupRecyclerView();
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewRecipesCount = view.findViewById(R.id.textViewRecipesCount);
        buttonFollow = view.findViewById(R.id.buttonFollow);
        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        Bundle args = getArguments();
        if (args != null) {
            targetUserId = args.getString("target_user_id");
        }
        
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId != null) {
            friendManager = new FriendManager(db, currentUserId);
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(recipeList, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onDeleteRecipe(Recipe recipe) {
                // Empty implementation - no deletion allowed for other user's recipes
            }
            
            @Override
            public void onHideRecipe(Recipe recipe) {
                // Empty implementation - no hide functionality for other user's recipes
            }
            
            @Override
            public void onRecipeClick(Recipe recipe) {
                // Handle recipe click for other user's recipes - open recipe detail
                if (getContext() != null) {
                    RecipeDetailFragment detailFragment = RecipeDetailFragment.newInstance(recipe);
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                    }
                }
            }
        }, false); // Disable editing for other user's recipes
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void loadUserData() {
        if (targetUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Invalid user ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Load user data
        db.collection("users").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Load user information
                            if (user.getUsername() != null) {
                                textViewUsername.setText(user.getUsername());
                            }
                            
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(imageViewAvatar.getContext())
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.drawable.ic_default_avatar)
                                        .into(imageViewAvatar);
                            } else {
                                imageViewAvatar.setImageResource(R.drawable.ic_default_avatar);
                            }
                            
                            // Load user's recipes
                            loadUserRecipes();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserRecipes() {
        if (targetUserId == null) return;
        
        // Check if current user is friends with target user
        // For now, we'll allow access to recipes regardless of friendship status
        // In a real app, you might want to restrict access based on privacy settings
        
        db.collection("recipes")
                .whereEqualTo("authorId", targetUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recipeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = document.toObject(Recipe.class);
                        recipe.setId(document.getId());
                        recipeList.add(recipe);
                    }
                    recipeAdapter.updateRecipes(recipeList);
                    textViewRecipesCount.setText("Recipes: " + recipeList.size());
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        if (buttonFollow != null) {
            buttonFollow.setOnClickListener(v -> {
                if (currentUserId == null || targetUserId == null) {
                    Toast.makeText(getContext(), "Unable to follow user", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (friendManager != null) {
                    friendManager.isFollowing(targetUserId)
                            .addOnSuccessListener(isFollowing -> {
                                if (isFollowing) {
                                    // Unfollow
                                    friendManager.unfollowUser(targetUserId)
                                            .addOnSuccessListener(aVoid -> {
                                                buttonFollow.setText("Follow");
                                                Toast.makeText(getContext(), "Unfollowed user", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Error unfollowing user", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    // Follow
                                    friendManager.followUser(targetUserId)
                                            .addOnSuccessListener(aVoid -> {
                                                buttonFollow.setText("Unfollow");
                                                Toast.makeText(getContext(), "Followed user", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Error following user", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendManager != null) {
            friendManager.cleanup();
        }
    }
}