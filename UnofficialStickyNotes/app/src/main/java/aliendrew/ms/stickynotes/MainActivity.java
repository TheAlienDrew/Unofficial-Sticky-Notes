package aliendrew.ms.stickynotes;

// basics
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
// used to inject JS files
import java.io.InputStream;
import java.io.IOException;
import android.util.Base64;
// no internet connection detection
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager.LayoutParams;
// toggle theme requirements
import android.view.KeyEvent;
import android.view.View;
import android.content.SharedPreferences;
// webView stuff
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
// webView flicker fix
import android.webkit.JavascriptInterface;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    // Use the chosen theme
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_DARK_THEME = "dark_theme";
    private boolean useDarkTheme = false;

    private WebView loadDark;
    private WebView loadLight;
    private NoSuggestionsWebView webStickies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        useDarkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        // dark load screen
        loadDark = findViewById(R.id.loadingDark);
        loadDark.setVerticalScrollBarEnabled(false);
        loadDark.setHorizontalScrollBarEnabled(false);
        loadDark.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        loadDark.setBackgroundColor(Color.BLACK);
        loadDark.loadUrl("file:///android_asset/html/loading-dark.html");
        // light load screen
        loadLight = findViewById(R.id.loadingLight);
        loadLight.setVerticalScrollBarEnabled(false);
        loadLight.setHorizontalScrollBarEnabled(false);
        loadLight.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        loadLight.setBackgroundColor(Color.WHITE);
        loadLight.loadUrl("file:///android_asset/html/loading-light.html");

        webStickies = findViewById(R.id.webView);

        if (useDarkTheme) {
            loadDark.setVisibility(View.VISIBLE);
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            loadLight.setVisibility(View.VISIBLE);
            webStickies.setBackgroundColor(Color.WHITE);
        }

        webStickies.setWebChromeClient(new WebChromeClient());
        webStickies.setWebViewClient(new WebViewClient() {

            // INTERNET DETECTION block
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

                if (view.canGoBack()) {
                    view.goBack();
                }

                AlertDialog alertDialog;
                if (useDarkTheme)
                    alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).create();
                else
                    alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Check your internet connection and try again.");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try Again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        webStickies.reload();
                    }
                });
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                });
                alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                alertDialog.getWindow().clearFlags(LayoutParams.FLAG_DIM_BEHIND);
                alertDialog.getWindow().getAttributes().verticalMargin = 0.3F;

                webStickies.setVisibility(View.GONE);
                alertDialog.show();
            }

            // LINKS OPEN AS EXTERNAL block
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (// open in default browser exception
                        url.startsWith("https://www.onenote.com/common1pauth/exchangecode?error=msa_signup")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else if (// open in webView
                        url.startsWith("https://www.onenote.com/stickynotes")
                                || url.startsWith("https://www.onenote.com/common1pauth/signin?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fstickynotes")
                                || url.startsWith("https://login.windows.net/common/oauth2/authorize")
                                || url.startsWith("https://login.microsoftonline.com/common/oauth2/authorize")
                                || url.startsWith("https://www.onenote.com/common1pauth/exchangecode")
                                || url.startsWith("https://login.live.com/oauth20_authorize.srf")
                                || url.startsWith("https://login.live.com/ppsecure/post.srf")
                                || url.startsWith("https://www.onenote.com/common1pauth/msaimplicitauthcallback?redirectUrl=https%3a%2f%2fwww.onenote.com%2fstickynotes")
                                || url.startsWith("https://www.onenote.com/common1pauth/signout?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fcommon1pauth%2Fsignin%3FredirectUrl%3Dhttps%253A%252F%252Fwww.onenote.com%252Fstickynotes")
                ) {
                    webStickies.setVisibility(View.GONE);
                    return false;
                } else {// open rest of URLS in default browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }

            // THEME MOD block
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (useDarkTheme)
                    injectScriptFile(webStickies, "js/dark_theme.js");
                else
                    injectScriptFile(webStickies, "js/light_theme.js");

                webStickies.loadUrl("javascript: window.CallToAnAndroidFunction.setVisible()");
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
                    view.loadUrl("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var script = document.createElement('script');" +
                            "script.type = 'text/javascript';" +
                            // Tell the browser to BASE64-decode the string into your script !!!
                            "script.innerHTML = window.atob('" + encoded + "');" +
                            "parent.appendChild(script)" +
                            "})()");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // enables site to work
        webStickies.getSettings().setJavaScriptEnabled(true);
        webStickies.addJavascriptInterface(new myJavaScriptInterface(), "CallToAnAndroidFunction");
        webStickies.getSettings().setDomStorageEnabled(true);
        // fixes keyboard suggestions/auto correct on some keyboards
        webStickies.getSettings().setSaveFormData(false);
        webStickies.clearFormData();
        // visual fixes
        webStickies.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        //webStickies.getSettings().setUseWideViewPort(true);
        //webStickies.getSettings().setLoadWithOverviewMode(true);
        webStickies.setVerticalScrollBarEnabled(false);
        webStickies.setHorizontalScrollBarEnabled(false);

        webStickies.loadUrl("https://www.onenote.com/stickynotes");
    }

    // button controls to change theme/reload page
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // volume buttons can toggle to a, or reload on a current, theme
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            toggleTheme(false);
        } else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            toggleTheme(true);
        } else if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            // back button reloads the page into the other theme
            toggleTheme(!useDarkTheme);
        }
        return true;
    }

    // toggles theme in shared preferences
    private void toggleTheme(boolean darkTheme) {
        if (darkTheme) {
            loadDark.setVisibility(View.VISIBLE);
            loadLight.setVisibility(View.GONE);
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            loadDark.setVisibility(View.GONE);
            loadLight.setVisibility(View.VISIBLE);
            webStickies.setBackgroundColor(Color.WHITE);
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_DARK_THEME, darkTheme);
        editor.apply();

        useDarkTheme = darkTheme;
        webStickies.setVisibility(View.GONE);
        webStickies.reload();
    }

    // used to enable webView after fully loaded theme
    public class myJavaScriptInterface {
        @JavascriptInterface
        public void setVisible(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            webStickies.setVisibility(View.VISIBLE);
                        }
                    }, 800);
                }
            });
        }
    }
}