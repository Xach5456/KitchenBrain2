package com.example.kitchenbrain;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class IngredientExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> categories;
    private HashMap<String, List<String>> ingredients;
    private Set<String> selectedIngredients;

    public IngredientExpandableListAdapter(Context context, List<String> categories,
                                           HashMap<String, List<String>> ingredients, Set<String> selectedIngredients) {
        this.context = context;
        this.categories = categories;
        this.ingredients = ingredients;
        this.selectedIngredients = selectedIngredients;
    }

    @Override
    public int getGroupCount() { return categories.size(); }

    @Override
    public int getChildrenCount(int groupPosition) { return ingredients.get(categories.get(groupPosition)).size(); }

    @Override
    public Object getGroup(int groupPosition) { return categories.get(groupPosition); }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return ingredients.get(categories.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) { return groupPosition; }

    @Override
    public long getChildId(int groupPosition, int childPosition) { return childPosition; }

    @Override
    public boolean hasStableIds() { return true; }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String category = (String) getGroup(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
        }
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(category);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String ingredient = (String) getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.ingredient_list_item, parent, false);
        }

        CheckBox checkBox = convertView.findViewById(R.id.ingredientCheckBox);
        checkBox.setText(ingredient);
        checkBox.setChecked(selectedIngredients.contains(ingredient));

        checkBox.setOnClickListener(v -> {
            if (checkBox.isChecked()) selectedIngredients.add(ingredient);
            else selectedIngredients.remove(ingredient);
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
}
