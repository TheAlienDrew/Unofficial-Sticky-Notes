package aliendrew.ms.stickynotes;

// NOTES:
//
// Android emulators are not updated with the newest version of Android System WebView and,
//   therefore, are not reliable for testing this app below API 24. When not using an Android Studio
//   emulator, at least API 21 (Android Lollipop fully updated) runs the app with no problems.
// Since Android KitKat (APIs 19) doesn't have a separate Android System WebView app that can be
//   updated, the CSS4 code being used on https://onenote.com/stickynotes will not work. More
//   specifically https://caniuse.com/#feat=css-variables&compare=android+4.4.3-4.4.4
//
// TODO: Microsoft uses an older version of DraftJS on Sticky Notes that wasn't compatible
//   with Android, and I'm not sure how to fix so it's using the latest version. So, for now, text
//   input is limited (no glide/voice, and no auto-text manipulations).
//
// TODO: Theme needs fix when uploading a photo

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;

import androidx.exifinterface.media.ExifInterface;

import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

// TODO: AND THEN FIX SCROLLBAR COLOR FOR POPUP after all is done, update graphics

public class MainActivity extends ImmersiveAppCompatActivity {
    // constants
    private static final String STICKY_NOTES_URL = "https://www.onenote.com/stickynotes";
    private static final String APP_VERSION = BuildConfig.VERSION_NAME;
    private static final String APP_NAME = "Unofficial Sticky Notes";
    private static final String POPUP_TITLE = APP_NAME + " v" + APP_VERSION;
    private static final String SAVE_DIRECTORY = Environment.DIRECTORY_PICTURES + File.separator + APP_NAME;

    // general app controls
    private boolean cacheErrorSent = false;
    private boolean appLaunched = true;
    private boolean disableReloading = true;
    private boolean singleBack = false;
    private ImageView splashImage;
    private SwipeRefreshLayout swipeRefresher;

    // file upload initialize
    private static final String TAG = MainActivity.class.getSimpleName();
    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location
    private static final int file_req_code = 1;
    // file blob converting javascript
    private static final String blobToBase64 = "javascript: (function() {var reader=new window.FileReader;const convertBlob=function(e){if(null!=e&&null!=e.href&&e.href.startsWith(\"blob\")){var r=e.href;fetch(r).then(e=>e.blob()).then(r=>{reader.readAsDataURL(r),reader.onload=function(r){var t=r.target.result;e.href=t,e.click()}})}};convertBlob(document.querySelector('.n-lightbox-download'));})()";

    // use the chosen theme
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_THEME = "theme";
    private boolean useSystemTheme = false;
    private boolean useDarkTheme = false;

    // for first time use
    private Dialog popupDialog;
    private WindowManager.LayoutParams popupLayoutParams;
    private static final String PREF_VERSION_USED = "version_used";
    private boolean updatedToNewVersion = false;

    // check system theme
    private boolean isSystemDark() {
        return ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
    }

    // function to see if we're online
    @SuppressWarnings("deprecation")
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        boolean isConnected = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // reprogram
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

    // webView variables
    private WebView webLoadingDark;
    private WebView webLoadingLight;
    private NoTextCorrectionsWebView webStickies;

    // functions for permissions
    public boolean file_permission(){
        if (Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        } else {
            return true;
        }
    }

    // alert dialog functions to make sure theme is applied
    @SuppressWarnings("deprecation")
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

    // functions for camera photo and file upload
    // code via https://github.com/mgks/Os-FileUp
    @SuppressWarnings("deprecation")
    private File create_image() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            // TODO: this is only a temp fix for when Android R comes out
            storageDir = getApplicationContext().getExternalFilesDir(SAVE_DIRECTORY);
        else
            // TODO: getExternalStoragePublicDirectory only works on Android Q because I've enabled
            //  requestLegacyExternalStorage in the AndroidManifest, otherwise it's deprecated
            storageDir = Environment.getExternalStoragePublicDirectory(SAVE_DIRECTORY);

        // need to create the directory if not already there
        if (storageDir != null && !storageDir.exists()) storageDir.mkdir();

