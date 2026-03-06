package com.example.kitchenbrain;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.HashSet;
import java.util.Set;

public class SelectIngredientsFragment extends Fragment {

    private LinearLayout selectedIngredientsContainer;
    private Button btnSelectIngredients;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_ingredients, container, false);

        selectedIngredientsContainer = view.findViewById(R.id.selectedIngredientsContainer);
        btnSelectIngredients = view.findViewById(R.id.btnSelectIngredients);

        loadSelectedIngredients(); // Загружаем сохранённые ингредиенты

        btnSelectIngredients.setOnClickListener(v -> {
            IngredientsFragment ingredientsFragment = new IngredientsFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ingredientsFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadSelectedIngredients() {
        selectedIngredientsContainer.removeAllViews();
        SharedPreferences preferences = requireActivity().getSharedPreferences("SelectedIngredients", Context.MODE_PRIVATE);
        Set<String> selectedIngredientsSet = preferences.getStringSet("ingredients", new HashSet<>());

        if (!selectedIngredientsSet.isEmpty()) {
            for (String ingredient : selectedIngredientsSet) {
                CheckBox ingredientCheckBox = new CheckBox(getContext());
                ingredientCheckBox.setText(ingredient);
                ingredientCheckBox.setChecked(true);
                ingredientCheckBox.setEnabled(false); // Делаем чекбоксы неактивными
                selectedIngredientsContainer.addView(ingredientCheckBox);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSelectedIngredients(); // Перезагружаем выбранные ингредиенты при возврате
    }
}
