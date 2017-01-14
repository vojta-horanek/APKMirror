package cf.vojtechh.apkmirror;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.licenses.GnuLesserGeneralPublicLicense21;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefsFragment;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        prefsFragment = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        Preference github = findPreference("github");
        Preference libs = findPreference("libs");
        Preference xda = findPreference("xda");
        Preference copy = findPreference("copy");
        Preference evo = findPreference("evo");
        Preference jasom = findPreference("ja_som");
        Preference nuke = findPreference("nuke");
        Preference nin = findPreference("nin");

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
                notices.addNotice(new Notice("Jericho HTML Parser", "http://jericho.htmlparser.net/docs/index.html", "Copyright 2013 Philip Schiffer", new GnuLesserGeneralPublicLicense21()));
                notices.addNotice(new Notice("Butter Knife", "https://github.com/JakeWharton/butterknife", "Copyright 2013 Jake Wharton", new ApacheSoftwareLicense20()));
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

        copy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Notices notices = new Notices();
                notices.addNotice(new Notice("APKMirror", "http://vojtechh.cf", "Copyleft 2016 Vojtěch Hořánek", new GnuGeneralPublicLicense20()));

                new LicensesDialog.Builder(getActivity())
                        .setNotices(notices)
                        .setTitle(getString(R.string.app_name))
                        .build()
                        .show();

                return true;
            }
        });

        evo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/member.php?u=5366167"));
                startActivity(threadIntent);
                return true;
            }
        });
        jasom.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/member.php?u=5201515"));
                startActivity(threadIntent);
                return true;
            }
        });
        nuke.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/member.php?u=6243385"));
                startActivity(threadIntent);
                return true;
            }
        });
        nin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/member.php?u=6002018"));
                startActivity(threadIntent);
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("navbarcolor")) {
            getActivity().recreate();
        } else if (key.equals("darktheme")) {
            if (sharedPreferences.getBoolean("darktheme", true)) {
                //theme is dark
                getActivity().setTheme(R.style.DarkSettings);
                getActivity().recreate();
            } else if (!sharedPreferences.getBoolean("darktheme", true)) {
                //theme isn't dark
                getActivity().setTheme(R.style.Settings);
                getActivity().recreate();
            }

        }

    }

}
