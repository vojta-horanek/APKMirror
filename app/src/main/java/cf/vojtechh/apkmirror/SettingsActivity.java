package cf.vojtechh.apkmirror;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class SettingsActivity extends AppCompatActivity {

    boolean hasAnythingChanged;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hasAnythingChanged = false;
        MainActivity.bottomBar.selectTabAtPosition(0);

        SharedPreferences sharedPrefs = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE);

        boolean crashlyticsSwitch = sharedPrefs.getBoolean("crashlytics", true);
        if (crashlyticsSwitch) {
            Fabric.with(this, new Crashlytics());
        }
        boolean darkSwitch = sharedPrefs.getBoolean("dark", true);
        boolean orientationSwitch = sharedPrefs.getBoolean("orientation", true);
        if(darkSwitch){
            this.setTheme(R.style.DarkSettings);
        }
        else{
            this.setTheme(R.style.Settings);
        }
        if (!orientationSwitch){
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_settings);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getResources().getColor(R.color.Recents));
            this.setTaskDescription(taskDesc);
        }


        boolean navbarSwitch = sharedPrefs.getBoolean("navcolor", true);
        if (navbarSwitch){

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        }

        final Switch option1switch = (Switch) findViewById(R.id.optionswitch1);
        final Switch option2switch = (Switch) findViewById(R.id.optionswitch2);
        final Switch option3switch = (Switch) findViewById(R.id.optionswitch3);
        final Switch option4switch = (Switch) findViewById(R.id.optionswitch4);
        final Switch option5switch = (Switch) findViewById(R.id.optionswitch5);
        final Switch option6switch = (Switch) findViewById(R.id.optionswitch6);
        final Switch option7switch = (Switch) findViewById(R.id.optionswitch7);

        option1switch.setChecked(sharedPrefs.getBoolean("cache", true));
        option2switch.setChecked(sharedPrefs.getBoolean("javascript", true));
        option3switch.setChecked(sharedPrefs.getBoolean("navcolor", false));
        option4switch.setChecked(sharedPrefs.getBoolean("title", true));
        option5switch.setChecked(sharedPrefs.getBoolean("dark", true));
        option6switch.setChecked(sharedPrefs.getBoolean("orientation", true));
        option7switch.setChecked(sharedPrefs.getBoolean("crashlytics", true));

        //setting switch1

        option1switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    disableCache();
                }else{
                    hasAnythingChanged = true;
                    enableCache();
                }
            }
        });

        //setting switch2

        option2switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    hasAnythingChanged = true;
                    disableJavascript();
                }else{
                    hasAnythingChanged = true;
                    enableJavascript();
                }
            }
        });

        //setting switch3

        option3switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    showActionbar();
                }else{
                    hasAnythingChanged = true;
                    hideActionbar();
                }
            }
        });

        //setting switch4

        option4switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    showTitle();
                }else{
                    hasAnythingChanged = true;
                    showFilename();
                }
            }
        });

        option5switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    darkEnabled();
                }else{
                    hasAnythingChanged = true;
                    darkDisabled();
                }
            }
        });
        option6switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    orientationEnabled();
                }else{
                    hasAnythingChanged = true;
                    orientationDisabled();
                    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        option7switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hasAnythingChanged = true;
                    crashlyticsEnabled();
                }else{
                    hasAnythingChanged = true;
                    crashlyticsDisabled();
                }
            }
        });

    }

    public void openGitHub(View view) {
        Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vojta-horanek/APKMirror"));
        startActivity(githubIntent);

    }
    public void openLibs(View view) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.libraries);

        alert.setView(R.layout.libsdialog);
        alert.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();

    }
    public void openThread(View view) {
        Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/android/apps-games/apkmirror-web-app-t3450564"));
        startActivity(threadIntent);

    }
    public void lib1(View view) {
        Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.android.com/topic/libraries/support-library/index.html"));
        startActivity(threadIntent);

    }
    public void lib2(View view) {
        Intent threadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/roughike/BottomBar"));
        startActivity(threadIntent);

    }
    public void disableCache() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("cache", true);
        editor.apply();
    }

    public void enableCache() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("cache", false);
        editor.apply();
    }

    public void disableJavascript() {
        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("javascript", true);
        editor.apply();

    }

    public void enableJavascript() {
        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("javascript", false);
        editor.apply();

    }

    public void showActionbar() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("navcolor", true);
        editor.apply();
        recreate();

    }

    public void hideActionbar() {
        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("navcolor", false);
        editor.apply();
        recreate();

    }

    public void showTitle() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("title", true);
        editor.apply();

    }

    public void showFilename() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("title", false);
        editor.apply();

    }

    public void darkEnabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("dark", true);
        editor.apply();
        this.setTheme(R.style.DarkSettings);
        recreate();
    }

    public void darkDisabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("dark", false);
        editor.apply();
        this.setTheme(R.style.Settings);
        recreate();
    }

    public void orientationEnabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("orientation", true);
        editor.apply();

    }

    public void orientationDisabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("orientation", false);
        editor.apply();

    }

    public void crashlyticsEnabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("crashlytics", true);
        editor.apply();

    }
    public void crashlyticsDisabled() {

        SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
        editor.putBoolean("crashlytics", false);
        editor.apply();

    }

    public void onBackPressed() {
        if(hasAnythingChanged){
            Toast.makeText(this, R.string.settingsrestart, Toast.LENGTH_SHORT).show();

            Intent i = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(i);
            finish();

        }else {
            finish();
        }

        }
}
