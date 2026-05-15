package com.example.kitchenbrain;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.Timestamp;

/**
 * Modern Recipe Bottom Sheet Modal
 * Compact popup with rounded corners, dimmed background
 * Features: Like, Rating, Comments, Ingredients, Steps
 */
public class RecipeBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_RECIPE = "arg_recipe";
    
    private Recipe recipe;
    
    // Views
    private ImageButton buttonClose;
    private com.google.android.material.imageview.ShapeableImageView imageRecipe;
    private android.widget.TextView textRecipeTitle;
    private android.widget.TextView textRecipeDescription;
    private android.widget.TextView textCookingTime;
    private android.widget.TextView textServings;
    private android.widget.TextView textDifficulty;
    private ImageButton buttonLike;
    private android.widget.TextView textLikesCount;
    private RatingBar ratingBar;
    private android.widget.TextView textRating;
    private ImageButton buttonComment;
    private android.widget.TextView textCommentsCount;
    private RecyclerView recyclerViewIngredients;
    private RecyclerView recyclerViewSteps;
    private RecyclerView recyclerViewComments;
    private TextInputEditText editComment;
    private MaterialButton buttonSendComment;
    
    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    
    // Data
    private boolean isLiked = false;
    private List<String> ingredients = new ArrayList<>();
    private List<String> steps = new ArrayList<>();
    private List<Comment> comments = new ArrayList<>();
    
    // Adapters
    private IngredientAdapter ingredientAdapter;
    private StepAdapter stepAdapter;
    private CommentAdapter commentAdapter;

    public static RecipeBottomSheetFragment newInstance(Recipe recipe) {
        RecipeBottomSheetFragment fragment = new RecipeBottomSheetFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_RECIPE, recipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            recipe = getArguments().getParcelable(ARG_RECIPE);
        }
        
        // SANITIZE: Ensure recipe has no null fields
        recipe = RecipeSanitizer.sanitize(recipe);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_recipe_modal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecipeData();
        setupClickListeners();
        setupRecyclerViews();
        loadComments();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        
        // Style the bottom sheet
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        return dialog;
    }

    private void initViews(View view) {
        buttonClose = view.findViewById(R.id.buttonClose);
        imageRecipe = view.findViewById(R.id.imageRecipe);
        textRecipeTitle = view.findViewById(R.id.textRecipeTitle);
        textRecipeDescription = view.findViewById(R.id.textRecipeDescription);
        textCookingTime = view.findViewById(R.id.textCookingTime);
        textServings = view.findViewById(R.id.textServings);
        textDifficulty = view.findViewById(R.id.textDifficulty);
        buttonLike = view.findViewById(R.id.buttonLike);
        textLikesCount = view.findViewById(R.id.textLikesCount);
        ratingBar = view.findViewById(R.id.ratingBar);
        textRating = view.findViewById(R.id.textRating);
        buttonComment = view.findViewById(R.id.buttonComment);
        textCommentsCount = view.findViewById(R.id.textCommentsCount);
        recyclerViewIngredients = view.findViewById(R.id.recyclerViewIngredients);
        recyclerViewSteps = view.findViewById(R.id.recyclerViewSteps);
        recyclerViewComments = view.findViewById(R.id.recyclerViewComments);
        editComment = view.findViewById(R.id.editComment);
        buttonSendComment = view.findViewById(R.id.buttonSendComment);
    }

    private void setupRecipeData() {
        if (recipe == null) return;
        
        // Load image with Glide
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(imageRecipe);
        }
        
        // Set text data
        textRecipeTitle.setText(recipe.getName() != null ? recipe.getName() : "Untitled Recipe");
        textRecipeDescription.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        textCookingTime.setText(recipe.getCookingTime() + " min");
        
        // Load ingredients and steps
        if (recipe.getIngredients() != null) {
            ingredients = recipe.getIngredients();
        }
        
        String instructions = recipe.getCookingInstructions();
        if (instructions != null && !instructions.isEmpty()) {
            // Split by newlines or numbers
            String[] stepsArray = instructions.split("\\n");
            for (String step : stepsArray) {
                if (!step.trim().isEmpty()) {
                    steps.add(step.trim());
                }
            }
        }
        
        // Update like button state
        updateLikeButton();
    }

    private void setupClickListeners() {
        // Close button
        buttonClose.setOnClickListener(v -> dismiss());
        
        // Like button with animation
        buttonLike.setOnClickListener(v -> {
            toggleLike();
            animateLikeButton();
        });
        
        // Rating bar
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                submitRating(rating);
            }
        });
        
        // Comment button (scroll to comments)
        buttonComment.setOnClickListener(v -> {
            recyclerViewComments.smoothScrollToPosition(0);
        });
        
        // Send comment
        buttonSendComment.setOnClickListener(v -> {
            submitComment();
        });
    }

    private void setupRecyclerViews() {
        // Ingredients
        ingredientAdapter = new IngredientAdapter(ingredients);
        recyclerViewIngredients.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewIngredients.setAdapter(ingredientAdapter);
        
        // Steps
        stepAdapter = new StepAdapter(steps);
        recyclerViewSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewSteps.setAdapter(stepAdapter);
        
        // Comments
        commentAdapter = new CommentAdapter(comments);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void toggleLike() {
        if (recipe == null || currentUserId == null) return;
        
        isLiked = !isLiked;
        updateLikeButton();
        
        // Update Firestore
        db.collection("recipes").document(recipe.getId())
                .update("likes." + currentUserId, isLiked)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), 
                            isLiked ? "❤️ Liked!" : "Like removed", 
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert on error
                    isLiked = !isLiked;
                    updateLikeButton();
                });
    }

    private void updateLikeButton() {
        if (isLiked) {
            buttonLike.setImageResource(R.drawable.ic_heart_filled);
            buttonLike.setColorFilter(requireContext().getResources().getColor(R.color.error_red));
        } else {
            buttonLike.setImageResource(R.drawable.ic_heart_outlined);
            buttonLike.setColorFilter(requireContext().getResources().getColor(R.color.text_secondary));
        }
    }

    private void animateLikeButton() {
        // Simple scale animation
        buttonLike.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .withEndAction(() -> {
                    buttonLike.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void submitRating(float rating) {
        if (recipe == null || currentUserId == null) return;
        
        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("userId", currentUserId);
        ratingData.put("rating", rating);
        ratingData.put("timestamp", FieldValue.serverTimestamp());
        
        db.collection("recipes").document(recipe.getId())
                .collection("ratings")
                .document(currentUserId)
                .set(ratingData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Rating submitted!", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitComment() {
        String commentText = editComment.getText() != null ? 
                editComment.getText().toString().trim() : "";
        
        if (commentText.isEmpty()) {
            Toast.makeText(requireContext(), "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (recipe == null || currentUserId == null) return;
        
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("userId", currentUserId);
        commentData.put("username", auth.getCurrentUser() != null ? 
                auth.getCurrentUser().getEmail() : "Anonymous");
        commentData.put("comment", commentText);
        commentData.put("timestamp", FieldValue.serverTimestamp());
        
        db.collection("recipes").document(recipe.getId())
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    editComment.setText("");
                    Toast.makeText(requireContext(), "Comment posted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to post comment", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments() {
        if (recipe == null) return;
        
        db.collection("recipes").document(recipe.getId())
                .collection("comments")
                .orderBy("timestamp")
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    
                    comments.clear();
                    if (value != null) {
                        for (var doc : value.getDocuments()) {
                            Comment comment = new Comment(
                                    doc.getString("userId"),
                                    doc.getString("username"),
                                    doc.getString("comment"),
                                    doc.getTimestamp("timestamp")
                            );
                            comments.add(comment);
                        }
                    }
                    
                    // PERFORMANCE FIX: Use notifyItemRangeChanged instead of notifyDataSetChanged
                    if (commentAdapter != null) {
                        commentAdapter.notifyItemRangeChanged(0, comments.size());
                    }
                    textCommentsCount.setText(String.valueOf(comments.size()));
                });
    }

    // ========== Simple Adapters ==========
    
    static class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {
        private List<String> ingredients;
        
        IngredientAdapter(List<String> ingredients) {
            this.ingredients = ingredients;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ingredient_check, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textIngredient.setText(ingredients.get(position));
            holder.checkIngredient.setOnCheckedChangeListener(null);
            holder.checkIngredient.setChecked(false);
        }
        
        @Override
        public int getItemCount() {
            return ingredients.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.CheckBox checkIngredient;
            android.widget.TextView textIngredient;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkIngredient = itemView.findViewById(R.id.checkIngredient);
                textIngredient = itemView.findViewById(R.id.textIngredient);
            }
        }
    }
    
    static class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {
        private List<String> steps;
        
        StepAdapter(List<String> steps) {
            this.steps = steps;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cooking_step, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textStepNumber.setText(String.valueOf(position + 1));
            holder.textStep.setText(steps.get(position));
        }
        
        @Override
        public int getItemCount() {
            return steps.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textStepNumber;
            android.widget.TextView textStep;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textStepNumber = itemView.findViewById(R.id.textStepNumber);
                textStep = itemView.findViewById(R.id.textStep);
            }
        }
    }
    
    static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        private List<Comment> comments;
        
        CommentAdapter(List<Comment> comments) {
            this.comments = comments;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.textUsername.setText(comment.username);
            holder.textComment.setText(comment.text);
            holder.textTimestamp.setText(comment.getFormattedTime());
        }
        
        @Override
        public int getItemCount() {
            return comments.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            de.hdodenhof.circleimageview.CircleImageView imageUserAvatar;
            android.widget.TextView textUsername;
            android.widget.TextView textComment;
            android.widget.TextView textTimestamp;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageUserAvatar = itemView.findViewById(R.id.imageUserAvatar);
                textUsername = itemView.findViewById(R.id.textUsername);
                textComment = itemView.findViewById(R.id.textComment);
                textTimestamp = itemView.findViewById(R.id.textTimestamp);
            }
        }
    }
    
    // Simple Comment Model
    static class Comment {
        String userId;
        String username;
        String text;
        Timestamp timestamp;
        
        Comment(String userId, String username, String text, Timestamp timestamp) {
            this.userId = userId;
            this.username = username;
            this.text = text;
            this.timestamp = timestamp;
        }
        
        String getFormattedTime() {
            if (timestamp == null) return "Just now";
            long diff = System.currentTimeMillis() - timestamp.toDate().getTime();
            long minutes = diff / 60000;
            long hours = diff / 3600000;
            long days = diff / 86400000;
            
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            if (hours < 24) return hours + "h ago";
            return days + "d ago";
        }
    }
}
