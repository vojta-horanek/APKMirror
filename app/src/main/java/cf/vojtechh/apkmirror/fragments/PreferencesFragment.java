package cf.vojtechh.apkmirror.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import cf.vojtechh.apkmirror.R;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class PreferencesFragment extends PreferenceFragment {

    SharedPreferences prefsFragment;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        prefsFragment = PreferenceManager.getDefaultSharedPreferences(this.getActivity());


        Preference github = findPreference("github");
        Preference libs = findPreference("libs");
        Preference xda = findPreference("xda");

        github.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vojta-horanek/APKMirror"));
                startActivity(githubIntent);

                return true;
            }
        });

        libs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                final Notices notices = new Notices();
                notices.addNotice(new Notice("jsoup: Java HTML Parser", "https://github.com/jhy/jsoup/", "Jonathan Hedley (jonathan@hedley.net)", new MITLicense()));
                notices.addNotice(new Notice("AdvancedWebView", "https://github.com/delight-im/Android-AdvancedWebView", "Copyright (c) delight.im (https://www.delight.im/)", new MITLicense()));
                notices.addNotice(new Notice("BottomBar library for Android", "https://github.com/roughike/BottomBar", "Copyright (c) 2016 Iiro Krankka (http://github.com/roughike)", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("Material Dialogs", "https://github.com/afollestad/material-dialogs", "Copyright (c) 2014-2016 Aidan Michael Follestad", new MITLicense()));
                notices.addNotice(new Notice("LicensesDialog", "http://psdev.de", "Copyright 2013 Philip Schiffer <admin@psdev.de>", new ApacheSoftwareLicense20()));

                new LicensesDialog.Builder(getActivity())
                        .setNotices(notices)
                        .setTitle(getString(R.string.libraries))
                        .build()
                        .show();

                return true;
            }
        });

        xda.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {

                Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/android/apps-games/apkmirror-web-app-t3450564"));
                startActivity(threadIntent);

                return true;
            }
        });

    }

}

