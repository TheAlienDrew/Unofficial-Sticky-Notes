package aliendrew.ms.stickynotes;

/* Copyright (C) 2020  Andrew Larson (thealiendrew@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
//import java.util.Locale;

/**
 * The configuration screen for the {@link NoteWidget NoteWidget} AppWidget.
 */
public class NoteWidgetConfigureActivity extends Activity {
    // constants
    private static final String STICKY_NOTES_URL = "https://www.onenote.com/stickynotes";
    /*private static final String APP_VERSION = BuildConfig.VERSION_NAME;
    private static final String APP_NAME = "Unofficial Sticky Notes";*/
    // user locale
    /*private final Locale USER_LOCALE = Locale.getDefault();
    private final String USER_LANGUAGE = USER_LOCALE.getLanguage();
    private final String USER_COUNTRY = USER_LOCALE.getCountry();*/

    // general app controls
    private static final String PREFS_NAME = "prefs";
    private ImageView splashImage;
    private boolean appLaunched = true;
    private boolean cacheErrorSent = false;

    // use the chosen theme
    private static final String PREF_THEME = "theme";
    private boolean useDarkTheme = false;

    // webView variables
    private WebView webLoadingDark;
    private WebView webLoadingLight;
    private WebView webStickies;

    // javascript interface (variable map)
    private static ImmutableMap<String, Integer> STRING_INTEGERS;

    // widget preference constants
    private static final String PREFS_WIDGET_NAME = "aliendrew.ms.stickynotes.NoteWidget";
    private static final String PREF_WIDGET_PREFIX_KEY = "notewidget_";
    // widget variables
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    // check system theme
    private boolean isSystemDark() {
        return ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
    }

    // function to see if we're online
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        boolean isConnected = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network[] networks = cm.getAllNetworks();
            if(networks.length>0){
                for(Network network :networks){
                    NetworkCapabilities nc = cm.getNetworkCapabilities(network);
                    if (nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                        isConnected = true;
                }
            }
        } else {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return isConnected;
    }

