package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.ui.CreateRecipeFragment;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.util.RecipeOwnershipUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddRecipeFragment extends Fragment {

    private ExtendedFloatingActionButton fabAddRecipe;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public AddRecipeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_recipe, container, false);

        fabAddRecipe = view.findViewById(R.id.fabAddRecipe);
        recyclerView = view.findViewById(R.id.recyclerViewRecipes);
        if (recyclerView != null && getContext() != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        recipeList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupFabClick();
        setupRecipeUpdatedListener();
        loadRecipes();

        return view;
    }

    private void setupFabClick() {
        if (fabAddRecipe != null) {
            fabAddRecipe.setOnClickListener(v -> {
                if (!isAdded()) return;
                
                // Check if we're in guest mode
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity.isGuestMode()) {
                        // Show message that feature is not available in guest mode
                        Toast.makeText(getContext(), "Creating recipes requires login. Please sign in to access this feature.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                
                if (getActivity() != null) {
                    // 🔥 UPDATED: Opening the modern social CreateRecipeFragment
                    ((MainActivity) getActivity()).showFragment(new CreateRecipeFragment(), "create_recipe");
                }
            });
        }
    }

    private void loadRecipes() {
        if (db == null) return;
        String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            recipeList.clear();
            updateRecipeList();
            return;
        }
        db.collection("recipes").whereEqualTo("authorId", currentUserId).get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                recipeList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    try {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recipeList.add(recipe);
                        }
                    } catch (Exception e) {
                        Log.e("AddRecipeFragment", "Error deserializing recipe: " + doc.getId(), e);
                        // Skip this recipe and continue loading others
                    }
                }
                updateRecipeList();
            } else {
                Toast.makeText(getContext(), "Error loading recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecipeList() {
        if (recyclerView == null || getContext() == null) return;
        if (recipeAdapter == null) {
            recipeAdapter = new RecipeAdapter(recipeList != null ? recipeList : new ArrayList<>(), new RecipeAdapter.OnRecipeClickListener() {
                @Override 
                public void onDeleteRecipe(Recipe recipe) {
                    if (!RecipeOwnershipUtils.isOwner(recipe, getCurrentUserId())) {
                        Toast.makeText(getContext(), "You can only delete recipes you created", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (recipe.getId() != null) {
                        new RecipeRepository().deleteRecipe(recipe.getId(), new RecipeRepository.RecipeCallback<>() {
                            @Override
                            public void onSuccess(Void result) {
                                    recipeList.remove(recipe);
                                    recipeAdapter.updateRecipes(recipeList);
                                    Toast.makeText(getContext(), "Recipe deleted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Error deleting recipe: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                
                @Override 
                public void onEditRecipe(Recipe recipe) {
                    openEditRecipe(recipe);
                }

                @Override 
                public void onRecipeClick(Recipe recipe) {
                    // Handle recipe click - maybe show details
                    Toast.makeText(getContext(), "Recipe clicked: " + recipe.getName(), Toast.LENGTH_SHORT).show();
                }
            });
            recyclerView.setAdapter(recipeAdapter);
        } else {
            recipeAdapter.updateRecipes(recipeList != null ? new ArrayList<>(recipeList) : new ArrayList<>());
        }
    }

    private void setupRecipeUpdatedListener() {
        getParentFragmentManager().setFragmentResultListener("recipe_updated", getViewLifecycleOwner(), (requestKey, result) -> {
            loadRecipes();
            if (getView() != null) {
                Snackbar.make(getView(), "Recipe updated", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void openEditRecipe(Recipe recipe) {
        if (!isAdded() || recipe == null || getActivity() == null) return;
        if (!RecipeOwnershipUtils.isOwner(recipe, getCurrentUserId())) {
            Toast.makeText(getContext(), "You can only edit recipes you created", Toast.LENGTH_SHORT).show();
            return;
        }
        CreateRecipeFragment editFragment = CreateRecipeFragment.newInstance(recipe);
        ((MainActivity) getActivity()).showFragment(editFragment, "edit_recipe_" + recipe.getId());
    }

    private String getCurrentUserId() {
        return auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        recipeAdapter = null;
        fabAddRecipe = null;
    }
}
