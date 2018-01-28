package cf.vojtechh.apkmirror.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import cf.vojtechh.apkmirror.R;
import cf.vojtechh.apkmirror.classes.PageAsync;
import cf.vojtechh.apkmirror.interfaces.AsyncResponse;
import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends AppCompatActivity implements AdvancedWebView.Listener, AsyncResponse {

    private AdvancedWebView webView;
    private ProgressBar progressBar;
    private BottomBar navigation;
    private FloatingActionButton fabSearch;

    private SwipeRefreshLayout refreshLayout;
    private RelativeLayout settingsLayoutFragment;
    private RelativeLayout webContainer;
    private FrameLayout progressBarContainer;
    private LinearLayout firstLoadingView;

    private static final String APKMIRROR_URL = "http://www.apkmirror.com/";
    private static final String APKMIRROR_UPLOAD_URL = "http://www.apkmirror.com/apk-upload/";

    Integer shortAnimDuration;

    Integer previsionThemeColor = Color.parseColor("#FF8B14");

    SharedPreferences sharedPreferences;


    private boolean settingsShortcut = false;
    private boolean triggerAction = true;

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setTheme(R.style.AppTheme);
            setContentView(R.layout.activity_main);

            previsionThemeColor = Color.parseColor("#FF8B14");

            //Views
            refreshLayout = findViewById(R.id.refresh_layout);
            progressBar = findViewById(R.id.main_progress_bar);
            navigation = findViewById(R.id.navigation);
            settingsLayoutFragment = findViewById(R.id.settings_layout_fragment);
            webContainer = findViewById(R.id.web_container);
            firstLoadingView = findViewById(R.id.first_loading_view);
            webView = findViewById(R.id.main_webview);
            fabSearch = findViewById(R.id.fab_search);progressBarContainer = findViewById(R.id.main_progress_bar_container);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            initSearchFab();
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            shortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            initNavigation();

            boolean saveUrl = sharedPreferences.getBoolean("save_url", false);

            String url;

            Intent link = getIntent();
            Uri data = link.getData();


            if (data != null) {
                //App was opened from browser
                url = data.toString();
            } else {
                //data is null which means it was either launched from shortcuts or normally
                Bundle bundle = link.getExtras();
                if (bundle == null) {
                    //Normal start from launcher
                    if (saveUrl) {
                        url = sharedPreferences.getString("last_url", APKMIRROR_URL);
                    } else {
                        url = APKMIRROR_URL;
                    }
                } else {
                    //Ok it was shortcuts, check if it was settings
                    String bundleUrl = bundle.getString("url");
                    if (bundleUrl != null) {
                        if (bundleUrl.equals("apkmirror://settings")) {
                            //It was settings
                            url = APKMIRROR_URL;
                            navigation.selectTabWithId(R.id.navigation_settings);
                            crossFade(webContainer, settingsLayoutFragment);
                            settingsShortcut = true;
                        } else {
                            url = bundleUrl;
                        }
                    } else {
                        if (saveUrl) {
                            url = sharedPreferences.getString("last_url", APKMIRROR_URL);
                        } else {
                            url = APKMIRROR_URL;
                        }
                    }
                }
            }

            initWebView(url);

            //I know not the best solution xD
            if (!settingsShortcut) {
                firstLoadingView.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (firstLoadingView.getVisibility() == View.VISIBLE) {
                            crossFade(firstLoadingView, webContainer);
                        }
                    }
                }, 2000);
            }

        } catch (final RuntimeException e) {

            new MaterialDialog.Builder(this)
                    .title(R.string.error)
                    .content(R.string.runtime_error_dialog_content)
                    .positiveText(android.R.string.ok)
                    .neutralText(R.string.copy_log)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        finish();
                                    }
                                }
                    ).onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    // Gets a handle to the clipboard service.
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    // Creates a new text clip to put on the clipboard
                    ClipData clip = ClipData.newPlainText("log", e.toString());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }else{
                        Toast.makeText(MainActivity.this, getString(R.string.clip_error), Toast.LENGTH_LONG).show();
                    }
                }
            }).show();

        }
    }

    private void initNavigation() {

        //Making the bottom navigation do something
        navigation.setOnTabSelectListener(tabSelectListener);
        navigation.setOnTabReselectListener(tabReselectListener);

        if (sharedPreferences.getBoolean("show_exit", false)) {
            navigation.setItems(R.xml.navigation_exit);
            navigation.invalidate();
        }


    }

    private void initSearchFab() {
        Boolean fab = sharedPreferences.getBoolean("fab", true);
        if (fab) {
            fabSearch.show();
            fabSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    search();
                }
            });
        }
    }



    private void initWebView(String url) {

        webView.setListener(this, this);
        webView.addPermittedHostname("apkmirror.com");
        webView.setWebChromeClient(chromeClient);
        webView.setUploadableFileTypes("application/vnd.android.package-archive");
        webView.loadUrl(url);


        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (sharedPreferences.getBoolean("save_url", false) && !webView.getUrl().equals("apkmirror://settings")) {
            sharedPreferences.edit().putString("last_url", webView.getUrl()).apply();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Next line causes crash
        //webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Next line causes crash
        //webView.restoreState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {

        if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {

            if (!webView.onBackPressed()) {
                return;
            }

        } else {
            crossFade(settingsLayoutFragment, webContainer);
            if (webView != null && webView.getUrl().equals(APKMIRROR_UPLOAD_URL)) {
                triggerAction = false;
                navigation.selectTabWithId(R.id.navigation_upload);
            } else {
                triggerAction = false;
                navigation.selectTabWithId(R.id.navigation_home);

            }
            return;
        }
        super.onBackPressed();
    }


    public void runAsync(String url) {
        //getting apps
        PageAsync pageAsync = new PageAsync();
        pageAsync.response = MainActivity.this;
        pageAsync.execute(url);
    }

    private void search() {

        new MaterialDialog.Builder(this)
                .title(R.string.search)
                .inputRange(1, 100)
                .input(R.string.search, R.string.nothing, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if(dialog.getInputEditText()!=null) {
                            webView.loadUrl("http://www.apkmirror.com/?s=" + dialog.getInputEditText().getText());
                        }else {
                            Toast.makeText(MainActivity.this, getString(R.string.search_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .negativeText(android.R.string.cancel)
                .show();

    }

    private OnTabReselectListener tabReselectListener = new OnTabReselectListener() {
        @Override
        public void onTabReSelected(@IdRes int tabId) {
            Integer webScrollY = webView.getScrollY();

            if (tabId == R.id.navigation_home) {
                //Home re-pressed
                if (webScrollY != 0) {
                    //Scroll to top
                    webView.setScrollY(0);
                } else {
                    //Load url
                    webView.loadUrl(APKMIRROR_URL);
                }
            } else if (tabId == R.id.navigation_upload) {
                //Upload re-pressed
                if (webScrollY != 0) {
                    //Scroll to top
                    webView.setScrollY(0);
                } else {
                    //Load url
                    webView.loadUrl(APKMIRROR_UPLOAD_URL);
                }
            }
        }
    };

    private OnTabSelectListener tabSelectListener = new OnTabSelectListener() {
        @Override
        public void onTabSelected(@IdRes int tabId) {

            if (triggerAction) {

                if (tabId == R.id.navigation_home) {
                    //Home pressed
                    if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {
                        //settings is not visible
                        //Load url
                        webView.loadUrl(APKMIRROR_URL);

                    } else {
                        //settings is visible, gonna hide it
                        if (webView.getUrl().equals(APKMIRROR_UPLOAD_URL)) {
                            webView.loadUrl(APKMIRROR_URL);
                        }
                        crossFade(settingsLayoutFragment, webContainer);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            changeUIColor(previsionThemeColor);
                        }

                    }
                } else if (tabId == R.id.navigation_upload) {
                    //Upload pressed
                    if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {
                        //settings is not visible
                        //Load url
                        webView.loadUrl(APKMIRROR_UPLOAD_URL);
                    } else {
                        //settings is visible, gonna hide it
                        if (!webView.getUrl().equals(APKMIRROR_UPLOAD_URL)) {
                            webView.loadUrl(APKMIRROR_UPLOAD_URL);
                        }
                        crossFade(settingsLayoutFragment, webContainer);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            changeUIColor(previsionThemeColor);
                        }

                    }
                } else if (tabId == R.id.navigation_settings) {
                    //Settings pressed
                    if (firstLoadingView.getVisibility() == View.VISIBLE) {
                        firstLoadingView.setVisibility(View.GONE);
                    }
                    crossFade(webContainer, settingsLayoutFragment);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        changeUIColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                    }

                } else if (tabId == R.id.navigation_exit) {
                    finish();
                }

            }

            triggerAction = true;

        }
    };

    private void crossFade(final View toHide, View toShow) {

        toShow.setAlpha(0f);
        toShow.setVisibility(View.VISIBLE);

        toShow.animate()
                .alpha(1f)
                .setDuration(shortAnimDuration)
                .setListener(null);

        toHide.animate()
                .alpha(0f)
                .setDuration(shortAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        toHide.setVisibility(View.GONE);
                    }
                });
    }

    private void download(String url, String name) {

        if (!sharedPreferences.getBoolean("external_download", false)) {
            if (AdvancedWebView.handleDownload(this, url, name)) {
                Toast.makeText(MainActivity.this, getString(R.string.download_started), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.cant_download), Toast.LENGTH_SHORT).show();
            }
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }


    }

    private boolean isWritePermissionGranted() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onProcessFinish(Integer themeColor) {

        // updating interface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            changeUIColor(themeColor);
        }
        previsionThemeColor = themeColor;

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeUIColor(Integer color) {

        ValueAnimator anim = ValueAnimator.ofArgb(previsionThemeColor, color);
        anim.setEvaluator(new ArgbEvaluator());

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                progressBar.getProgressDrawable().setColorFilter(new LightingColorFilter(0xFF000000, (Integer) valueAnimator.getAnimatedValue()));
                setSystemBarColor((Integer) valueAnimator.getAnimatedValue());
                navigation.setActiveTabColor((Integer) valueAnimator.getAnimatedValue());
                fabSearch.setBackgroundTintList(ColorStateList.valueOf((Integer) valueAnimator.getAnimatedValue()));

            }
        });

        anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        anim.start();
        refreshLayout.setColorSchemeColors(color, color, color);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setSystemBarColor(int color) {

        int clr;

        //this makes the color darker or uses nicer orange color

        if (color != Color.parseColor("#FF8B14")) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 0.8f;
            clr = Color.HSVToColor(hsv);
        } else {
            clr = Color.parseColor("#F47D20");
        }

        Window window = MainActivity.this.getWindow();
        window.setStatusBarColor(clr);

    }

    private void setupNFC(String url) {

        if (nfcAdapter != null) { // in case there is no NFC

            try {
                // create an NDEF message containing the current URL:
                NdefRecord rec = NdefRecord.createUri(url); // url: current URL (String or Uri)
                NdefMessage ndef = new NdefMessage(rec);
                // make it available via Android Beam:
                nfcAdapter.setNdefPushMessage(ndef, this, this);

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }


    //WebView factory methods bellow
    @Override
    public void onPageStarted(String url, Bitmap favicon) {


        if (!url.contains("http://www.apkmirror.com/wp-content/")) {

            runAsync(url);
            setupNFC(url);

            //Updating bottom navigation
            if (navigation.getCurrentTabId() == R.id.navigation_home) {
                if (url.equals(APKMIRROR_UPLOAD_URL)) {
                    triggerAction = false;
                    navigation.selectTabWithId(R.id.navigation_upload);
                }
            } else if (navigation.getCurrentTabId() == R.id.navigation_upload) {
                if (!url.equals(APKMIRROR_UPLOAD_URL)) {
                    triggerAction = false;
                    navigation.selectTabWithId(R.id.navigation_home);

                }
            }

            //Showing progress bar
            progressBarContainer.animate()
                    .alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            progressBarContainer.setVisibility(View.VISIBLE);
                        }
                    });

        }
    }

    @Override
    public void onPageFinished(String url) {

        progressBarContainer.animate()
                .alpha(0f)
                .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        progressBarContainer.setVisibility(View.GONE);
                    }
                });

        if (refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }

    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {

        if (errorCode == -2) {
            new MaterialDialog.Builder(this)
                    .title(R.string.error)
                    .content(getString(R.string.error_while_loading_page) + " " + failingUrl + "(" + String.valueOf(errorCode) + " " + description + ")")
                    .positiveText(R.string.refresh)
                    .negativeText(android.R.string.cancel)
                    .neutralText("Dismiss")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            webView.reload();
                            dialog.dismiss();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .show();
        }

    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

        if (isWritePermissionGranted()) {
            download(url, suggestedFilename);
        } else {
            new MaterialDialog.Builder(MainActivity.this)
                    .title(R.string.write_permission)
                    .content(R.string.storage_access)
                    .positiveText(R.string.request_permission)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            //Request permission
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    })
                    .show();
        }

    }


    @Override
    public void onExternalPageRequest(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private WebChromeClient chromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int progress) {

            //update the progressbar value
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progress);
            animation.setDuration(100); // 0.5 second
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();

        }

    };

}
