package aliendrew.ms.stickynotes;

// This Android app aims to make the Microsoft Sticky Notes website work with any Android 5.0+ device.
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

// NOTES:
//
// Android emulators are not updated with the newest version of Android System WebView and,
//   therefore, are not reliable for testing this app below API 24. When not using an Android Studio
//   emulator, at least API 21 (Android Lollipop fully updated) runs the app with no problems.
// Since Android KitKat (APIs 19) doesn't have a separate Android System WebView app that can be
//   updated, the CSS4 code being used on https://onenote.com/stickynotes will not work. More
//   specifically https://caniuse.com/#feat=css-variables&compare=android+4.4.3-4.4.4
// All deprecations that you might see in the build log are all taken care of, and shouldn't
//   interfere with the newer versions of Android that don't support them. (up to API 29)

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AppCompatActivity;
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
import android.text.TextUtils;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends KeyboardCheckerAppCompatActivity {
    // constants
    private static final String STICKY_NOTES_URL = "https://www.onenote.com/stickynotes";
    private static final String APP_VERSION = BuildConfig.VERSION_NAME;
    private static final String APP_NAME = "Unofficial Sticky Notes";
    private static final String APP_DIRECT_DOWNLOAD = "https://raw.githubusercontent.com/TheAlienDrew/Unofficial-Sticky-Notes/master/release/app-release.apk";
    private static final String APP_DIRECT_INFO = "https://raw.githubusercontent.com/TheAlienDrew/Unofficial-Sticky-Notes/master/release/output-metadata.json";
    private static final String POPUP_TITLE = APP_NAME + " v" + APP_VERSION;
    private static final String SAVE_DIRECTORY = Environment.DIRECTORY_PICTURES + File.separator + APP_NAME;
    // user locale
    private final Locale USER_LOCALE = Locale.getDefault();
    private final String USER_LANGUAGE = USER_LOCALE.getLanguage();
    private final String USER_COUNTRY = USER_LOCALE.getCountry();
    private final String URL_LOCALE = (TextUtils.isEmpty(USER_LANGUAGE) ? "en" : USER_LANGUAGE) + '-' + (TextUtils.isEmpty(USER_COUNTRY) ? "US" : USER_COUNTRY);
    // help page related
    private static final String STICKY_HELP_URL_START = "https://support.office.com/client/results";
    //                          STICKY_HELP_URL_START: normally this URL is for any MS app, but in our case, it'll only lead to the Sticky Notes help page
    private static final String STICKY_HELP_URL_P1 = STICKY_HELP_URL_START + "?NS=stickynotes&Context=%7B%22ThemeId%22:";
    private static final String DARK_STICKY_HELP_THEME_ID = "4";
    private static final String LIGHT_STICKY_HELP_THEME_ID = "6";
    private static final String STICKY_HELP_URL_P2 = ",%22LinkColor%22:%22";
    private static final String DARK_STICKY_HELP_LINK_COLOR = "BCD6E6";
    private static final String LIGHT_STICKY_HELP_LINK_COLOR = "106EBE";
    private static final String STICKY_HELP_URL_P3 = "%22,%22IsWebShell%22:true,%22Domain%22:%22www.onenote.com%22%7D&Locale=";
    private static final String STICKY_HELP_URL_P4 = "&ShowNav=true&Version=16&omkt=";
    private static final String STICKY_HELP_URL_P5 = "&origin=https://www.onenote.com&feedback=0&moveSupportCard=0";
    // simplified
    private final String BOTH_STICKY_HELP_URL_END = URL_LOCALE + STICKY_HELP_URL_P4 + URL_LOCALE + STICKY_HELP_URL_P5;
    private final String DARK_STICKY_HELP_URL = STICKY_HELP_URL_P1 + DARK_STICKY_HELP_THEME_ID + STICKY_HELP_URL_P2 + DARK_STICKY_HELP_LINK_COLOR + STICKY_HELP_URL_P3 + BOTH_STICKY_HELP_URL_END;
    private final String LIGHT_STICKY_HELP_URL = STICKY_HELP_URL_P1 + LIGHT_STICKY_HELP_THEME_ID + STICKY_HELP_URL_P2 + LIGHT_STICKY_HELP_LINK_COLOR + STICKY_HELP_URL_P3 + BOTH_STICKY_HELP_URL_END;

    // general app controls
    private static final String PREFS_NAME = "prefs";
    private ImageView splashImage;
    private SwipeRefreshLayout swipeRefresher;
    private boolean appLaunched = true;
    private boolean cacheErrorSent = false;
    private boolean disableReloading = true;
    private boolean singleBack = false;
    private boolean closeButtonActive = false;
    private String closeButtonSelector = null;

    // file upload initialize
    private static final String TAG = MainActivity.class.getSimpleName();
    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location
    private static final int file_req_code = 1;
    // file blob converting javascript
    private static final String blobToBase64 = "javascript: (function() {var reader=new window.FileReader;const convertBlob=function(e){if(null!=e&&null!=e.href&&e.href.startsWith(\"blob\")){var r=e.href;fetch(r).then(e=>e.blob()).then(r=>{reader.readAsDataURL(r),reader.onload=function(r){var t=r.target.result;e.href=t,e.click()}})}};convertBlob(document.querySelector('.n-lightbox-download'));})()";

    // widget based
    private static final int widget_req_code = 2;

    // use the chosen theme
    private static final String PREF_THEME = "theme";
    private boolean useSystemTheme = false;
    private boolean useDarkTheme = false;

    // turn off tooltips
    private static final String PREF_TOOLTIPS = "tooltips";
    private boolean disableToolTips = false;

    // for first time use
    private Dialog popupDialog;
    private WindowManager.LayoutParams popupLayoutParams;
    private static final int APP_UPDATE_REQUEST_CODE = 180;
    private static final String PREF_VERSION_USED = "version_used";
    private boolean updatedToNewVersion = false;
    private AppUpdateManager appUpdateManager;

    // webView variables
    private WebView webLoadingDark;
    private WebView webLoadingLight;
    private WebView webStickies;

    // javascript interface (variable map)
    private static ImmutableMap<String, Integer> STRING_INTEGERS;

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

    // functions for permissions
    public boolean file_permission(){
        // TODO: Build.VERSION.SDK_INT <= 29: might need to be added to prepare for API 30 support
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        } else {
            return true;
        }
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

    // functions for camera photo and file upload
    // code via https://github.com/mgks/Os-FileUp
    private File create_image() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            // TODO: this is only a temp fix for when Android R comes out
            storageDir = getApplicationContext().getExternalFilesDir(SAVE_DIRECTORY);
        else
            // NOTE: getExternalStoragePublicDirectory only works on Android Q because I've enabled
            //       requestLegacyExternalStorage in the AndroidManifest, otherwise it's deprecated
            storageDir = Environment.getExternalStoragePublicDirectory(SAVE_DIRECTORY);

        // need to create the directory if not already there
        if (storageDir != null && !storageDir.exists()) //noinspection ResultOfMethodCallIgnored
            storageDir.mkdir();

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
        if (results != null) {
            file_path.onReceiveValue(results);
            file_path = null;
        }

        // if we are doing an update, show the following
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this,
                        getString(R.string.updateFailed),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // check if app is installed from Google Play Store
    // code via https://stackoverflow.com/a/37540163/7312536
    boolean verifyInstallerId(Context context) {
        // A list with valid installers package name
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        // The package name of the app that has installed your app
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        // true if your app has been downloaded from Play Store
        return (installer != null && validInstallers.contains(installer));
    }

    // in-app update functions
    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.relativeLayout),
                        getString(R.string.downloadedUpdate),
                        Snackbar.LENGTH_INDEFINITE);
        View snackBarView = snackbar.getView();

        snackbar.setAction(getString(R.string.restartBtn), view -> {
            if (appUpdateManager != null) { appUpdateManager.completeUpdate(); }
        });

        if (useDarkTheme) {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDarkPrimary));
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        } else {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorLightPrimary));
            snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorAccentDark));
        }

        snackbar.show();
    }
    InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(InstallState state) {
            if (state.installStatus() == InstallStatus.DOWNLOADED){
                popupSnackbarForCompleteUpdate();
            } else if (state.installStatus() == InstallStatus.INSTALLED) {
                if (appUpdateManager != null) { appUpdateManager.unregisterListener(installStateUpdatedListener); }
            } else {
                Log.i(TAG, "InstallStateUpdatedListener: state: " + state.installStatus());
            }
        }
    };

    // altered clients to work with sticky notes
    public class ChromeClient extends WebChromeClient {
        // enable file choosing
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (file_permission()) {
                file_path = filePathCallback;

                // TODO: DEBUG THIS SOMETIME TO FIX BLANK 0-BYTE IMAGES ISSUE
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
                chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.fileChooser));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, file_req_code);

                return true;
            } else return false;
        }

        // allow JS alert dialogs
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // modified to not show website url
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this);
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

            // is page history back is possible, exit from here
            if (failingUrl.startsWith(STICKY_HELP_URL_START)) {
                // if we are on the help page, send a toast that the user needs to view that page
                //   online first for it to work offline
                Toast.makeText(MainActivity.this,
                        getString(R.string.helpNotCached),
                        Toast.LENGTH_LONG).show();

                if (view.canGoBack()) view.goBack();
                else internetCacheLoad(view, STICKY_NOTES_URL);
                return;
            }

            final AlertDialog alertDialog = createAlertDialog(MainActivity.this);
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
                    alertDialogThemeButtons(MainActivity.this, alertDialog);
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
            return url.startsWith(STICKY_NOTES_URL) || url.startsWith(STICKY_HELP_URL_START)
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
            if (url.startsWith(STICKY_HELP_URL_START)) {
                // fix scaling and themes (Microsoft didn't prepare a dark mode)
                swipeRefresher.setEnabled(false);
                swipeRefresher.setRefreshing(false);
                injectScriptFile(view, "js/convert_helpPage.js");
            } else if (url.startsWith(STICKY_NOTES_URL)) {
                // disable tooltips if setting is active
                if (disableToolTips) injectScriptFile(view, "js/disable_tooltips.js");
                // make the main sticky notes website compatible with Android webView
                injectScriptFile(view, "js/convert_stickyNotesMain.js");
                // swipe is handled in the convert_stickyNotesMain.js
            } else {
                // make logins and other pages compatible with Android webView
                swipeRefresher.setEnabled(false);
                swipeRefresher.setRefreshing(false);
                injectScriptFile(view, "js/convert_loginsOther.js");
            }

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
            if (view.getUrl().startsWith(STICKY_HELP_URL_START)) {
                if (useDarkTheme) url = DARK_STICKY_HELP_URL;
                else url = LIGHT_STICKY_HELP_URL;
                view.loadUrl(url);
            } else {
                view.reload();
            }
        }
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
        boolean prefToolTips = preferences.getBoolean(PREF_TOOLTIPS, true);
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
        disableToolTips = !prefToolTips;
        // set string to integers map
        STRING_INTEGERS = ImmutableMap.of(
                "options", R.string.options,
                "switchTheme", R.string.switchTheme,
                "toggleToolTips", R.string.toggleToolTips,
                "helpNotCached", R.string.helpNotCached,
                "loginNotCached", R.string.loginNotCached
        );

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
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                if (file_permission()) {
                    if (url.startsWith("blob:")) { // encode blob into base64
                        webStickies.evaluateJavascript(blobToBase64, null);
                        return;
                    }

                    if (url.startsWith("data:")) { // decode base64 to file
                        File path;
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) // TODO: SAME FIX ME TOO WHEN R COMES OUT
                            path = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        else
                            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
                        String customName = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
                        customName = (filetype.equals("apk") && !customName.equals("application")) ? customName : String.valueOf(System.currentTimeMillis());
                        String filename = customName + "." + (filetype.equals("jpeg") ? "jpg" : filetype);
                        File file = new File(path, filename);
                        Toast.makeText(getApplicationContext(), getString(R.string.downloading) + " \"" + filename + "\" ...", Toast.LENGTH_LONG).show();
                        try {
                            if (path != null && !path.exists()) //noinspection ResultOfMethodCallIgnored
                                path.mkdirs();
                            if (!file.exists()) //noinspection ResultOfMethodCallIgnored
                                file.createNewFile();

                            String base64EncodedString = url.substring(url.indexOf(",") + 1);
                            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
                            OutputStream os = new FileOutputStream(file);
                            os.write(decodedBytes);
                            os.close();

                            // Tell the media scanner about the new file so that it is immediately available to the user.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                //noinspection Convert2Lambda
                                MediaScannerConnection.scanFile(getApplicationContext(),
                                        new String[]{file.toString()}, null,
                                        new MediaScannerConnection.OnScanCompletedListener() {
                                            public void onScanCompleted(String path, Uri uri) {
                                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                                Log.i("ExternalStorage", "-> uri=" + uri);
                                            }
                                        });
                            }

                            Toast.makeText(getApplicationContext(), getString(R.string.downloadSucceeded), Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.downloadFailed), Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    String downloading = getString(R.string.downloading) + " \"" + filename + "\" ...";
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setMimeType(mimeType);
                    request.setTitle(filename);
                    request.setDescription(downloading);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) request.allowScanningByMediaScanner();
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(true);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), downloading, Toast.LENGTH_SHORT).show();
                        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                    } else Toast.makeText(getApplicationContext(), getString(R.string.downloadFailed), Toast.LENGTH_LONG).show();
                }
            }
            final BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(getApplicationContext(), getString(R.string.downloadSucceeded), Toast.LENGTH_SHORT).show();
                }
            };
        });
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
        webStickies.addJavascriptInterface(new StickiesJS(), "Android");
        // visual fixes
        webStickies.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webStickies.setVerticalScrollBarEnabled(false);
        webStickies.setHorizontalScrollBarEnabled(false);
        webSettings.setTextZoom(100);

        // start the webView
        internetCacheLoad(webStickies, STICKY_NOTES_URL);

        // add swipe to refresh listener
        //noinspection Convert2Lambda
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

            //noinspection Convert2Lambda
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

    // what to do when the app starts
    @Override
    protected void onStart() {
        super.onStart();
        // check for updates
        appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            // check update exists
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // do alternate prompt for non play store users
                if (verifyInstallerId(this)) {
                    // start type of update if needed
                    try {
                        int installType; // where -1 will be neither
                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            installType = AppUpdateType.FLEXIBLE;
                        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            installType = AppUpdateType.IMMEDIATE;
                        } else {
                            installType = -1;
                        }

                        // do update if one is real
                        if (installType >= 0) {
                            if (installType == AppUpdateType.FLEXIBLE) {
                                appUpdateManager.registerListener(installStateUpdatedListener);
                            }

                            appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                                    installType, this, APP_UPDATE_REQUEST_CODE);
                        }
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                } else {
                    // show a prompt for other app stores
                    final PackageManager pm = getApplicationContext().getPackageManager();
                    final String installerPackageName = pm.getInstallerPackageName(getPackageName());
                    ApplicationInfo ai;
                    try {
                        // making sure to display correct prompt if user installed the app manually.
                        if (installerPackageName.equals("com.google.android.packageinstaller")) {
                            ai = null;
                        } else {
                            ai = pm.getApplicationInfo(installerPackageName, 0);
                        }
                    } catch (final PackageManager.NameNotFoundException e) {
                        ai = null;
                    }
                    final String otherInstaller = (String) (ai != null ? pm.getApplicationLabel(ai) : getString(R.string.installerUnknown));

                    final AlertDialog otherInstallerDialog = createAlertDialog(MainActivity.this);
                    // doing this will allow for easier change to language strings
                    otherInstallerDialog.setTitle(getString(R.string.updateTitle));
                    otherInstallerDialog.setMessage(getString(R.string.updatePrompt).replace(getString(R.string.updateInstallerVariable), otherInstaller));
                    //noinspection Convert2Lambda
                    otherInstallerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.downloadApkBtn), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            webStickies.evaluateJavascript("javascript:(function() {" +
                                    "var versionName, xhr = new XMLHttpRequest();" +
                                    "xhr.open('GET', '" + APP_DIRECT_INFO + "', true);" +
                                    "xhr.responseType = 'json';" +
                                    "xhr.onload = function() {" +
                                        "var status = xhr.status;" +
                                        "if (status === 200) { versionName = xhr.response.elements[0].versionName; }" +
                                        "else { var dateObj = new Date(), dd = dateObj.getUTCDate(), mm = dateObj.getUTCMonth() + 1, yyyy = dateObj.getUTCFullYear(); versionName = dd+'-'+mm+'-'+yyyy;" +
                                    "}}; xhr.send();" +
                                    "var waitForVersion = setInterval(function() {" +
                                        "if (versionName != null) {" +
                                            "clearInterval(waitForVersion);" +
                                            "var reader = new FileReader();" +
                                            "var xhr = new XMLHttpRequest(), link = document.createElement('a'), file;" +
                                            "xhr.open('GET', '" + APP_DIRECT_DOWNLOAD + "', true);" +
                                            "xhr.responseType = 'blob';" +
                                            "xhr.onload = function () {" +
                                            "file = new Blob([xhr.response], { type : '" + getApplicationContext().getPackageName() + "_' + versionName + '/apk' });" +
                                            "reader.readAsDataURL(file);" +
                                            "reader.onloadend = function() {" +
                                            "var base64data = reader.result;" +
                                            "link.download = '';" +
                                            "link.href = base64data;" +
                                            "link.click();" +
                                            "}" +
                                            "};" +
                                            "xhr.send();" +
                                    "}}, 100);})()", null);
                        }
                    });
                    //noinspection Convert2Lambda
                    otherInstallerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.updateBtn), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // show available app stores that the app may be downloaded from
                            Uri marketUrl = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUrl);

                            PackageManager packageManager = getPackageManager();
                            List<ResolveInfo> activities = packageManager.queryIntentActivities(marketIntent, 0);
                            if (activities.size() > 0) {
                                startActivity(marketIntent);
                            }
                        }
                    });
                    //noinspection Convert2Lambda
                    otherInstallerDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dismissBtn), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            otherInstallerDialog.dismiss();
                        }
                    });
                    //noinspection Convert2Lambda
                    otherInstallerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface arg0) {
                            alertDialogThemeButtons(MainActivity.this, otherInstallerDialog);
                        }
                    });
                    otherInstallerDialog.show();
                }
            }
        });
    }

    // what to do after the app resumes
    @Override
    protected void onResume() {
        super.onResume();
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            // make sure it was originally installed from play store to do this kind of update
            if (verifyInstallerId(this)) {
                // if update download, and not installed, ask user to restart
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate();
                }

                // otherwise, check for an immediate update
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // if in-app update is already running, resume it
                        appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                                AppUpdateType.IMMEDIATE, this, APP_UPDATE_REQUEST_CODE);
                    }
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // what to do after the app stops
    @Override
    protected void onStop() {
        super.onStop();
        // stop the update listener, if it was ever started
        if (verifyInstallerId(this) && appUpdateManager != null) {
            appUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }

    // make back button exit app
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // different actions for other pages
            URL currentUrl;
            try {
                currentUrl = new URL(webStickies.getUrl());
                String currentHost = currentUrl.getHost();
                String topUrl = currentHost.substring(0, currentHost.indexOf('.'));
                // if the page is at the login screen, allow go back
                // if it's on the help page, go back to notes
                if ((topUrl.equals("login")) && webStickies.canGoBack()) {
                    webStickies.goBack();
                    return true;
                } else if (currentUrl.toString().startsWith(STICKY_HELP_URL_START)) {
                    internetCacheLoad(webStickies, STICKY_NOTES_URL);
                    return true;
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            // if there is an active close button in sticky notes, click it
            if (closeButtonActive) {
                // fix immersive mode back to normal after the last image based close button is clicked
                if (closeButtonSelector.equals(".n-lightbox-close")) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                }

                webStickies.evaluateJavascript("javascript: document.querySelector('"+closeButtonSelector+"').click()", null);
                return true;
            }

            // must press back twice within 2 seconds to exit app
            if (singleBack) {
                super.onBackPressed();
                return true;
            }
            singleBack = true;
            Toast.makeText(this, getString(R.string.pressBackToExit), Toast.LENGTH_SHORT).show();
            //noinspection Convert2Lambda
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    singleBack = false;
                }
            }, 2000);
        }

        return true;
    }
    // show the widget option to user
    private void pickNoteWidget() {
        // TODO: get me working, needs to shortcut to creating widget
        //Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        //pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ...);
        //startActivityForResult(pickIntent, widget_req_code);
    }
    // shift through theme settings
    /* No longer using this in favor for the options menu buttons
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
    } */
    private void toggleThemePrompt() {
        final AlertDialog toggleThemeDialog = createAlertDialog(MainActivity.this);
        toggleThemeDialog.setTitle(getString(R.string.themesTitle));
        toggleThemeDialog.setMessage(getString(R.string.themesMessage));

        String negativeText;
        String negativeMode;
        String positiveText;
        String positiveMode;

        if (useSystemTheme) {
            negativeText = getString(R.string.themeLight);
            negativeMode = "light";
            positiveText = getString(R.string.themeDark);
            positiveMode = "dark";
        } else {
            negativeText = useDarkTheme ? getString(R.string.themeLight) : getString(R.string.themeDark);
            negativeMode = useDarkTheme ? "light" : "dark";
            positiveText = getString(R.string.themeSystem);
            positiveMode = "system";
        }
        String finalNegativeMode = negativeMode;
        String finalPositiveMode = positiveMode;

        //noinspection Convert2Lambda
        toggleThemeDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                useSystemTheme = false;
                toggleTheme(finalNegativeMode);
            }
        });
        //noinspection Convert2Lambda
        toggleThemeDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                useSystemTheme = true;
                toggleTheme(finalPositiveMode);
            }
        });
        //noinspection Convert2Lambda
        toggleThemeDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alertDialogThemeButtons(MainActivity.this, toggleThemeDialog);
            }
        });

        toggleThemeDialog.show();
    }

    // toggles theme in shared preferences
    private void toggleTheme(String theTheme) {
        if (!disableReloading) {
            disableReloading = true;

            // show theme update info
            String getTranslatableTheme = null;
            switch (theTheme) {
                case "light":
                    getTranslatableTheme = getString(R.string.themeLight);
                    break;
                case "dark":
                    getTranslatableTheme = getString(R.string.themeDark);
                    break;
                case "system":
                    getTranslatableTheme = getString(R.string.themeSystem);
                    break;
            }
            String displayToast = getTranslatableTheme + ' ' + getString(R.string.themeEnabled);
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
    }

    // toggles tooltips in shared preferences
    private void toggleToolTips() {
        if (!disableReloading) {
            disableReloading = true;

            // show theme update info
            String displayToast = getString(R.string.toolTips) + ' ' + (disableToolTips ? getString(R.string.enabled) : getString(R.string.disabled));
            Toast.makeText(this, displayToast, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(PREF_TOOLTIPS, disableToolTips);
            editor.apply();

            disableToolTips = !disableToolTips;
            internetCacheLoad(webStickies, null);
        }
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
                        if (updatedToNewVersion) {
                            popupDialog.show();
                            Window popupWindow = popupDialog.getWindow();
                            if (popupWindow != null) popupWindow.setAttributes(popupLayoutParams);
                        }
                    } else webStickies.setVisibility(View.INVISIBLE);
                }
            });
        }

        // soft keyboard open or being at anywhere but the top of the page will disable swipe to refresh
        @JavascriptInterface
        public void setSwipeRefresher(final int scrollTop, final boolean editActive) {
            //noinspection Convert2Lambda
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // enable if phone edit is inactive and less than or equal to minimum scrollTop, otherwise, disable
                    if (!keyboardVisible() && !editActive && (scrollTop <= 0)) {
                        swipeRefresher.setEnabled(true);
                    } else {
                        swipeRefresher.setEnabled(false);
                        swipeRefresher.setRefreshing(false);
                    }
                }
            });
        }

        // it will show the user the option to use the note widget from the app
        @JavascriptInterface
        public void promptPickNoteWidget() {
            runOnUiThread(MainActivity.this::pickNoteWidget);
        }

        // switch theme prompt when clicking on the button from the menu
        @JavascriptInterface
        public void promptSwitchTheme() {
            runOnUiThread(MainActivity.this::toggleThemePrompt);
        }

        // toggle tooltips on or off when clicking on the button from the menu
        @JavascriptInterface
        public void promptToggleToolTips() {
            runOnUiThread(MainActivity.this::toggleToolTips);
        }

        // updates variable that knows if there is an available close button on screen
        @JavascriptInterface
        public void setCloseAvailable(final boolean availability, final String elementToClick) {
            closeButtonActive = availability;
            closeButtonSelector = elementToClick;
            //noinspection Convert2Lambda
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (elementToClick.equals(".n-lightbox-close")) getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                }
            });
        }

        // loads the specific theme url
        @JavascriptInterface
        public String getHelpUrl() {
            if (useDarkTheme) return DARK_STICKY_HELP_URL;
            else return LIGHT_STICKY_HELP_URL;
        }

        // load help in fullscreen
        @JavascriptInterface
        public void loadStickiesHelp() {
            //noinspection Convert2Lambda
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webStickies.setVisibility(View.INVISIBLE);
                    String helpURL = useDarkTheme ? DARK_STICKY_HELP_URL : LIGHT_STICKY_HELP_URL;
                    internetCacheLoad(webStickies, helpURL);
                }
            });
        }

        // returns Android strings to webView
        @JavascriptInterface
        // can't be null anyways
        public String getAndroidString(String stringVariable) {
            return getResources().getString(STRING_INTEGERS.get(stringVariable));
        }
    }
}