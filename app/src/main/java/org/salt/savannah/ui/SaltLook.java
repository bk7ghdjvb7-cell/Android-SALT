package org.salt.savannah.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.salt.savannah.R;

/** iOS Theme.swift colors, type scale, and control metrics in density-independent points. */
public final class SaltLook {
    public static final int PURPLE = Color.rgb(Math.round(0.42f * 255), Math.round(0.18f * 255), Math.round(0.55f * 255));
    public static final int RED = Color.rgb(Math.round(0.78f * 255), Math.round(0.18f * 255), Math.round(0.22f * 255));
    public static final int GOLD = Color.rgb(Math.round(0.85f * 255), Math.round(0.68f * 255), Math.round(0.22f * 255));
    public static final int BACKGROUND = Color.WHITE;
    public static final int SECONDARY = Color.rgb(242, 242, 247);
    public static final int LABEL = Color.BLACK;
    public static final int SECONDARY_LABEL = Color.rgb(138, 138, 142);
    public static final int SEPARATOR = Color.rgb(198, 198, 200);
    public static final int SEARCH = Color.rgb(233, 233, 235);
    public static final int GREEN = Color.rgb(52, 199, 89);

    public final Context context;
    public final Typeface nunito;
    public final Typeface inter;

    public SaltLook(Context context) {
        this.context = context;
        this.nunito = context.getResources().getFont(R.font.nunito);
        this.inter = context.getResources().getFont(R.font.inter);
    }

    public int dp(float points) {
        return Math.round(points * context.getResources().getDisplayMetrics().density);
    }

    public static int alpha(int color, float opacity) {
        return Color.argb(Math.round(opacity * 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    public TextView text(String value, float size, int weight, boolean rounded, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        view.setTypeface(rounded ? nunito : inter);
        view.setFontVariationSettings("'wght' " + weight + (rounded ? "" : ", 'opsz' " + Math.round(size)));
        view.setIncludeFontPadding(false);
        return view;
    }

    public ImageView icon(String name, int color, float size) {
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        ImageView view = new ImageView(context);
        view.setImageResource(id);
        view.setColorFilter(color);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(size), dp(size));
        view.setLayoutParams(params);
        return view;
    }

    public GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    public EditText input(String hint, boolean secure) {
        EditText field = new EditText(context);
        field.setHint(hint);
        field.setHintTextColor(SECONDARY_LABEL);
        field.setTextColor(LABEL);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        field.setTypeface(inter);
        field.setFontVariationSettings("'wght' 400, 'opsz' 17");
        field.setBackground(rounded(SECONDARY, 12));
        field.setPadding(dp(16), dp(16), dp(16), dp(16));
        field.setSingleLine(true);
        field.setInputType(secure
            ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (secure) {
            field.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        return field;
    }

    public TextView button(String title, int background, int foreground) {
        TextView view = text(title, 17, 600, true, foreground);
        view.setGravity(Gravity.CENTER);
        view.setBackground(rounded(background, 12));
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setClickable(true);
        return view;
    }

    public LinearLayout column() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public LinearLayout row(int gravity) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(gravity);
        return layout;
    }

    public void add(LinearLayout parent, View child, float gapBefore) {
        if (gapBefore > 0 && parent.getChildCount() > 0) {
            View gap = new View(context);
            parent.addView(gap, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(gapBefore)));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        parent.addView(child, params);
    }

    public void press(View view, Runnable action) {
        view.setOnClickListener(v -> action.run());
        view.setClickable(true);
    }

    public View hairline() {
        View line = new View(context);
        line.setBackgroundColor(SEPARATOR);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.5f))));
        return line;
    }
}
