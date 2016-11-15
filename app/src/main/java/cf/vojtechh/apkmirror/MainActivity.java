package cf.vojtechh.apkmirror;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.KeyEvent;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.common.CommonUtils;
import io.fabric.sdk.android.services.concurrency.DependsOn;

import java.io.File;

import java.util.Arrays;

import static cf.vojtechh.apkmirror.R.id.progress;

public class MainActivity extends AppCompatActivity  {
    private final static int REQUEST_WRITE_STORAGE_RESULT = 112;
    public WebView mWebView;
    private SwipeRefreshLayout swipeRefreshLayout;
    public ProgressBar Pbar;
    public ProgressBar PbarSplash;
    String url;
    public String pageColor;
    public static BottomBar bottomBar;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //Check if response is positive
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null){
                        //if no file available
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled"})

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE);
        boolean crashlyticsSwitch = prefs.getBoolean("crashlytics", true);
        if (crashlyticsSwitch) {
            Fabric.with(this, new Crashlytics());
        }
        setContentView(R.layout.activity_main);
        Intent Openedfromexternallink = getIntent();
        createShortcuts();


        //all of the resources
        Pbar = (ProgressBar) findViewById(R.id.loading);
        PbarSplash = (ProgressBar) findViewById(progress);
        //sets the height of progressbar
        Pbar.setScaleY(2f);
        //checking for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {}
            else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.storage_access, Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_RESULT);
            }
        }
        createBottomBar();
        //Support for external links
        Uri data = Openedfromexternallink.getData();
        if (data != null){
            url = data.toString();
        }
        else if (data == Uri.parse("http://www.apkmirror.com/developers/")){
            mWebView.loadUrl("http://www.apkmirror.com/developers/");

        }
        else if (data == Uri.parse("http://www.apkmirror.com/apk-upload/")){
            mWebView.loadUrl("http://www.apkmirror.com/apk-upload/");
        }

        else {
            url = "http://www.apkmirror.com/";
        }
        //setting webview
        mWebView = (WebView) findViewById(R.id.apkmirror);
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(false);
        mWebView.getSettings().setSaveFormData (true);
        mWebView.getSettings().setSavePassword(true);
        mWebView.setWebViewClient(new mWebClient());
        mWebView.setWebChromeClient(new mChromeClient());
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieSyncManager.createInstance(MainActivity.this);
        CookieSyncManager.getInstance().startSync();
        mWebView.loadUrl(url);
        mWebView.requestFocus(View.FOCUS_DOWN);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mWebView.reload();

                //i know this is wrong, im sorry but idk how to do it
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 2000);
            }

        });
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorAccent, R.color.colorAccent);


        //checking if shared preferences exists
        File f = new File(getFilesDir().getPath(), "../shared_prefs/cf.vojtechh.apkmirror.xml");
        if (!f.exists()){
            SharedPreferences.Editor editor = getSharedPreferences("cf.vojtechh.apkmirror", MODE_PRIVATE).edit();
            editor.putBoolean("cache", false);
            editor.putBoolean("javascript", false);
            editor.putBoolean("navcolor", true);
            editor.putBoolean("title", false);
            editor.putBoolean("dark", false);
            editor.putBoolean("orientation", false);
            editor.putBoolean("crashlytics", true);
            editor.apply();
        }


        boolean cacheSwitch = prefs.getBoolean("cache", false);
        boolean javascriptSwitch = prefs.getBoolean("javascript", false);
        boolean navbarSwitch = prefs.getBoolean("navcolor", true);
        final boolean titleSwitch = prefs.getBoolean("title", false);
        boolean orientationSwitch = prefs.getBoolean("orientation", false);

        if(cacheSwitch){
            mWebView.getSettings().setAppCacheEnabled(false);
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            mWebView.clearCache(true);
            mWebView.clearHistory();
        }
        else{
            mWebView.getSettings().setAppCacheEnabled(true);
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            mWebView.clearCache(false);
            mWebView.clearHistory();
        }

        if (javascriptSwitch){

            mWebView.getSettings().setJavaScriptEnabled(false);
        }
        else {

            mWebView.getSettings().setJavaScriptEnabled(true);
        }

        if (navbarSwitch){

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        }

        if (!orientationSwitch){
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimetype, final long contentLength) {
                final String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                android.util.Log.d("Applog", "fileName:" + fileName);
                final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType("application/vnd.android.package-archive");
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                Snackbar.make(findViewById(R.id.fab), getString(R.string.download_started) + "  (" + fileName + ")", 1500)
                        .show();

                final String dwn = getResources().getString(R.string.download);
                final View.OnClickListener opendown = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File apk = new File(downloads, fileName);
                            Uri apkUri = FileProvider.getUriForFile(MainActivity.this, "cf.vojtechh.apkmirror.provider", apk);
                            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                            intent.setData(apkUri);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } else {
                            File apkfile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + fileName);
                            Uri apkUri = Uri.fromFile(apkfile);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        }
                    }
                };
                BroadcastReceiver onComplete=new BroadcastReceiver() {
                    public void onReceive(Context ctxt, Intent intent) {
                        Snackbar.make(findViewById(R.id.fab), dwn + " " + fileName , Snackbar.LENGTH_LONG)
                                .setAction(R.string.open, opendown)
                                .show();
                    }
                };
                registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                if (titleSwitch){
                    String regex = "\\bAPK\\b";
                    String regex2 = "\\bDownload\\b\\s*";
                    String title = mWebView.getTitle();
                    String title1 = title.replaceAll(regex, "");
                    String title2 = title1.replaceAll(regex2, "");

                    request.setTitle(title2 + ".apk");

                }

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                manager.enqueue(request);
            }
        });

    }
    private class mWebClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (findViewById(R.id.splash_screen).getVisibility() == View.VISIBLE) {
                crossfade(findViewById(R.id.splash_screen),findViewById(R.id.main_view));
            }
            CookieSyncManager.getInstance().sync();
            updateRecents();
            //updateInterfaceColor();


            Pbar.animate()
                    .alpha(0f)
                    .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Pbar.setVisibility(View.GONE);
                        }
                    });

        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            Pbar.animate()
                    .alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
            Pbar.setVisibility(ProgressBar.VISIBLE);

            updateBottomBar();
        }
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            //This will make the user able to login to disqus with twitter,google,disqus or facebook account
            if (!url.contains("apkmirror.com")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception exc) {
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            else {
                mWebView.loadUrl(url);
                return true;
            }
        }
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mWebView.loadUrl("file:///android_asset/errorpage.html");
        }

    }

    private class mChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int progress) {
            //Webview progressbar
            //update the progressbar value
            ObjectAnimator animation = ObjectAnimator.ofInt(Pbar, "progress", progress);
            animation.setDuration(100); // 0.5 second
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();

            //splash screen progress bar
            // will update the "progress" propriety of seekbar until it reaches progress
            ObjectAnimator animation2 = ObjectAnimator.ofInt(PbarSplash, "progress", progress);
            animation2.setDuration(500); // 0.5 second
            animation2.setInterpolator(new DecelerateInterpolator());
            animation2.start();
        }


        //For Android 4.1+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
            mUM = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/vnd.android.package-archive");
            MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), MainActivity.FCR);
        }


        //For Android 5.0+
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams){
            if(mUMA != null){
                mUMA.onReceiveValue(null);
            }
            mUMA = filePathCallback;
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("application/vnd.android.package-archive");
            Intent[] intentArray;
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Choose APK");
            startActivityForResult(chooserIntent, FCR);
            return true;
        }

        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.d("PageColor!", message);
            result.confirm();
            pageColor = message;
            return true;
        }
    }

    //settings opening
    public void openSettings(){
        Intent i = new Intent
                (MainActivity.this, SettingsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

    }
    //back key
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
    //requesting the permission to write to external storage
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == REQUEST_WRITE_STORAGE_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
            }else{
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("RESTART!");
                alertDialog.setMessage(getString(R.string.storage_access) + ". " + getString(R.string.storage_access_denied));
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Restart",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = getBaseContext().getPackageManager()
                                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });
                alertDialog.show();
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void createBottomBar(){
        bottomBar = (BottomBar) findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                if (tabId == R.id.tab_homepage) {
                    if (findViewById(R.id.splash_screen).getVisibility() == View.VISIBLE) {
                        return;
                    }
                    if (findViewById(R.id.splash_screen).getVisibility() == View.GONE) {
                        mWebView.loadUrl("http://www.apkmirror.com/");
                    }
                }if (tabId == R.id.tab_devs) {
                    mWebView.loadUrl("http://www.apkmirror.com/developers/");
                }if (tabId == R.id.tab_upload) {
                    mWebView.loadUrl("http://www.apkmirror.com/apk-upload/");
                }if (tabId == R.id.tab_settings) {
                    openSettings();
                }if (tabId == R.id.tab_exit) {
                    finish();
                }
            }
        });
        bottomBar.setOnTabReselectListener(new OnTabReselectListener() {
            @Override
            public void onTabReSelected(@IdRes int tabId) {
                if (tabId == R.id.tab_homepage) {
                    mWebView.scrollTo(0,0);
                }if (tabId == R.id.tab_devs) {
                    mWebView.scrollTo(0,0);
                }if (tabId == R.id.tab_upload) {
                    mWebView.scrollTo(0,0);
                }
            }
        });

    }
    public void updateRecents(){
        if (mWebView.getUrl().matches("http://www.apkmirror.com/")){
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getResources().getColor(R.color.Recents));
                this.setTaskDescription(taskDesc);
            }
        }else{
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(mWebView.getTitle(), bm, getResources().getColor(R.color.Recents));
                this.setTaskDescription(taskDesc);
            }

        }
    }

    public void createShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Context context = getBaseContext();
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
                    .setShortLabel(getString(R.string.latest_uploads))
                    .setLongLabel(getString(R.string.latest_uploads))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_upload_shortcut))
                    .setIntent(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.apkmirror.com/apk-upload/")))
                    .build();
            ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "id2")
                    .setShortLabel(getString(R.string.developers))
                    .setLongLabel(getString(R.string.developers))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_people_shortcut))
                    .setIntent(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.apkmirror.com/developers/")))
                    .build();

            shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut, shortcut2));

        }


    }

    public void updateBottomBar(){
        //Try-catch prevents the app from crashing duo to loading some custom javascript (see above)
        try {
            if (mWebView.getUrl().matches("http://www.apkmirror.com/developers/") && bottomBar.getCurrentTabPosition() != 1) {
                bottomBar.selectTabAtPosition(1);
            } else if (mWebView.getUrl().matches("http://www.apkmirror.com/apk-upload/") && bottomBar.getCurrentTabPosition() != 2) {
                bottomBar.selectTabAtPosition(2);
            } else if (!mWebView.getUrl().matches("http://www.apkmirror.com/developers/") && !mWebView.getUrl().matches("http://www.apkmirror.com/apk-upload/") && bottomBar.getCurrentTabPosition() != 0) {

            }
        }catch (NullPointerException e){
            Log.w("Javascript error", "Incorrect bottombar update");
        }
    }

    public void updateInterfaceColor(){
        mWebView.loadUrl("javascript:alert($(\"#masthead\").css(\"background\"))");
    }


    public void search(View view){
        mWebView.loadUrl("javascript:document.getElementById(\"searchButtonMobile\").click();");
        mWebView.loadUrl("javascript:document.getElementById(\"searchbox-sidebar\").focus();");
    }

    public void crossfade(final View loadingView, View screen) {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        screen.setAlpha(0f);
        screen.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        screen.animate()
                .alpha(1f)
                .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime))
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        loadingView.animate()
                .alpha(0f)
                .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingView.setVisibility(View.GONE);
                    }
                });
    }

}
