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
import com.example.kitchenbrain.repository.FollowRepository;
import com.example.kitchenbrain.manager.FollowGraphRepository;
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
    private FollowRepository followRepository;
    private FollowGraphRepository followGraph;

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
            followRepository = new FollowRepository();
            followGraph = FollowGraphRepository.getInstance();
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
                    if (!isAdded() || getContext() == null) return;
                    
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
                            
                            // Check follow status and update button
                            checkFollowStatus();
                        }

                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
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
                    if (!isAdded() || getContext() == null) return;
                    
                    recipeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = document.toObject(Recipe.class);
                        recipe.setId(document.getId());
                        recipeList.add(recipe);
                    }
                    recipeAdapter.updateRecipes(recipeList);
                    if (textViewRecipesCount != null) {
                        textViewRecipesCount.setText("Recipes: " + recipeList.size());
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error loading recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkFollowStatus() {
        if (followGraph != null && targetUserId != null) {
            // Use instant cache check instead of Firestore query
            boolean isFollowing = followGraph.isFollowing(targetUserId);
            
            if (!isAdded() || getContext() == null) return;
            
            if (isFollowing) {
                buttonFollow.setText(R.string.following);
            } else {
                buttonFollow.setText(R.string.follow);
            }
        }
    }

    private void setupClickListeners() {
        if (buttonFollow != null) {
            buttonFollow.setOnClickListener(v -> {
                if (currentUserId == null || targetUserId == null || !isAdded()) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Unable to follow user", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                
                if (followRepository != null) {
                    // Check current status from cache
                    boolean isFollowing = followGraph.isFollowing(targetUserId);
                    
                    if (isFollowing) {
                        // Unfollow
                        followRepository.unfollowUser(currentUserId, targetUserId, (success, error) -> {
                            if (!isAdded() || getContext() == null) return;
                            
                            if (success) {
                                buttonFollow.setText(R.string.follow);
                                Toast.makeText(getContext(), "Unfollowed user", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Follow
                        followRepository.followUser(currentUserId, targetUserId, (success, error) -> {
                            if (!isAdded() || getContext() == null) return;
                            
                            if (success) {
                                buttonFollow.setText(R.string.following);
                                Toast.makeText(getContext(), "Followed user", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // FollowGraphRepository is singleton - no cleanup needed here
    }
}