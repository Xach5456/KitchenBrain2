package com.example.kitchenbrain;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddRecipeFragment extends Fragment {

    private FloatingActionButton fabAddRecipe;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private FirebaseFirestore db;

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

        setupFabClick();
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
                
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CreateRecipeFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss();
            });
        }
    }

    private void loadRecipes() {
        if (db == null) return;
        db.collection("recipes").get().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                recipeList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    if (recipe != null) {
                        recipe.setId(doc.getId());
                        recipeList.add(recipe);
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
                    // Handle recipe deletion
                    if (db != null && recipe.getId() != null) {
                        db.collection("recipes").document(recipe.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    recipeList.remove(recipe);
                                    recipeAdapter.updateRecipes(recipeList);
                                    Toast.makeText(getContext(), "Recipe deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error deleting recipe", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
                
                @Override 
                public void onHideRecipe(Recipe recipe) {
                    // Empty implementation for AddRecipeFragment - hide functionality not applicable here
                    // This fragment displays all recipes, not suggestions to hide
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        recipeAdapter = null;
        fabAddRecipe = null;
    }
}