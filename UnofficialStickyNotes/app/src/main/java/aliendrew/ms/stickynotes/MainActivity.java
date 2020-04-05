package aliendrew.ms.stickynotes;

// NOTES:
//
// Android emulators are not updated with the newest version of Android System WebView and,
//   therefore, are not reliable for testing this app below API 24. When not using an Android Studio
//   emulator, at least API 21 (Android Lollipop fully updated) runs the app with no problems.
// Since Android KitKat (APIs 19) doesn't have a separate Android System WebView app that can be
//   updated, the CSS4 code being used on https://onenote.com/stickynotes (CSS4) will not work. More
//   specifically https://caniuse.com/#feat=css-variables&compare=android+4.4.3-4.4.4
//
// TODO: At this time, the camera function for taking pictures is broken on API 29 (Android 10), so
//   only the file chooser is working until the issue is fixed.

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends ImmersiveAppCompatActivity {

    // general app controls
    private boolean singleBack = false;

    // file upload initialize
    private static final String TAG = MainActivity.class.getSimpleName();
    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location
    private final static int file_req_code = 1;

    // use the chosen theme
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_THEME = "theme";
    private boolean useSystemTheme = false;
    private boolean useDarkTheme = false;

    // check system theme
    private boolean isSystemDark() {
        return ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
    }

    // function to see if we're online
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        @SuppressWarnings("ConstantConditions") NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private NoSuggestionsWebView webStickies;

    // functions for permissions
    public boolean file_permission(){
        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        }else{
            return true;
        }
    }

    // functions for camera photo and file upload
    // code via https://github.com/mgks/Os-FileUp
    private File create_image() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Uri[] results = null;

        /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/
        if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == file_req_code) {
                file_path.onReceiveValue(null);
                return;
            }
        }

        /*-- continue if response is positive --*/
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == file_req_code) {
                if (null == file_path) {
                    return;
                }

                ClipData clipData;
                String stringData;
                try {
                    clipData = intent.getClipData();
                    stringData = intent.getDataString();
                } catch (Exception e) {
                    clipData = null;
                    stringData = null;
                }

                if (clipData == null && stringData == null && cam_file_data != null) {
                    results = new Uri[]{Uri.parse(cam_file_data)};
                } else {
                    if (clipData != null) { // checking if multiple files selected or not
                        final int numSelectedFiles = clipData.getItemCount();
                        results = new Uri[numSelectedFiles];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else {
                        results = new Uri[]{Uri.parse(stringData)};
                    }
                }
            }
        }
        file_path.onReceiveValue(results);
        file_path = null;
    }
    public class ChromeClient extends WebChromeClient {
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (file_permission()) {
                file_path = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = create_image();
                        takePictureIntent.putExtra("PhotoPath", cam_file_data);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        cam_file_data = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        cam_file_data = null;
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, file_req_code);
                return true;
            } else return false;
        }
    }

    // altered webView client to work with sticky notes
    public class ViewClient extends WebViewClient {
        // INTERNET FAIL DETECTION block
        public void onReceivedError(final WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if (view.canGoBack()) {
                view.goBack();
            }

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Check your internet connection and try again.");
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    view.reload();
                }
            });
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
            Objects.requireNonNull(alertDialog.getWindow()).getAttributes().verticalMargin = 0.3F;

            view.setVisibility(View.GONE);
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
                view.setVisibility(View.GONE);
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
                injectScriptFile(view, "js/dark_theme.js");
            else
                injectScriptFile(view, "js/light_theme.js");

            view.loadUrl("javascript: window.CallToAnAndroidFunction.setVisible()");
            // close keyboard that automatically opens on reload
            view.loadUrl("javascript: setTimeout(function(){document.activeElement.blur()},1100)");
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
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get theme
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String prefTheme = preferences.getString(PREF_THEME, "system");
        switch (prefTheme) {
            case "dark":
                useDarkTheme = true;
                break;
            case "system":
                useSystemTheme = true;
                if (isSystemDark()) useDarkTheme = true;
                break;
        }

        // get view
        webStickies = findViewById(R.id.webView);
        WebSettings webSettings = webStickies.getSettings();

        if (useDarkTheme) {
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            webStickies.setBackgroundColor(Color.WHITE);
        }

        // set website clients
        webStickies.setWebChromeClient(new ChromeClient());
        webStickies.setWebViewClient(new ViewClient());
        // allows for caching the website when using it offline
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        // enables site to work
        webSettings.setJavaScriptEnabled(true);
        webStickies.addJavascriptInterface(new myJavaScriptInterface(), "CallToAnAndroidFunction");
        webSettings.setDomStorageEnabled(true);
        // fixes keyboard suggestions/auto correct on some keyboards
        webSettings.setSaveFormData(false);
        webStickies.clearFormData();
        // visual fixes
        webStickies.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webStickies.setVerticalScrollBarEnabled(false);
        webStickies.setHorizontalScrollBarEnabled(false);
        // TODO: testing
        webStickies.clearCache(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // load webView online/offline depending on situation...
        // it will load online by default
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if ( !isNetworkAvailable() ) {
            webSettings.setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }

        webStickies.loadUrl("https://www.onenote.com/stickynotes");
    }

    // make back button exit app
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // must press back twice within 2 seconds to exit app
            if (singleBack) {
                super.onBackPressed();
                return true;
            }
            this.singleBack = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    singleBack = false;
                }
            }, 2000);
        }

        return true;
    }
    // shift through theme settings
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (useSystemTheme) {
                useSystemTheme = false;
                toggleTheme("dark");
            } else if (!useDarkTheme) {
                useSystemTheme = true;
                toggleTheme("system");
            } else toggleTheme("dark");
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (useSystemTheme) {
                useSystemTheme = false;
                toggleTheme("light");
            } else if (useDarkTheme) {
                useSystemTheme = true;
                toggleTheme("system");
            } else toggleTheme("light");
        }

        return true;
    }

    // toggles theme in shared preferences
    private void toggleTheme(String theTheme) {
        String displayToast = theTheme.substring(0, 1).toUpperCase() + theTheme.substring(1).toLowerCase() + " theme enabled";
        Toast.makeText(this, displayToast, Toast.LENGTH_SHORT).show();

        // determine theme by user choice or by system settings
        boolean darkTheme = false;
        if (theTheme.equals("dark") || ((theTheme.equals("system") && isSystemDark()))) {
            darkTheme = true;
            webStickies.setBackgroundColor(Color.BLACK);
        } else webStickies.setBackgroundColor(Color.WHITE);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(PREF_THEME, theTheme);
        editor.apply();

        useDarkTheme = darkTheme;
        webStickies.setVisibility(View.GONE);
        webStickies.reload();
    }

    // used to enable webView after fully loaded theme
    class myJavaScriptInterface {
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