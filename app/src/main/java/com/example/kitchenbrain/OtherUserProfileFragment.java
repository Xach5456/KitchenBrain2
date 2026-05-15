package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔥 REDESIGNED OTHER USER PROFILE
 * Now with consistent buttons and improved follow/message logic.
 */
public class OtherUserProfileFragment extends Fragment {
    private static final String TAG = "OtherUserProfile";

    private ImageView imageViewAvatar;
    private TextView textViewUsername;
    private TextView textViewRecipesCount;
    private TextView textViewFollowersCount;
    private TextView textViewFollowingCount;
    
    private Button buttonFollow;
    private Button buttonMessage;
    private Button buttonUnfollowProfile;
    
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
        textViewFollowersCount = view.findViewById(R.id.textViewFollowersCount);
        textViewFollowingCount = view.findViewById(R.id.textViewFollowingCount);
        
        buttonFollow = view.findViewById(R.id.buttonFollow);
        buttonMessage = view.findViewById(R.id.buttonMessage);
        buttonUnfollowProfile = view.findViewById(R.id.buttonUnfollowProfile);
        
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
            @Override public void onDeleteRecipe(Recipe recipe) {}
            @Override public void onRecipeClick(Recipe recipe) {
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
        }, false);
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void loadUserData() {
        if (targetUserId == null) return;

        db.collection("users").document(targetUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (!isAdded() || getContext() == null || documentSnapshot == null) return;
                    
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(documentSnapshot.getId());
                            if (textViewUsername != null) textViewUsername.setText(user.getUsername());
                            if (textViewFollowersCount != null) textViewFollowersCount.setText(String.valueOf(user.getFollowersCount()));
                            if (textViewFollowingCount != null) textViewFollowingCount.setText(String.valueOf(user.getFollowingCount()));
                            if (textViewRecipesCount != null) textViewRecipesCount.setText(String.valueOf(user.getRecipesCount()));
                            
                            if (imageViewAvatar != null) {
                                Glide.with(imageViewAvatar.getContext())
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.drawable.ic_default_avatar)
                                        .circleCrop()
                                        .into(imageViewAvatar);
                            }
                            
                            loadUserRecipes();
                            checkFollowStatus();
                        }
                    }
                });
    }

    private void loadUserRecipes() {
        if (targetUserId == null) return;
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
                });
    }

    private void checkFollowStatus() {
        if (followGraph != null && targetUserId != null) {
            boolean isFollowing = followGraph.isFollowing(targetUserId);
            boolean isFollower = followGraph.isFollower(targetUserId);
            
            // Reset visibility
            buttonFollow.setVisibility(View.VISIBLE);
            buttonMessage.setVisibility(View.GONE);
            buttonUnfollowProfile.setVisibility(View.GONE);

            if (isFollowing && isFollower) {
                // Mutual Friends
                buttonFollow.setVisibility(View.GONE);
                buttonMessage.setVisibility(View.VISIBLE);
                buttonUnfollowProfile.setVisibility(View.VISIBLE);
            } else if (isFollowing) {
                // Following only
                buttonFollow.setText("Following");
                buttonFollow.setBackgroundTintList(null); // Use default style or outlined
            } else if (isFollower) {
                // Follower only
                buttonFollow.setText("Follow Back");
            } else {
                // Not following
                buttonFollow.setText("Follow");
            }
        }
    }

    private void setupClickListeners() {
        if (buttonFollow != null) {
            buttonFollow.setOnClickListener(v -> {
                if (currentUserId == null || targetUserId == null) return;
                boolean isFollowing = followGraph.isFollowing(targetUserId);
                if (isFollowing) {
                    unfollow();
                } else {
                    follow();
                }
            });
        }

        if (buttonMessage != null) {
            buttonMessage.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openChat(targetUserId, textViewUsername.getText().toString());
                }
            });
        }

        if (buttonUnfollowProfile != null) {
            buttonUnfollowProfile.setOnClickListener(v -> unfollow());
        }
    }

    private void follow() {
        followRepository.followUser(currentUserId, targetUserId, (success, error) -> {
            if (success) {
                checkFollowStatus();
                Toast.makeText(getContext(), "Followed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unfollow() {
        followRepository.unfollowUser(currentUserId, targetUserId, (success, error) -> {
            if (success) {
                checkFollowStatus();
                Toast.makeText(getContext(), "Unfollowed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
