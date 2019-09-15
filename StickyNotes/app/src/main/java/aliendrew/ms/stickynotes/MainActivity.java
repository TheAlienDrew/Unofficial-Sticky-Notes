package aliendrew.ms.stickynotes;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

public class MainActivity extends AppCompatActivity {

    // for the toggle button
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_DARK_THEME = "dark_theme";

    private WebView splashview;

    private NoSuggestionsWebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use the chosen theme
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final boolean useDarkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleTheme);
        toggle.setChecked(useDarkTheme);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleTheme(isChecked);
            }
        });

        webview = (NoSuggestionsWebView) findViewById(R.id.webView);

        // splash loading
        splashview = (WebView) findViewById(R.id.splashView);
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

            // INTERNET DETECTION START
            public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
                try {
                    webView.stopLoading();
                } catch (Exception e) {
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
            // INTERNET DETECTION END

            // THEME MOD START
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(webview, url);

                if (useDarkTheme)
                    injectScriptFile(webview, "js/dark_theme.js");
                else
                    injectScriptFile(webview, "js/light_theme.js");

                webview.loadUrl("javascript:setTimeout(test(), 0)");

                toggle.setVisibility(View.VISIBLE);
                webview.setVisibility(View.VISIBLE);
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
            // THEME MOD END
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
}