        return File.createTempFile(imageFileName,".jpg",storageDir);
    }
    public static int getImageOrientation(String imagePath) {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
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
                    // Sticky Notes image upload doesn't read Android Exif data on some phones so,
                    // we have to rotate the image before it gets sent to Microsoft servers
                    Uri uri = Uri.parse(cam_file_data);
                    String filePath = uri.toString();
                    if (uri.getPath() != null) filePath = uri.getPath();
                    File file = new File(filePath);
                    int rotation = getImageOrientation(filePath);
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    Matrix m = new Matrix();
                    m.postRotate(rotation);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), m, true);

                    try (FileOutputStream out = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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

    // altered clients to work with sticky notes
    public class ChromeClient extends WebChromeClient {
        // enable file choosing
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

        // allow JS alert dialogs
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }
    }
    public class ViewClient extends WebViewClient {
        // INTERNET FAIL DETECTION block
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(final WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            // if there is no view can't really show a error for it
            // or if the page loaded a cached url of a page
            // or if the error has already been sent to the user, also exit
            if (view == null || !view.getUrl().equals(failingUrl) || cacheErrorSent) return;

            // is page history back is possible, exit from here
            if (view.canGoBack()) {
                view.goBack();
                return;
            }

            final AlertDialog alertDialog = createAlertDialog(MainActivity.this);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setTitle("Error, data not cached");
            alertDialog.setMessage("Check your internet connection and try again.");
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Try Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cacheErrorSent = false;
                    internetCacheLoad(view, null);
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Quit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cacheErrorSent = false;
                    finish();
                }
            });
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface arg0) {
                    alertDialogThemeButtons(MainActivity.this, alertDialog);
                }
            });
            Objects.requireNonNull(alertDialog.getWindow()).getAttributes().verticalMargin = 0.3F;

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
            return url.startsWith("https://www.onenote.com/stickynotes")
                    || url.startsWith("https://storage.live.com/mydata/myprofile/expressionprofile/profilephoto")
                    || url.startsWith("https://www.onenote.com/common1pauth/signin?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fstickynotes")
                    || url.startsWith("https://login.windows.net/common/oauth2/authorize")
                    || url.startsWith("https://login.microsoftonline.com/common/oauth2/authorize")
                    || url.startsWith("https://login.live.com/oauth20_authorize.srf")
                    || url.startsWith("https://login.live.com/ppsecure/post.srf")
                    || url.startsWith("https://www.onenote.com/common1pauth/msaimplicitauthcallback?redirectUrl=https%3a%2f%2fwww.onenote.com%2fstickynotes")
                    || url.startsWith("https://login.live.com/logout.srf")
                    || url.startsWith("https://www.onenote.com/common1pauth/signout?redirectUrl=https%3A%2F%2Fwww.onenote.com%2Fcommon1pauth%2Fsignin%3FredirectUrl%3Dhttps%253A%252F%252Fwww.onenote.com%252Fstickynotes")
                    || url.startsWith("https://account.live.com/password/reset?wreply=https%3a%2f%2flogin.microsoftonline.com%2fcommon%2freprocess")
                    || (url.startsWith("https://www.onenote.com/common1pauth/exchangecode") && !url.endsWith("?error=msa_signup"));
        }
        @SuppressWarnings("deprecation")
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

            // light theme is the Microsoft default
            if (useDarkTheme) injectScriptFile(view, "js/dark_theme.js");

            // make the website compatible with Android webView
            injectScriptFile(view, "js/webView_convert.js");

            // visibility is handled in webView_convert.js
            disableReloading = false;
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

    // custom loader to make sure offline cache works
    private void internetCacheLoad(WebView view, String url) {
        // load webView online/offline depending on situation... it will load online by default
        view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        if (!isNetworkAvailable(this)) {
            view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }

        if (url != null && url.length() > 0) view.loadUrl(url);
        else view.reload();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String prefVersionUse = preferences.getString(PREF_VERSION_USED, "0");
        String prefTheme = preferences.getString(PREF_THEME, "system");
        updatedToNewVersion = (!APP_VERSION.equals(prefVersionUse));
        switch (prefTheme) {
            case "dark":
                useDarkTheme = true;
                break;
            case "system":
                useSystemTheme = true;
                if (isSystemDark()) useDarkTheme = true;
                break;
        }

        // need splash image to focus on it after webView reloads so keyboard doesn't auto popup
        splashImage = findViewById(R.id.splashImage);

        // initialize swipe refresh layout, and disable it while first load happens
        swipeRefresher = findViewById(R.id.swipeContainer);
        swipeRefresher.setEnabled(false);

        // loading spinners, NO loadURL or javascript toggled css theme classes because they cause flashes
        webLoadingDark = findViewById(R.id.loadingDark);
        webLoadingDark.setBackgroundColor(Color.BLACK);
        webLoadingDark.loadUrl("file:///android_asset/html/loading-dark.html");
        webLoadingLight = findViewById(R.id.loadingLight);
        webLoadingLight.setBackgroundColor(Color.WHITE);
        webLoadingLight.loadUrl("file:///android_asset/html/loading-light.html");

        // initialize primary webView
        webStickies = findViewById(R.id.webView);
        theWebView = webStickies; // NEEDED FOR ImmersiveAppCompatActivity!
        final WebSettings webSettings = webStickies.getSettings();
        //WebView.setWebContentsDebuggingEnabled(true); // TODO: turn off before official builds

        if (useDarkTheme) {
            swipeRefresher.setColorSchemeResources(R.color.colorAccent);
            swipeRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorDarkPrimaryLight));
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            swipeRefresher.setColorSchemeResources(R.color.colorAccentDark);
            swipeRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorLightPrimary));
            webStickies.setBackgroundColor(Color.WHITE);
        }

        // set website clients
        webStickies.setWebChromeClient(new ChromeClient());
        webStickies.setWebViewClient(new ViewClient());
        // allow webView to download
        webStickies.setDownloadListener(new DownloadListener() {
            @SuppressWarnings("deprecation")
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                if (file_permission())
                {
                    if (url.startsWith("blob:")) { // encode blob into base64
                        webStickies.loadUrl(blobToBase64);
                        return;
                    } else if (url.startsWith("data:")) { // decode base64 to file
                        File path;
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) // TODO: SAME FIX ME TOO WHEN R COMES OUT
                            path = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        else
                            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
                        String filename = System.currentTimeMillis() + "." + (filetype.equals("jpeg") ? "jpg" : filetype);
                        File file = new File(path, filename);
                        Toast.makeText(getApplicationContext(), "Downloading \"" + filename + "\" ...", Toast.LENGTH_LONG).show();
                        try {
                            if (path != null && !path.exists())
                                path.mkdirs();
                            if(!file.exists())
                                file.createNewFile();

                            String base64EncodedString = url.substring(url.indexOf(",") + 1);
                            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
                            OutputStream os = new FileOutputStream(file);
                            os.write(decodedBytes);
                            os.close();

                            // Tell the media scanner about the new file so that it is immediately available to the user.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                MediaScannerConnection.scanFile(getApplicationContext(),
                                        new String[]{file.toString()}, null,
                                        new MediaScannerConnection.OnScanCompletedListener() {
                                            public void onScanCompleted(String path, Uri uri) {
                                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                                Log.i("ExternalStorage", "-> uri=" + uri);
                                            }
                                        });
                            }

                            Toast.makeText(getApplicationContext(), "Download succeeded!", Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(), "Download failed!", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    DownloadManager.Request request = new DownloadManager.Request(
                            Uri.parse(url));
                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading \"" + filename + "\" ...");
                    request.setTitle(filename);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Download succeeded!", Toast.LENGTH_LONG).show();
                    } else Toast.makeText(getApplicationContext(), "Download failed!", Toast.LENGTH_LONG).show();
                }
            }
        });
        // allows for caching the website when using it offline
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        // enables site to work
        webStickies.clearCache(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webStickies.addJavascriptInterface(new JavaScriptInterface(), "Android");
        // visual fixes
        webStickies.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webStickies.setVerticalScrollBarEnabled(false);
        webStickies.setHorizontalScrollBarEnabled(false);
        // TODO: testing... need to get 'no extension' (dynamic) images to load

        // start the webView
        internetCacheLoad(webStickies, STICKY_NOTES_URL);

        // add swipe to refresh listener
        swipeRefresher.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        disableReloading = true;
                        swipeRefresher.setEnabled(false);
                        swipeRefresher.setRefreshing(false);
                        internetCacheLoad(webStickies, null);
                    }
                }
        );

        // show popup if updated or first time install
        if (updatedToNewVersion) {
            Dialog dialog;
            int dialogBg, linkColor;
            if (useDarkTheme) {
                dialog = new Dialog(this,R.style.PopupDialogDark);
                dialogBg = R.drawable.popup_bg_dark;
                linkColor = R.color.colorAccent;
            } else {
                dialog = new Dialog(this,R.style.PopupDialogLight);
                dialogBg = R.drawable.popup_bg_light;
                linkColor = R.color.colorAccentDark;
            }
            popupDialog = dialog;
            popupDialog.setContentView(R.layout.popup_updated);
            popupDialog.setCanceledOnTouchOutside(false);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(Objects.requireNonNull(popupDialog.getWindow()).getAttributes());
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = (int) Math.round(screenHeight * 0.85);
            popupLayoutParams = layoutParams;
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupDialog.getWindow().setBackgroundDrawableResource(dialogBg);
            popupDialog.setTitle(POPUP_TITLE);
            TextView popupInfo = dialog.findViewById(R.id.popupInfo);
            popupInfo.setMovementMethod(LinkMovementMethod.getInstance());
            popupInfo.setLinkTextColor(ContextCompat.getColor(this, linkColor));
            Button closePopupBtn = dialog.findViewById(R.id.closePopupBtn);

            closePopupBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    popupDialog.dismiss();

                    updatedToNewVersion = false;

                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(PREF_VERSION_USED, APP_VERSION);
                    editor.apply();
                }
            });
        }
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
        if (!disableReloading) {
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
        }

        return true;
    }

    // toggles theme in shared preferences
    private void toggleTheme(String theTheme) {
        disableReloading = true;

        // show theme update info
        String displayToast = theTheme.substring(0, 1).toUpperCase() + theTheme.substring(1).toLowerCase() + " theme enabled";
        Toast.makeText(this, displayToast, Toast.LENGTH_SHORT).show();

        // determine theme by user choice or by system settings
        boolean darkTheme = false;
        if (theTheme.equals("dark") || ((theTheme.equals("system") && isSystemDark()))) {
            darkTheme = true;
            swipeRefresher.setColorSchemeResources(R.color.colorAccent);
            swipeRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorDarkPrimaryLight));
            webLoadingDark.setVisibility(View.VISIBLE);
            webLoadingLight.setVisibility(View.INVISIBLE);
            webStickies.setBackgroundColor(Color.BLACK);
        } else {
            swipeRefresher.setColorSchemeResources(R.color.colorAccentDark);
            swipeRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorLightPrimary));
            webLoadingDark.setVisibility(View.INVISIBLE);
            webLoadingLight.setVisibility(View.VISIBLE);
            webStickies.setBackgroundColor(Color.WHITE);
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(PREF_THEME, theTheme);
        editor.apply();

        if (updatedToNewVersion) popupDialog.dismiss();
        useDarkTheme = darkTheme;
        internetCacheLoad(webStickies, null);
    }

    class JavaScriptInterface {
        // used to enable webView after fully loaded theme
        @JavascriptInterface
        public void webViewSetVisible() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webStickies.setVisibility(View.VISIBLE);
                    // make sure one loading screen is available after first launch
                    if (appLaunched) {
                        if (useDarkTheme) webLoadingDark.setVisibility(View.VISIBLE);
                        else webLoadingLight.setVisibility(View.VISIBLE);
                        appLaunched = false;
                    }
                    if (updatedToNewVersion) {
                        popupDialog.show();
                        Window popupWindow = popupDialog.getWindow();
                        if (popupWindow != null) popupWindow.setAttributes(popupLayoutParams);
                    }
                }
            });
        }
        // soft keyboard open or being at anywhere but the top of the page will disable swipe to refresh
        @JavascriptInterface
        public void setSwipeRefresher(final int scrollTop) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // enable if less than or equal to minimum scrollTop, otherwise, disable
                    if (!keyboardVisible() && (scrollTop <= 0)) {
                        swipeRefresher.setEnabled(true);
                    } else {
                        swipeRefresher.setEnabled(false);
                        swipeRefresher.setRefreshing(false);
                    }
                }
            });
        }
    }
}