package aliendrew.ms.stickynotes;

/* This Android app aims to make the Microsoft Sticky Notes website work with any Android 5.0+ device.
 * Copyright (C) 2020  Andrew Larson (thealiendrew@gmail.com)
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
//   interfere with the newer versions of Android that don't support them.

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
import android.content.IntentSender;
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

public class MainActivity extends ImmersiveAppCompatActivity {
    // constants
    private static final String STICKY_NOTES_URL = "https://www.onenote.com/stickynotes";
    private static final String STICKY_HELP_URL_START = "https://support.office.com/client/results";
    //                          STICKY_HELP_URL_START: normally this host + path is for any MS app, but in our case, it'll only lead to the Sticky Notes help page
    private static final String APP_VERSION = BuildConfig.VERSION_NAME;
    private static final String APP_NAME = "Unofficial Sticky Notes";
    // TODO: the following commented out old code for text bug prompt (strings), might be useful to repurpose as a "rating me" popup
    //private static final String DEV_EMAIL = "thealiendrew@gmail.com";
    //private static final String FEEDBACK_EMAIL_HEADER = APP_NAME+" ("+APP_VERSION+") User Feedback | Device: "+Build.MANUFACTURER+' '+Build.DEVICE+" ("+Build.MODEL+") API: "+Build.VERSION.SDK_INT;
    private static final String POPUP_TITLE = APP_NAME + " v" + APP_VERSION;
    private static final String SAVE_DIRECTORY = Environment.DIRECTORY_PICTURES + File.separator + APP_NAME;
    // user locale
    private final Locale USER_LOCALE = Locale.getDefault();
    private final String USER_LANGUAGE = USER_LOCALE.getLanguage();
    private final String USER_COUNTRY = USER_LOCALE.getCountry();
    private final String URL_LOCALE = (TextUtils.isEmpty(USER_LANGUAGE) ? "en" : USER_LANGUAGE) + '-' + (TextUtils.isEmpty(USER_COUNTRY) ? "US" : USER_COUNTRY);
    // help page related
    private static final String STICKY_HELP_URL_P1 = "https://support.office.com/client/results?NS=stickynotes&Context=%7B%22ThemeId%22:";
    private static final String DARK_STICKY_HELP_THEME_ID = "4";
    private static final String LIGHT_STICKY_HELP_THEME_ID = "6";
    private static final String STICKY_HELP_URL_P2 = ",%22LinkColor%22:%22";
    private static final String DARK_STICKY_HELP_LINK_COLOR = "B3D6FC";
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

    // use the chosen theme
    private static final String PREF_THEME = "theme";
    private boolean useSystemTheme = false;
    private boolean useDarkTheme = false;

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
    private NoTextCorrectionsWebView webStickies;

    // TODO: old code for text bug prompt (theming), might be useful to repurpose as a "rating me" popup
    /*LinearLayout tempLinear;
    TextView tempText;
    Button upvotePlea;
    Button sendFeedbackBtn;
    Button dismissMessageBtn;
    private void setTempTheme() {
        if (useDarkTheme) {
            tempLinear.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorDarkPrimary));
            MyDrawableCompat.setColorFilter(upvotePlea.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorDarkPrimaryLight));
            MyDrawableCompat.setColorFilter(sendFeedbackBtn.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorDarkPrimaryLight));
            MyDrawableCompat.setColorFilter(dismissMessageBtn.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorDarkPrimaryLight));
            tempText.setTextColor(Color.WHITE);
            upvotePlea.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
            sendFeedbackBtn.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
            dismissMessageBtn.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
            tempText.setText(getResources().getText(R.string.tempStringDark));
        } else {
            tempLinear.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorLightPrimary));
            MyDrawableCompat.setColorFilter(upvotePlea.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorLightPrimaryDark));
            MyDrawableCompat.setColorFilter(sendFeedbackBtn.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorLightPrimaryDark));
            MyDrawableCompat.setColorFilter(dismissMessageBtn.getBackground(), ContextCompat.getColor(MainActivity.this, R.color.colorLightPrimaryDark));
            tempText.setTextColor(Color.BLACK);
            upvotePlea.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccentDark));
            sendFeedbackBtn.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccentDark));
            dismissMessageBtn.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccentDark));
            tempText.setText(getResources().getText(R.string.tempStringLight));
        }
    }*/

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

    // TODO: old code for text bug prompt (feedback sender), might be useful to repurpose as a "rating me" popup
    // functions to allow user to send feedback
    /*private void sendFeedback() {
        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_EMAIL_HEADER);
        email.setData(Uri.parse("mailto:"+DEV_EMAIL));
        email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(email, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }*/

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
            // TODO: getExternalStoragePublicDirectory only works on Android Q because I've enabled
            //  requestLegacyExternalStorage in the AndroidManifest, otherwise it's deprecated
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
        // if we are doing an update, show the following
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this,
                        "App Update failed, please try again on the next app launch.",
                        Toast.LENGTH_SHORT).show();
            }
        }

        if (results != null) {
            file_path.onReceiveValue(results);
            file_path = null;
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
        return installer != null && validInstallers.contains(installer);
    }

    // in-app update functions
    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.relativeLayout),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE);
        View snackBarView = snackbar.getView();

        snackbar.setAction("RESTART", view -> {
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
            // modified to not show website url
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this);
            builder.setMessage(message)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
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
            if (view.canGoBack()) {
                // if we are on the help page, send a toast that the user needs to view that page
                //   online first for it to work offline
                if (failingUrl.startsWith(STICKY_HELP_URL_START))
                    Toast.makeText(MainActivity.this,
                            "You must visit the Help website once online, before you can use it offline.",
                            Toast.LENGTH_LONG).show();

                view.goBack();
                return;
            }

            // couldn't go back so, there is no page, must hide webView
            view.setVisibility(View.INVISIBLE);

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

            // Don't load themes/don't allow swipe on the help page, as it's already themed/doesn't need reload
            if (url.startsWith(STICKY_HELP_URL_START)) {
                // fix scaling/note boxes backgrounds (Microsoft theming errors)
                injectScriptFile(view, "js/help_fixes.js");
                // swipe to refresh is disabled so users can scroll the help page
                swipeRefresher.setEnabled(false);
                swipeRefresher.setRefreshing(false);
                view.setVisibility(View.VISIBLE);
            } else {
                // light theme is the Microsoft default
                if (useDarkTheme) injectScriptFile(view, "js/dark_theme.js");

                // make the website compatible with Android webView
                injectScriptFile(view, "js/webView_convert.js");

                // visibility + swipe is handled in webView_convert.js for Sticky Notes and login
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
            keepNavBar = url.startsWith(STICKY_HELP_URL_START);

            setToImmersiveMode(true);

            if (url.length() > 0) view.loadUrl(url);
        } else {
            if (view.getUrl().startsWith(STICKY_HELP_URL_START)) {
                keepNavBar = true;
                if (useDarkTheme) url = DARK_STICKY_HELP_URL;
                else url = LIGHT_STICKY_HELP_URL;
                view.loadUrl(url);
            } else {
                keepNavBar = false;
                view.reload();
            }
        }
    }

    // TODO: old code for text bug prompt (dismissal), might be useful to repurpose as a "rating me" popup
    /*public void dismissTempMsg(View v) {
        LinearLayout tempLinearContainer = findViewById(R.id.tempLinearContainer);
        LinearLayout tempLinear = findViewById(R.id.tempLinear);
        TextView tempText = findViewById(R.id.tempText);
        LinearLayout tempButtons = findViewById(R.id.tempButtons);
        LayoutTransition dismissTransition = new LayoutTransition();

        dismissTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        tempLinearContainer.setLayoutTransition(dismissTransition);
        tempLinear.setLayoutTransition(dismissTransition);

        tempText.setVisibility(TextView.GONE);
        tempButtons.setVisibility(LinearLayout.GONE);

        tempLinearContainer.removeViewAt(0);
    }
    public void sendFeedbackClick(View v) {
        sendFeedback();
    }
    public void upvotePlea(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://onenote.uservoice.com/forums/909886-sticky-notes/suggestions/40272370-sticky-notes-website-android-text-input-broken"));
        startActivity(intent);
    }*/

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

        // TODO: old code for text bug prompt (initializing), might be useful to repurpose as a "rating me" popup
        /*tempLinear = findViewById(R.id.tempLinear);
        tempText = findViewById(R.id.tempText);
        tempText.setMovementMethod(LinkMovementMethod.getInstance());
        upvotePlea = findViewById(R.id.upvotePlea);
        sendFeedbackBtn = findViewById(R.id.sendFeedbackBtn);
        dismissMessageBtn = findViewById(R.id.dismissMessageBtn);
        setTempTheme();*/

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
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                if (file_permission())
                {
                    if (url.startsWith("blob:")) { // encode blob into base64
                        webStickies.evaluateJavascript(blobToBase64, null);
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
                    // show a prompt for other app stores // TODO: Maybe use remnants of text bug prompt for this instead
                    String otherInstaller = getPackageManager().getInstallerPackageName(getPackageName());

                    final AlertDialog otherInstallerDialog = createAlertDialog(MainActivity.this);
                    otherInstallerDialog.setTitle("New update available!");
                    otherInstallerDialog.setMessage("Please visit \"" + otherInstaller
                            + "\" in order to update to the newest version of Unofficial Sticky Notes. Or alternatively, go to the Google Play Store and install the update.");
                    otherInstallerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            otherInstallerDialog.dismiss();
                        }
                    });
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
                // fix immersive mode back to normal
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
        // setTempTheme(); // TODO: old code for text bug prompt (sets theme), might be useful to repurpose as a "rating me" popup
        internetCacheLoad(webStickies, null);
    }

    // TODO: old code for text bug prompt (relates to background of prompt somehow), might be useful to repurpose as a "rating me" popup
    /*public static class MyDrawableCompat {
        public static void setColorFilter(@NonNull Drawable drawable, int color) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drawable.setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_ATOP));
            } else {
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }*/

    class StickiesJS {
        // check if it's dark theme for javascript
        @JavascriptInterface
        public boolean isDarkMode() {
            return useDarkTheme;
        }

        // used to enable webView after fully loaded theme
        @JavascriptInterface
        public void webViewSetVisible() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webStickies.setVisibility(View.VISIBLE);
                    // make sure one loading screen is available after first launch
                    if (appLaunched) {
                        // TODO: old code for text bug prompt (sets visibility), might be useful to repurpose as a "rating me" popup
                        // tempLinear.setVisibility(View.VISIBLE);

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

        // updates variable that knows if there is an available close button on screen
        @JavascriptInterface
        public void setCloseAvailable(final boolean availability, final String elementToClick) {
            closeButtonActive = availability;
            closeButtonSelector = elementToClick;
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
            runOnUiThread(() -> {
                webStickies.setVisibility(View.INVISIBLE);
                internetCacheLoad(webStickies, (useDarkTheme ? DARK_STICKY_HELP_URL : LIGHT_STICKY_HELP_URL));
            });
        }
    }
}