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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
    private String mCM = null;
    public String appName;

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
    boolean isNougat;

    Drawable drawable;

    Bitmap favico;

    @BindView(R.id.loading) ProgressBar Pbar;
    @BindView(R.id.progress) ProgressBar PbarSplash;
    @BindView(R.id.fab_search) FloatingActionButton fab;
    @BindView(R.id.fab_share) FloatingActionButton fabShare;
    @BindView(R.id.logo) ImageView Logo;
    @BindView(R.id.splash_screen) View splash;
    @BindView(R.id.apkmirror) ObservableWebView mWebView;
    @BindView(R.id.swiperefresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.bottom_navigation) BottomNavigationView bottomBar;
    @BindView(R.id.settings_layout_fragment) View settingsFragment;
    @BindView(R.id.items) RelativeLayout items;

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
    @JavascriptInterface
    @SuppressWarnings({"deprecation"})

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

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
        isNougat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

        if (!rotation) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (crashlytics) Fabric.with(this, new Crashlytics());

        if (navbar && isLollipop)
            getWindow().setNavigationBarColor(Color.parseColor("#cc6f10"));


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
        mWebView.setOnScrollChangedCallback(new mScrollCallback());
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

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(mWebView.getUrl());
            }
        });
    }

    private class mScrollCallback implements ObservableWebView.OnScrollChangedCallback{

        private int oldNumber = 0;
        private boolean isHidden = false;
        @Override
        public void onScroll(int l, int t) {
            if(t > oldNumber + 10){
                //user scrolled down
                if (!isHidden){
                    items.animate().translationY(items.getHeight()).setDuration(1000);

                    isHidden = true;
                }
            }
            else if (t < oldNumber - 10){
                //user scrolled up
                if (isHidden) {
                    items.animate().translationY(0).setDuration(350);
                    isHidden = false;
                }

            }
            oldNumber = t;

        }
    }
    private class mDownloadManager implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            final String fileName;
            if (title) {
                fileName=appName + ".apk";
            }else {
                fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            }

            final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            if (title) {
                request.setTitle(fileName);
            }else {
                request.setTitle(fileName + ".apk");
            }



            Snackbar.make(findViewById(R.id.fab_search), getString(R.string.download_started) + "  (" + fileName + ")", 1500).show();

            final View.OnClickListener opendown = new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (isNougat) {
                        File apk;
                        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        apk = new File(downloads, fileName);
                        Uri apkUri = FileProvider.getUriForFile(MainActivity.this, "cf.vojtechh.apkmirror.provider", apk);
                        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.setData(apkUri);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        File apkfile;
                        apkfile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + fileName);
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

                        Snackbar.make(findViewById(R.id.fab_search), getResources().getString(R.string.download) + " " + fileName, Snackbar.LENGTH_LONG)
                                .setAction(R.string.open, opendown)
                                .show();
                }
            };

            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            manager.enqueue(request);
        }
    }
    private class mWebClient extends WebViewClient {

        private String mainURL;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            currentUrl = url;
            mainURL = url;

            if (!url.equals(urlFrontPage) && !url.equals(urlDev) && !url.equals(urlUp)){
                fabShare.show();
            }else {
                fabShare.hide();
            }

            Pbar.animate()
                    .alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);

            Pbar.setVisibility(ProgressBar.VISIBLE);
            updateBottomBar();




        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // Workaround this issue by checking if the URL equals .
            if (interfaceUpdating) {
                if (mainURL.equals(url)) {
                    new themeColorTask().execute(url);
                }

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

            if (!url.contains("apkmirror.com") || url.contains("market://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                return true;
            }else {
                mWebView.loadUrl(url);
                return false;
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

            if(splash.getVisibility() == View.VISIBLE) {
                //splash screen progress bar
                // will update the "progress" propriety of progressbar until it reaches progress
                ObjectAnimator animation2 = ObjectAnimator.ofInt(PbarSplash, "progress", progress);
                animation2.setDuration(500); // 0.5 second
                animation2.setInterpolator(new DecelerateInterpolator());
                animation2.start();
            }



            if (findViewById(R.id.splash_screen).getVisibility() == View.VISIBLE && progress >= 90){
                //makes the webview visible before ads load for faster experience
                splash.setVisibility(View.GONE);
                findViewById(R.id.main_view).setVisibility(View.VISIBLE);
            }


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


    private void createBottomBar(){
        bottomBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int Y = mWebView.getScrollY();

                if (settingsFragment.getVisibility() == View.VISIBLE && item.getItemId() != R.id.tab_settings && item.getItemId() != R.id.tab_exit){
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.settingsrestart), Toast.LENGTH_SHORT).show();
                    MainActivity.this.recreate();
                }

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
                        swipeRefreshLayout.setVisibility(View.GONE);
                        Pbar.setVisibility(View.GONE);
                        fab.hide();
                        fabShare.hide();

                        bottomBar.setBackgroundColor(Color.parseColor("ff8b14"));

                        //updating recents

                        if (isLollipop) {

                            ActivityManager.TaskDescription taskDesc;

                            favico = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
                            taskDesc = new ActivityManager.TaskDescription(getString(R.string.settings), favico, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                            MainActivity.this.setTaskDescription(taskDesc);

                            setSystemBarColor(Color.parseColor("#ff8b14"));
                        }
                        //recycling the favicon bitmap so the app wont crash -- http://stackoverflow.com/questions/41401548/asynctask-memory-crash/
                        if(favico!=null) {
                            favico.recycle();
                        }

                        settingsFragment.setVisibility(View.VISIBLE);

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
            if (currentUrl.equals(urlDev) && !bottomBar.getMenu().getItem(1).isChecked()) {
                bottomBar.getMenu().getItem(1).setChecked(true);
            } else if (currentUrl.equals(urlUp) && !bottomBar.getMenu().getItem(2).isChecked()) {
                bottomBar.getMenu().getItem(2).setChecked(true);
            } else if (!currentUrl.equals(urlDev) && !currentUrl.equals(urlUp) && !bottomBar.getMenu().getItem(0).isChecked()) {
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
                    String correctUlr = url[0];
                    for (String something : url){
                        if (something != null){
                            correctUlr = something;
                        }
                    }

                    // Downloading html source
                    URLConnection connection = (new URL(correctUlr)).openConnection();
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
                    String extractedThemeColor = null;
                    List<Element> elements = source.getAllElements("meta");
                    for (Element element : elements) {
                        final String id = element.getAttributeValue("name"); // Get Attribute 'id'
                        if (id != null && id.equals("theme-color")) {
                            extractedThemeColor = element.getAttributeValue("content");
                        }
                    }

                    //App title
                    String appTitle = "APKMirror";
                    List<Element> h1elements = source.getAllElements("h1");
                    for (Element element : h1elements) {
                        final String id = element.getAttributeValue("class"); // Get Attribute 'id'
                        if (id != null && id.contains("app-title")) {
                            appTitle = element.getAttributeValue("title");
                        }
                    }
                    if (appTitle == null){
                        appTitle = "APKMirror";
                    }
                    appName = appTitle;

                    //Favicon
                    String faviconURL = "http://www.apkmirror.com/favicon.ico";

                    List<Element> IMGelements = source.getAllElements("img");
                    for (Element element : IMGelements) {
                        final String id = element.getAttributeValue("style"); // Get Attribute 'id'
                        if (id != null && id.equals("width:96px; height:96px;")) {
                            faviconURL = "http://www.apkmirror.com" + element.getAttributeValue("src");
                        }
                    }

                    // Favicon download
                    URLConnection favIconDownload = (new URL(faviconURL)).openConnection();
                    favIconDownload.setConnectTimeout(5000);
                    favIconDownload.setReadTimeout(5000);
                    favIconDownload.connect();

                    InputStream favIconDownloadStream = favIconDownload.getInputStream();
                    favico = BitmapFactory.decodeStream(favIconDownloadStream);
                    favIconDownloadStream.close();


                    if (extractedThemeColor != null) {
                        return Color.parseColor(extractedThemeColor);
                    } else {
                        Log.d("ThemeColor:","No color");
                        //if color is not found we will return null and handle it in onPostExecute
                        return null;

                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    //if error we will return null and handle it in onPostExecute
                    return null;
                }
            }


        @Override
        protected void onPostExecute(Integer result) {

            if (result != null) {
                // updating interface

                fab.setBackgroundTintList(ColorStateList.valueOf(result));
                fabShare.setBackgroundTintList(ColorStateList.valueOf(result));
                drawable.setColorFilter(new LightingColorFilter(0xFF000000, result));
                bottomBar.setBackgroundColor(result);
                swipeRefreshLayout.setColorSchemeColors(result, result, result);

                //updating recents

                if (isLollipop) {

                    ActivityManager.TaskDescription taskDesc;

                    if (currentUrl.equals(urlFrontPage)) {
                        favico = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);  //for frontpage
                        taskDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), favico, ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    } else {
                        taskDesc = new ActivityManager.TaskDescription(appName, favico, result); //setting the
                    }
                    MainActivity.this.setTaskDescription(taskDesc);

                    setSystemBarColor(result);
                }
                //recycling the favicon bitmap so the app wont crash -- http://stackoverflow.com/questions/41401548/asynctask-memory-crash/
                if(favico!=null) {
                    favico.recycle();
                }

            }
        }


    }

    private void share(String text){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));    }


    private void search(){
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.search_relative_layout);

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.search_dialog, rl, false);


        final EditText editText = (EditText) alertLayout.findViewById(R.id.textViewSearch);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(alertLayout);
        alertDialogBuilder.setTitle(R.string.search);


        final InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);


        editText.requestFocus();
        alertDialogBuilder.setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(mWebView != null){
                    mWebView.loadUrl("http://www.apkmirror.com/?s=" + editText.getText());
                    inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS,0);
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS,0);
            }
        }).setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS,0);
                    }
                }
        ).show();
    }



    private void setSystemBarColor(int color){

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        int clr = Color.HSVToColor(hsv);

        Window window = MainActivity.this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(clr);
        if (navbar) {
            window.setNavigationBarColor(clr);
        }

    }


}