    // alert dialog functions to make sure theme is applied
    private AlertDialog createAlertDialog(Context context) {
        AlertDialog alertDialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (useDarkTheme) alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogDark).create();
            else alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogLight).create();
        } else {
            if (useDarkTheme) alertDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK).create();
            else alertDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).create();
        }
        return alertDialog;
    }
    private void alertDialogThemeButtons(Context context, AlertDialog alertDialog) {
        if (useDarkTheme) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccentDark));
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(context, R.color.colorAccentDark));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccentDark));
        }
    }

    // altered clients to work with sticky notes
    public class ChromeClient extends WebChromeClient {
        // allow JS alert dialogs
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // modified to not show website url
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    NoteWidgetConfigureActivity.this);
            //noinspection Convert2Lambda
            builder.setMessage(message)
                    .setNeutralButton(getString(R.string.okayBtn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            arg0.dismiss();
                        }
                    }).show();
            result.cancel();
            return true;
        }
    }
    public class ViewClient extends WebViewClient {
        // INTERNET FAIL DETECTION block
        @Override
        public void onReceivedError(final WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            // if there is no view can't really show a error for it
            // or if the page loaded a cached url of a page
            // or if the error has already been sent to the user, also exit
            if (view == null || !view.getUrl().equals(failingUrl) || cacheErrorSent) return;

            final AlertDialog alertDialog = createAlertDialog(NoteWidgetConfigureActivity.this);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setTitle(getString(R.string.errorNotCached));
            alertDialog.setMessage(getString(R.string.checkInternet));
            //noinspection Convert2Lambda
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.tryAgainBtn), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cacheErrorSent = false;
                    internetCacheLoad(view, null);
                }
            });
            //noinspection Convert2Lambda
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.quitBtn), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cacheErrorSent = false;
                    finish();
                }
            });
            //noinspection Convert2Lambda
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface arg0) {
                    alertDialogThemeButtons(NoteWidgetConfigureActivity.this, alertDialog);
                }
            });

            // couldn't go back so, there is no page, must hide webView
            view.setVisibility(View.INVISIBLE);
            alertDialog.show();
            cacheErrorSent = true;
        }
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error){
            onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
        }

        // LINKS OPEN AS EXTERNAL block
        private boolean checkLink(String url) {
            return url.startsWith(STICKY_NOTES_URL)
                    || url.startsWith("https://www.onenote.com/common1pauth/signin?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fstickynotes")
                    || url.startsWith("https://login.windows.net/common/oauth2/authorize")
                    || url.startsWith("https://login.microsoftonline.com/common/oauth2/authorize")
                    || url.startsWith("https://login.live.com/oauth20_authorize.srf")
                    || url.startsWith("https://login.live.com/ppsecure/post.srf")
                    || url.startsWith("https://www.onenote.com/common1pauth/msaimplicitauthcallback?redirectUrl=https%3a%2f%2fwww.onenote.com%2fstickynotes")
                    || url.startsWith("https://login.live.com/logout.srf")
                    || url.startsWith("https://www.onenote.com/common1pauth/signout?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fcommon1pauth%2Fsignin%3FredirectUrl%3Dhttps%253A%252F%252Fwww.onenote.com%252Fstickynotes")
                    || url.startsWith("https://account.live.com/password/reset?wreply=https%3a%2f%2flogin.microsoftonline.com%2fcommon%2freprocess")
                    || url.startsWith("https://www.onenote.com/common1pauth/exchangecode");
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // open in webView
            if (checkLink(url)) return false;
            else { // open rest of URLS in default browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        }
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // open in webView
            if (checkLink(url)) return false;
            else { // open rest of URLS in default browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        }

        // THEME MOD block
        @Override
        public void onPageStarted(WebView view, String url, Bitmap bitmap) {
            super.onPageStarted(view, url, bitmap);

            // disallows auto keyboard popup
            splashImage.requestFocus();

            view.setVisibility(View.INVISIBLE);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Websites have their special needs when it comes to theming or added functionality
            // NOTE: visibility is handled by respective convert_*.js files
            if (url.startsWith(STICKY_NOTES_URL)) {
                // make the main sticky notes page act like a picker for the widget
                injectScriptFile(view, "js/disable_tooltips.js");
                injectScriptFile(view, "js/linkify.min.js"); // needed for creating links in widget
                injectScriptFile(view, "js/convert_stickyNotesPicker.js");
            } else {
                // make logins and other pages compatible with Android webView
                injectScriptFile(view, "js/convert_loginsOther.js");
            }
        }

        private void injectScriptFile(WebView view, String scriptFile) {
            InputStream input;
            try {
                input = getAssets().open(scriptFile);
                byte[] buffer = new byte[input.available()];
                //noinspection ResultOfMethodCallIgnored
                input.read(buffer);
                input.close();

                // String-ify the script byte-array using BASE64 encoding !!!
                String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                //noinspection SpellCheckingInspection
                view.evaluateJavascript("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'text/javascript';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "script.innerHTML = window.atob('" + encoded + "');" +
                        "parent.appendChild(script)" +
                        "})()", null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // custom loader to make sure offline cache works
    private void internetCacheLoad(WebView view, String url) {
        // load webView online/offline depending on situation... it will load online by default
        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        if (!isNetworkAvailable(this)) {
            view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        if (url != null) {
            if (url.length() > 0) {
                view.loadUrl(url);
            }
        } else {
            view.reload();
        }
    }

    // the following functions, before onCreate, are specific to widget

    public NoteWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget // TODO: reprogram this pref save
    static void saveNotePref(Context context, int appWidgetId, String dataJSON) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_WIDGET_NAME, 0).edit();
        prefs.putString(PREF_WIDGET_PREFIX_KEY + appWidgetId, dataJSON);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget. // TODO: reprogram this pref load
    // If there is no preference saved, cancel widget
    static String loadNotePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_WIDGET_NAME, 0);
        String dataJSON = prefs.getString(PREF_WIDGET_PREFIX_KEY + appWidgetId, null);
        if (dataJSON != null) {
            return dataJSON;
        } else { // TODO: MAKE THIS CANCEL THE WIDGET
            return "Error: Couldn't load note content.";
        }
    }

    static void deleteNotePref(Context context, int appWidgetId) { // TODO: reprogram this pref delete
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_WIDGET_NAME, 0).edit();
        prefs.remove(PREF_WIDGET_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.note_widget_configure);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // get preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String prefTheme = preferences.getString(PREF_THEME, "system");
        switch (prefTheme) {
            case "dark":
                useDarkTheme = true;
                break;
            case "system":
                useDarkTheme = isSystemDark();
                break;
        }
        // set string to integers map
        STRING_INTEGERS = ImmutableMap.of(
                "loginNotCached", R.string.loginNotCached
        );

        // need splash image to focus on it after webView reloads so keyboard doesn't auto popup
        splashImage = findViewById(R.id.splashImage);

        // loading spinners, NO loadURL or javascript toggled css theme classes because they cause flashes
        webLoadingDark = findViewById(R.id.loadingDark);
        webLoadingDark.setBackgroundColor(Color.BLACK);
        webLoadingDark.loadUrl("file:///android_asset/html/loading-dark.html");
        webLoadingLight = findViewById(R.id.loadingLight);
        webLoadingLight.setBackgroundColor(Color.WHITE);
        webLoadingLight.loadUrl("file:///android_asset/html/loading-light.html");

        // initialize primary webView
        webStickies = findViewById(R.id.webView);
        final WebSettings webSettings = webStickies.getSettings();
        //WebView.setWebContentsDebuggingEnabled(true); // TODO: turn off before official builds

        if (useDarkTheme) {
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            webStickies.setBackgroundColor(Color.WHITE);
        }

        // set website clients
        webStickies.setWebChromeClient(new NoteWidgetConfigureActivity.ChromeClient());
        webStickies.setWebViewClient(new NoteWidgetConfigureActivity.ViewClient());
        // required to show profile pictures and allow longer login sessions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webStickies, true);
        else
            CookieManager.getInstance().setAcceptCookie(true);
        // allows for caching the website when using it offline
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webSettings.setAppCacheEnabled(true);
        // enables site to work
        webStickies.clearCache(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webStickies.addJavascriptInterface(new NoteWidgetConfigureActivity.StickiesJS(), "Android");
        // visual fixes
        webStickies.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webStickies.setVerticalScrollBarEnabled(false);
        webStickies.setHorizontalScrollBarEnabled(false);
        webSettings.setTextZoom(100);

        // start the webView
        internetCacheLoad(webStickies, STICKY_NOTES_URL);
    }

    class StickiesJS {
        // check if it's dark theme for javascript
        @JavascriptInterface
        public boolean isDarkMode() {
            return useDarkTheme;
        }

        // used to enable webView after fully loaded theme
        @JavascriptInterface
        public void webViewSetVisible(boolean choice) {
            //noinspection Convert2Lambda
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (choice) {
                        webStickies.setVisibility(View.VISIBLE);
                        // make sure one loading screen is available after first launch
                        if (appLaunched) {
                            if (useDarkTheme) webLoadingDark.setVisibility(View.VISIBLE);
                            else webLoadingLight.setVisibility(View.VISIBLE);
                            appLaunched = false;
                        }
                    } else webStickies.setVisibility(View.INVISIBLE);
                }
            });
        }

        // returns Android strings to webView
        @JavascriptInterface
        // can't be null anyways
        public String getAndroidString(String stringVariable) {
            return getResources().getString(STRING_INTEGERS.get(stringVariable));
        }

        // create a new note widget from the data we get from the webView
        @JavascriptInterface
        public void createNoteWidget(String noteDataJSON) { // TODO: NEED TO IMPLEMENT ME!!!
            //noinspection Convert2Lambda
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Context context = NoteWidgetConfigureActivity.this;

                    // When the note is clicked, store the json string locally
                    saveNotePref(context, mAppWidgetId, noteDataJSON); // TODO: NEED TO SAVE NOTE ID TO WIDGET ID AND SAVE NOTE DATA IN NOTE ID

                    // It is the responsibility of the configuration activity to update the app widget
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    NoteWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                    // Make sure we pass back the original appWidgetId
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
            });
        }
    }
}