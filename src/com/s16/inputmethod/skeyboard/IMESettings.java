/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.s16.inputmethod.skeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
//import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.AutoText;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.s16.android.KeyboardApp;
import com.s16.inputmethod.skeyboard.R.color;

public class IMESettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

	public static final String APPLICATRION_ICON_KEY = "application_icon";
    private static final String QUICK_FIXES_KEY = "quick_fixes";
    private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";
    /* package */ static final String PREF_LANGUAGE_KEY = "language_key";
    /* package */ static final String PREF_AUTO_HIDE_MINIKEYBOARD = "auto_hide_minikeyboard";
    private static final String KEYBOARD_LAYOUT_SETTINGS_KEY = "keyboard_layout";
    private static final String TEXT_SIZE_SETTINGS_KEY = "key_text_size";
    //private static final String USED_UNICODE_SETTINGS_KEY = "used_unicode";
    private static final String ABOUT_KEY = "about_keyboard";
    
    //private static final String CATEGORY_PREDICTION_KEY = "prediction_category";

    protected static final String TAG = "IMESettings";

    private CheckBoxPreference mQuickFixes;
    private ListPreference mLanguageKeyPreference;
    private ListPreference mKeyboardLayoutPreference;
    private ListPreference mKeyboardTextSizePreference;

    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs);
        
        mQuickFixes = (CheckBoxPreference) findPreference(QUICK_FIXES_KEY);
        //mSettingsKeyPreference = (ListPreference) findPreference(PREF_SETTINGS_KEY);
        mLanguageKeyPreference = (ListPreference) findPreference(PREF_LANGUAGE_KEY);
        mKeyboardLayoutPreference = (ListPreference) findPreference(KEYBOARD_LAYOUT_SETTINGS_KEY);
        mKeyboardTextSizePreference = (ListPreference) findPreference(TEXT_SIZE_SETTINGS_KEY);

        //final PreferenceGroup predictionCategory = (PreferenceGroup) findPreference(CATEGORY_PREDICTION_KEY);
        //predictionCategory.removePreference(findPreference(USED_UNICODE_SETTINGS_KEY));
        
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);

		//CheckBoxPreference jbFix = (CheckBoxPreference)findPreference("used_jbfix");
		//jbFix.setEnabled(Build.VERSION.SDK_INT > 15);
        
        PreferenceItem aboutVersion = (PreferenceItem)this.findPreference(ABOUT_KEY);
		aboutVersion.setOnClickListener(this);
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			aboutVersion.setSummary(getText(R.string.version_name) + " " + pInfo.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

    @Override
    protected void onStart() {
        super.onStart();
        IMESettings.AlertForInstallLocation(this, null);
    }
    
    @SuppressWarnings("deprecation")
	@Override
    protected void onResume() {
        super.onResume();
        int autoTextSize = AutoText.getSize(getListView());
        if (autoTextSize < 1) {
            ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY)).removePreference(mQuickFixes);
        }
        //updateSettingsKeySummary();
        updateLanguageKeySummary();
        updateKeyboardLayoutSummary();
        updateTextSizeSummary();
    }

    @SuppressWarnings("deprecation")
	@Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        (new BackupManager(this)).dataChanged();
        
        if (key.equals(APPLICATRION_ICON_KEY)) {
        	boolean defaultAppIcon = getResources().getBoolean(R.bool.default_application_icon);
        	if (prefs.getBoolean(APPLICATRION_ICON_KEY, defaultAppIcon)) {
        		KeyboardApp.ShowApplicationIcon(getApplicationContext());
        	} else {
        		KeyboardApp.HideApplicationIcon(getApplicationContext());
        	}
        }
        
        updateLanguageKeySummary();
        updateKeyboardLayoutSummary();
        updateTextSizeSummary();
    }
    
    private void updateLanguageKeySummary() {
        mLanguageKeyPreference.setSummary(
                getResources().getStringArray(R.array.language_key_modes)
                [mLanguageKeyPreference.findIndexOfValue(mLanguageKeyPreference.getValue())]);
    }
    
    private void updateKeyboardLayoutSummary() {
    	mKeyboardLayoutPreference.setSummary(
                getResources().getStringArray(R.array.keyboard_layout_modes)
                [mKeyboardLayoutPreference.findIndexOfValue(mKeyboardLayoutPreference.getValue())]);
    }
    
    private void updateTextSizeSummary() {
    	mKeyboardTextSizePreference.setSummary(
	            getResources().getStringArray(R.array.keyboard_text_size_modes)
	            [mKeyboardTextSizePreference.findIndexOfValue(mKeyboardTextSizePreference.getValue())]);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.getKey().contains(ABOUT_KEY)) {
			showAboutDialog();
		}
		return false;
	}

	// SMM {
	/* package */ static boolean AlertForInstallLocation(Context context, DialogInterface.OnDismissListener dismissListener) {
		String pkgPath = context.getPackageCodePath();
		if(pkgPath == null || pkgPath == "") {
			try {
				pkgPath = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if(pkgPath != null && !pkgPath.startsWith("/data/app/") && !pkgPath.startsWith("/system/app/")) {
			
			if(context instanceof Activity) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(context.getText(R.string.alert_installocation_title));
				builder.setMessage(context.getText(R.string.alert_installocation_message));
				builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				AlertDialog dialog = builder.create();
				if(dismissListener != null) dialog.setOnDismissListener(dismissListener);
				dialog.show();
				return false;
			} else {
				Toast.makeText(context, R.string.alert_installocation_message, Toast.LENGTH_LONG).show();
				return false;
			}
		}
		return true;
	}
	
	private void showAboutDialog() {
		Context context = this;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, dm);
		
		final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		dialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
		dialogBuilder.setTitle(R.string.english_ime_name);
		
		String html = context.getText(R.string.about_text).toString();
		final TextView message = new TextView(context);
		message.setPadding(padding, padding, padding, padding);
		message.setTextColor(context.getResources().getColor(color.key_text_color_dark));
		message.setMovementMethod(LinkMovementMethod.getInstance());
		message.setText(Html.fromHtml(html));
		dialogBuilder.setView(message);
		
		dialogBuilder.setNegativeButton(context.getText(android.R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialogBuilder.setPositiveButton(null, null);
		dialogBuilder.show();
    }
	// } SMM
}