package aliendrew.ms.stickynotes;

// NOTES:
//
// Android emulators are not updated with the newest version of Android System WebView and,
//   therefore, are not reliable for testing this app below API 24. When not using an Android Studio
//   emulator, at least API 21 (Android Lollipop fully updated) runs the app with no problems.
// Since Android KitKat 4.4.4 (APIs 19 & 20) doesn't have a separate Android System WebView app that
//   can be updated, the CSS4 code being used on https://onenote.com/stickynotes (CSS4) will not
//   work. More specifically https://caniuse.com/#feat=css-variables&compare=android+4.4.3-4.4.4

// basics
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Toast;
import android.content.Intent;
// no internet connection detection
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
// toggle theme requirements
import android.view.KeyEvent;
import android.view.View;
import android.content.SharedPreferences;
// webView basics
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.net.Uri;
// webView flicker fix
import android.webkit.JavascriptInterface;
import android.os.Handler;
// webView javascript injection
import java.io.InputStream;
import java.io.IOException;
import java.util.Objects;
import android.util.Base64;
// webView file uploads
import android.webkit.ValueCallback;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import android.app.Activity;
import android.provider.MediaStore;

public class MainActivity extends ImmersiveAppCompatActivity {

    // general app controls
    private boolean singleBack = false;

    // file upload initialize
    public static final int INPUT_FILE_REQUEST_CODE = 1;

    // use the chosen theme
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_DARK_THEME = "dark_theme";
    private boolean useDarkTheme = false;

    // function to see if we're online
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        @SuppressWarnings("ConstantConditions") NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private NoSuggestionsWebView webStickies;

    // other needed file upload variables
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // functions for file upload
    // code via http://developer.android.com/training/camera/photobasics.html
    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data == null) {
                // If there is not data, then we may have taken a photo
                if(mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        useDarkTheme = preferences.getBoolean(PREF_DARK_THEME, false);

        webStickies = findViewById(R.id.webView);

        if (useDarkTheme) {
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            webStickies.setBackgroundColor(Color.WHITE);
        }

        webStickies.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if(mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if(takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });
        webStickies.setWebViewClient(new WebViewClient() {

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
        });

        // allows for caching the website when using it offline
        webStickies.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webStickies.getSettings().setAllowFileAccess(true);
        webStickies.getSettings().setAppCacheEnabled(true);
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

        // load webView online/offline depending on situation...
        // it will load online by default
        webStickies.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        if ( !isNetworkAvailable() ) {
            webStickies.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }

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
            //toggleTheme(!useDarkTheme); // back button reloads the page into the other theme

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
                    singleBack=false;
                }
            }, 2000);
        }
        return true;
    }

    // toggles theme in shared preferences
    private void toggleTheme(boolean darkTheme) {
        if (darkTheme) {
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
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