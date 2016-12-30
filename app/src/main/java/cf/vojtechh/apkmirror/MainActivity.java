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
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity  {

    private String url;
    private String urlFrontPage = "http://www.apkmirror.com/";
    private String urlDev = "http://www.apkmirror.com/developers/";
    private String urlUp = "http://www.apkmirror.com/apk-upload/";
    private String currentUrl;
    private String mCM;
    String fileName;
    String ColorDarkTheme = "#f47d20";
    public String htmlSource;
    public String appName;
    public String faviconURL = "http://www.apkmirror.com/favicon.ico";

    private final static int FCR = 1;
    private final static int REQUEST_WRITE_STORAGE_RESULT = 112;

    int themeColor = Color.parseColor("#f47d20");

    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;

    boolean navbar;
    boolean title;
    boolean interfaceUpdating;
    boolean isLollipop;
    boolean isNougat_MR1;

    Drawable drawable;

    Bitmap favico;

    @BindView(R.id.loading) ProgressBar Pbar;
    @BindView(R.id.progress) ProgressBar PbarSplash;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.logo) ImageView Logo;
    @BindView(R.id.splash_screen) View splash;
    @BindView(R.id.apkmirror) WebView mWebView;
    @BindView(R.id.swiperefresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.bottom_navigation) BottomNavigationView bottomBar;

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

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean crashlytics = prefs.getBoolean("crashlytics", true);
        boolean cache = prefs.getBoolean("cache", false);
        boolean javascript = prefs.getBoolean("javascript", false);
        boolean rotation = prefs.getBoolean("rotation", false);
        navbar = prefs.getBoolean("navbarcolor", true);
        title = prefs.getBoolean("title", false);
        interfaceUpdating = prefs.getBoolean("colorupdate", true);

        drawable = Pbar.getProgressDrawable();
        Intent OpenedFromExternalLink = getIntent();
        WebSettings WebViewSettings = mWebView.getSettings();
        isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        isNougat_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;

        if (!rotation) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (crashlytics) Fabric.with(this, new Crashlytics());

        if (navbar && isLollipop)
            getWindow().setNavigationBarColor(themeColor);

        //this has to be done because the recents color will stay orange which looks ugly
        if (isNougat_MR1) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_circle);
            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            this.setTaskDescription(taskDesc);
        } else {
            if (isLollipop) {
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, ContextCompat.getColor(getApplicationContext(), R.color.Recents));
                this.setTaskDescription(taskDesc);
            }
        }

        try {
            Logo.setImageBitmap(getBitmapFromAsset("logo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //sets the height of progressbar
        Pbar.setScaleY(2f);
        //checking for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, R.string.storage_access, Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_RESULT);
        }

        createShortcuts();
        createBottomBar();
        //Support for external links
        Uri data = OpenedFromExternalLink.getData();
        if (data != null)
            url = data.toString();
        else if (data == Uri.parse(urlDev))
            url = urlDev;
        else if (data == Uri.parse(urlUp))
            url = urlUp;
        else
            url = urlFrontPage;

        //setting webview
        WebViewSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        WebViewSettings.setJavaScriptEnabled(true);
        WebViewSettings.setLoadsImagesAutomatically(true);
        WebViewSettings.setUseWideViewPort(true);
        WebViewSettings.setLoadWithOverviewMode(false);
        WebViewSettings.setSaveFormData(true);
        WebViewSettings.setSavePassword(true);
        mWebView.setWebViewClient(new mWebClient());
        mWebView.setWebChromeClient(new mChromeClient());
        mWebView.setDownloadListener(new mDownloadManager());
        WebViewSettings.setAllowFileAccess(true);
        WebViewSettings.setAllowFileAccessFromFileURLs(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.setAcceptFileSchemeCookies(true);
        CookieSyncManager.createInstance(MainActivity.this);
        CookieSyncManager.getInstance().startSync();
        mWebView.loadUrl(url);
        mWebView.requestFocus(View.FOCUS_DOWN);
        currentUrl = mWebView.getUrl();


        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (currentUrl.equals("file:///android_asset/errorpage.html"))
                    mWebView.loadUrl(url);
                else mWebView.reload();
            }

        });
        swipeRefreshLayout.setColorSchemeColors(themeColor,themeColor,themeColor);

        if (cache) {
            WebViewSettings.setAppCacheEnabled(false);
            WebViewSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            mWebView.clearCache(true);
            mWebView.clearHistory();
        } else {
            WebViewSettings.setAppCacheEnabled(true);
            WebViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            mWebView.clearCache(false);
            mWebView.clearHistory();
        }

        if (javascript)
            WebViewSettings.setJavaScriptEnabled(false);

        else
            WebViewSettings.setJavaScriptEnabled(true);
    }


    private class mDownloadManager implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

            fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

            final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            Snackbar.make(findViewById(R.id.fab), getString(R.string.download_started) + "  (" + fileName + ")", 1500)
                    .show();

            final View.OnClickListener opendown = new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (isNougat_MR1) {
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

            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    Snackbar.make(findViewById(R.id.fab), getResources().getString(R.string.download) + " " + fileName, Snackbar.LENGTH_LONG)
                            .setAction(R.string.open, opendown)
                            .show();
                }
            };

            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            if (title) {
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
    }
    private class mWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            currentUrl = url;

            Pbar.animate()
                    .alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);

            Pbar.setVisibility(ProgressBar.VISIBLE);
            updateBottomBar();
            if (interfaceUpdating) {
                themeColorTask runner = new themeColorTask();
                runner.execute(currentUrl);
            }



        }

        @Override
        @SuppressWarnings("deprecation")
        public void onPageFinished(WebView view, String url) {
            currentUrl = url;

            if (swipeRefreshLayout.isRefreshing())
                swipeRefreshLayout.setRefreshing(false);

            CookieSyncManager.getInstance().sync();

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

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (!url.contains("apkmirror.com")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
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
            if (errorCode==-2) //this is the error code for no network
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
            // will update the "progress" propriety of progressbar until it reaches progress
            ObjectAnimator animation2 = ObjectAnimator.ofInt(PbarSplash, "progress", progress);
            animation2.setDuration(500); // 0.5 second
            animation2.setInterpolator(new DecelerateInterpolator());
            animation2.start();



            if (findViewById(R.id.splash_screen).getVisibility() == View.VISIBLE && progress >= 85) //makes the webview visible before ads load for faster experience
                crossfade(findViewById(R.id.splash_screen), findViewById(R.id.main_view));


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
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Choose APK");
            startActivityForResult(chooserIntent, FCR);
            return true;
        }

    }

    public void openSettings(){
        Intent i = new Intent
                (MainActivity.this, SettingsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
    //back key
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            themeColorTask runner = new themeColorTask();
            runner.execute(mWebView.getOriginalUrl());

            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
    //requesting the permission to write to external storage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == REQUEST_WRITE_STORAGE_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED){
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
        bottomBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int Y = mWebView.getScrollY();
                int X = mWebView.getScrollX();
                switch (item.getItemId()) {
                    case R.id.tab_homepage:
                        if (!bottomBar.getMenu().getItem(0).isChecked()){
                            mWebView.loadUrl(urlFrontPage);
                            currentUrl = urlFrontPage;

                        }else {
                            if (Y != 0)
                                mWebView.scrollTo(0,0);
                            else {
                                mWebView.loadUrl(urlFrontPage);
                                currentUrl = urlFrontPage;

                            }
                        }

                        break;
                    case R.id.tab_devs:
                        if (!bottomBar.getMenu().getItem(1).isChecked()){
                            mWebView.loadUrl(urlDev);
                            currentUrl = urlDev;

                        }else {
                            if (Y != 0)
                                mWebView.scrollTo(0,0);
                            else {
                                mWebView.loadUrl(urlDev);
                                currentUrl = urlDev;

                            }

                        }

                        break;
                    case R.id.tab_upload:
                        if (!bottomBar.getMenu().getItem(2).isChecked()){
                            mWebView.loadUrl(urlUp);
                            currentUrl = urlUp;

                        }else {
                            if (Y != 0)
                                mWebView.scrollTo(0,0);
                            else {
                                mWebView.loadUrl(urlUp);
                                currentUrl = urlUp;

                            }
                        }

                        break;
                    case R.id.tab_settings:
                        openSettings();
                        break;
                    case R.id.tab_exit:
                        finish();
                        break;
                }
                return true;
            }
        });

    }

    public void updateBottomBar(){
        //Try-catch prevents the app from crashing duo to loading some custom javascript (see above)
        try {
            if (currentUrl.matches(urlDev) && !bottomBar.getMenu().getItem(1).isChecked()) {
                bottomBar.getMenu().getItem(1).setChecked(true);
            } else if (currentUrl.matches(urlUp) && !bottomBar.getMenu().getItem(2).isChecked()) {
                bottomBar.getMenu().getItem(2).setChecked(true);
            } else if (!currentUrl.matches(urlDev) && !currentUrl.matches(urlUp) && !bottomBar.getMenu().getItem(0).isChecked()) {
                Log.d("Hi!", ":)");
            }
        }catch (NullPointerException e){
            Log.w("Javascript error", "Incorrect bottombar update");
        }
    }





    public void createShortcuts() {
        if (isNougat_MR1) {

            Context context = getBaseContext    ();
            ShortcutManager shortcutManager;
            shortcutManager = getSystemService(ShortcutManager.class);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
                        .setShortLabel(getString(R.string.latest_uploads))
                        .setLongLabel(getString(R.string.latest_uploads))
                        .setIcon(Icon.createWithResource(context, R.drawable.ic_upload_shortcut))
                        .setIntent(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(urlUp)))
                        .build();
            ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "id2")
                    .setShortLabel(getString(R.string.developers))
                    .setLongLabel(getString(R.string.developers))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_people_shortcut))
                    .setIntent(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(urlDev)))
                    .build();

            shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut, shortcut2));
        }


    }





    private class themeColorTask extends AsyncTask<String, Integer, Integer> {


        @Override
        protected Integer doInBackground(String... url) {
            try {
                // Downloading html source
                URLConnection connection = (new URL(url[0])).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                // Read and store the result line by line then return the entire string.
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder html = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    html.append(line);
                }
                in.close();

                Source source = new Source(html.toString());

                //theme color
                String extractedThemeColor = ColorDarkTheme;
                List<Element> elements = source.getAllElements("meta");
                for (Element element : elements) {
                    final String id = element.getAttributeValue("name"); // Get Attribute 'id'
                    if (id != null && id.equals("theme-color")) {
                        extractedThemeColor = element.getAttributeValue("content");
                    }
                }

                //App title
                appName = "APKMirror";
                List<Element> h1elements = source.getAllElements("h1");
                for (Element element : h1elements) {
                    final String id = element.getAttributeValue("class"); // Get Attribute 'id'
                    if (id != null && id.contains("app-title")) {
                        appName = element.getAttributeValue("title");
                    }
                }
                if (appName == null){
                    appName = "APKMirror";
                }

                //Favicon
                List<Element> IMGelements = source.getAllElements("img");
                for (Element element : IMGelements) {
                    final String id = element.getAttributeValue("style"); // Get Attribute 'id'
                    if (id != null && id.matches("width:96px; height:96px;")) {
                        faviconURL = "http://www.apkmirror.com" + element.getAttributeValue("src");
                    }
                }


                // Favicon download
                URLConnection favIconDownload = (new URL(faviconURL)).openConnection();
                favIconDownload.setConnectTimeout(5000);
                favIconDownload.setReadTimeout(5000);
                favIconDownload.connect();

                // Read and store the result line by line then return the entire string.
                InputStream favIconDownloadStream = favIconDownload.getInputStream();
                favico = BitmapFactory.decodeStream(favIconDownloadStream);


                if (extractedThemeColor != null && !extractedThemeColor.matches("#FF8B14")){
                    //convert the color to darker color so its nice and material
                    float[] hsv = new float[3];
                    int color = Color.parseColor(extractedThemeColor);
                    Color.colorToHSV(color, hsv);
                    hsv[2] *= 0.8f; // value component
                    themeColor = Color.HSVToColor(hsv);
                    return themeColor;
                }
                else {
                    //if color is not found we will return orange color
                    return Color.parseColor(ColorDarkTheme);
                }



            }catch (IOException e){
                e.printStackTrace();
                return null;
            }

        }


        @Override
        protected void onPostExecute(Integer result) {
            // updating interface
            if (isLollipop) {
                setSystemBarColor(themeColor);
            }

            fab.setBackgroundTintList(ColorStateList.valueOf(themeColor));
            drawable.setColorFilter(new LightingColorFilter(0xFF000000, themeColor));
            bottomBar.setBackgroundColor(themeColor);
            swipeRefreshLayout.setColorSchemeColors(themeColor,themeColor,themeColor);

            //updating recents

            if (isLollipop) {
                if (currentUrl.matches(urlFrontPage)) {
                    if (isNougat_MR1) {
                        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_circle);
                        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                        MainActivity.this.setTaskDescription(taskDesc);
                    } else {
                        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, ContextCompat.getColor(getApplicationContext(), R.color.Recents));
                        MainActivity.this.setTaskDescription(taskDesc);
                    }
                } else {
                    if (isNougat_MR1) {
                        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(appName, favico, themeColor);
                        MainActivity.this.setTaskDescription(taskDesc);
                    } else {
                        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(appName, favico, themeColor);
                        MainActivity.this.setTaskDescription(taskDesc);
                    }

                }
            }


        }

    }



    public void search(View view){
        mWebView.scrollTo(0,0);
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
                .setDuration(0)
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

    private Bitmap getBitmapFromAsset(String asset) throws IOException
    {
        AssetManager assetManager = getAssets();
        InputStream stream = assetManager.open(asset);
        return BitmapFactory.decodeStream(stream);
    }

    private void setSystemBarColor(int color){
        Window window = MainActivity.this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
        if (navbar) {
            window.setNavigationBarColor(color);
        }

    }


}
