package rkr.simplekeyboard.inputmethod.latin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme;

public class KeyboardSettingsReceiver extends BroadcastReceiver {
    public static final String ACTION_CHANGE_THEME = "rkr.simplekeyboard.CHANGE_THEME";
    public static final String EXTRA_THEME_ID = "theme_id";
    public static final String SURFACE_VARIANT = "surface_variant";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            switch (intent.getAction()){
                case ACTION_CHANGE_THEME: {
                    int themeId = intent.getIntExtra(EXTRA_THEME_ID, KeyboardTheme.DEFAULT_THEME_ID);
                    int surfaceVariant = intent.getIntExtra(SURFACE_VARIANT, 0);
                    SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);
                    KeyboardTheme.saveKeyboardThemeId(themeId, prefs);
                    KeyboardTheme.saveSurfaceVariant(surfaceVariant, prefs); // this changes the keyboard background color, but we might want to use the color for something else later on

                    Log.i("ThemeChangeReceiver", "Keyboard theme changed to ID: " + themeId);
                }
                default: {
                    // do nothing
                }
            }
        }
    }
}