package aliendrew.ms.stickynotes;

// basics
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
// webview stuff
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.Uri;
// no internet connection detection
import android.app.AlertDialog;
import android.content.DialogInterface;
// used to inject JS files
import java.io.InputStream;
import java.io.IOException;
import android.util.Base64;
// toggle button stuff
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
// splash screen stuff
import android.graphics.Color;
// webview flicker fix
import android.webkit.JavascriptInterface;
import android.annotation.SuppressLint;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    // for the toggle button
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_DARK_THEME = "dark_theme";
    private ToggleButton toggle;

    private NoSuggestionsWebView webview;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use the chosen theme
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final boolean useDarkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggle = (ToggleButton) findViewById(R.id.toggleTheme);
        toggle.setChecked(useDarkTheme);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleTheme(isChecked);
            }
        });

        webview = (NoSuggestionsWebView) findViewById(R.id.webView);

        // splash loading
        WebView splashview = (WebView) findViewById(R.id.splashView);
        splashview.setVerticalScrollBarEnabled(false);
        splashview.setHorizontalScrollBarEnabled(false);
        splashview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        if (useDarkTheme) {
            splashview.setBackgroundColor(Color.BLACK);
            splashview.loadUrl("file:///android_asset/html/loading-dark.html");
            webview.setBackgroundColor(Color.BLACK);
        } else
            splashview.loadUrl("file:///android_asset/html/loading-light.html");

        webview.setWebViewClient(new WebViewClient() {

            // INTERNET DETECTION block
            public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
                try {
                    webView.stopLoading();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (webView.canGoBack()) {
                    webView.goBack();
                }

                webView.loadUrl("about:blank");
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Check your internet connection and try again.");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try Again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        startActivity(getIntent());
                    }
                });

                alertDialog.show();
                super.onReceivedError(webView, errorCode, description, failingUrl);
            }

            // LINKS OPEN AS EXTERNAL block
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (// open in default browser exception
                        url.startsWith("https://www.onenote.com/common1pauth/exchangecode?error=msa_signup")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else if (// open in Webview
                        url.startsWith("https://www.onenote.com/stickynotes")
                        || url.startsWith("https://www.onenote.com/common1pauth/signin?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fstickynotes")
                        || url.startsWith("https://login.windows.net/common/oauth2/authorize")
                        || url.startsWith("https://login.microsoftonline.com/common/oauth2/authorize")
                        || url.startsWith("https://www.onenote.com/common1pauth/exchangecode")
                        || url.startsWith("https://login.live.com/oauth20_authorize.srf")
                        || url.startsWith("https://login.live.com/ppsecure/post.srf")
                        || url.startsWith("https://www.onenote.com/common1pauth/msaimplicitauthcallback?redirectUrl=https%3a%2f%2fwww.onenote.com%2fstickynotes")
                        || url.startsWith("https://www.onenote.com/common1pauth/signout?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fcommon1pauth%2Fsignin%3FredirectUrl%3Dhttps%253A%252F%252Fwww.onenote.com%252Fstickynotes")
                ){
                    return false;
                }
                // open rest of URLS in default browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            // THEME MOD block
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(webview, url);

                if (useDarkTheme)
                    injectScriptFile(webview, "js/dark_theme.js");
                else
                    injectScriptFile(webview, "js/light_theme.js");

                webview.loadUrl("javascript: window.CallToAnAndroidFunction.setVisible()");
            }

            private void injectScriptFile(WebView view, String scriptFile) {
                InputStream input;
                try {
                    input = getAssets().open(scriptFile);
                    byte[] buffer = new byte[input.available()];
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // enables site to work
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        // visual fixes
        webview.setVerticalScrollBarEnabled(false);
        webview.setHorizontalScrollBarEnabled(false);
        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        // fixes keyboard suggestions/auto correct on some keyboards
        webview.getSettings().setSaveFormData(false);
        webview.clearFormData();
        webview.clearFormData();

        webview.addJavascriptInterface(new myJavaScriptInterface(), "CallToAnAndroidFunction");
        webview.loadUrl("https://www.onenote.com/stickynotes");

    }

    private void toggleTheme(boolean darkTheme) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_DARK_THEME, darkTheme);
        editor.apply();

        Intent intent = getIntent();
        finish();

        startActivity(intent);
    }

    public class myJavaScriptInterface {
        @JavascriptInterface
        public void setVisible(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toggle.setVisibility(View.VISIBLE);
                            webview.setVisibility(View.VISIBLE);
                        }
                    }, 500);
                }
            });
        }
    }
}