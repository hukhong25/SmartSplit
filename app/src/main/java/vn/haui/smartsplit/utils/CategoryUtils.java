package vn.haui.smartsplit.utils;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;

public class CategoryUtils {

    /**
     * Deduces the category based on the description text.
     */
    public static String deduceCategory(String description) {
        String desc = description != null ? description.toLowerCase() : "";
        if (desc.contains("ăn") || desc.contains("uống") || desc.contains("cà phê") || desc.contains("food")) 
            return Expense.CAT_FOOD;
        if (desc.contains("xe") || desc.contains("grab") || desc.contains("vé") || desc.contains("travel") || desc.contains("xăng")) 
            return Expense.CAT_TRAVEL;
        if (desc.contains("mua") || desc.contains("shop") || desc.contains("quần") || desc.contains("áo") || desc.contains("siêu thị")) 
            return Expense.CAT_SHOPPING;
        if (desc.contains("phim") || desc.contains("game") || desc.contains("giải trí") || desc.contains("karaoke")) 
            return Expense.CAT_ENTERTAINMENT;
        
        return Expense.CAT_OTHER;
    }

    /**
     * Returns the string resource ID for a given category constant.
     */
    public static int getCategoryNameResId(String category) {
        if (category == null) return R.string.cat_other;
        switch (category.toUpperCase()) {
            case Expense.CAT_FOOD: return R.string.cat_food;
            case Expense.CAT_TRAVEL: return R.string.cat_travel;
            case Expense.CAT_SHOPPING: return R.string.cat_shopping;
            case Expense.CAT_ENTERTAINMENT: return R.string.cat_entertainment;
            default: return R.string.cat_other;
        }
    }

    /**
     * Returns a fixed index for categories, useful for arrays/charts.
     */
    public static int getCategoryIndex(String category) {
        if (category == null) return 4;
        switch (category.toUpperCase()) {
            case Expense.CAT_FOOD: return 0;
            case Expense.CAT_TRAVEL: return 1;
            case Expense.CAT_SHOPPING: return 2;
            case Expense.CAT_ENTERTAINMENT: return 3;
            default: return 4;
        }
    }
}
