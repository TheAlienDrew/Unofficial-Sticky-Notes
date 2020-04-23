package aliendrew.ms.stickynotes;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.lang.ref.WeakReference;

public abstract class ImmersiveAppCompatActivity extends AppCompatActivity {
    private HideHandler mHideHandler;

    private static boolean softKeyboardOpen = false;
    public SwipeRefreshLayout theSwipeRefresher;
    public NoTextCorrectionsWebView theWebView;

    public static boolean keyboardVisible() {
        return softKeyboardOpen;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create a handler to set immersive mode on a delay
        mHideHandler = new HideHandler(this);

        final View theView = getWindow().getDecorView();

        // change to immersive mode after ui change
        theView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            setToImmersiveMode(!softKeyboardOpen);
                    }
            });

        // ContentView is the root view of the layout of this activity/fragment
        theView.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                theView.getWindowVisibleDisplayFrame(r);
                int screenHeight = theView.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    if (!softKeyboardOpen) {
                        softKeyboardOpen = true;
                        setToImmersiveMode(false);
                        // disable refreshing
                        if (theSwipeRefresher != null) {
                            theSwipeRefresher.setEnabled(false);
                            theSwipeRefresher.setRefreshing(false);
                        }
                    }
                } else {
                    // keyboard is closed
                    if (softKeyboardOpen) {
                        softKeyboardOpen = false;
                        setToImmersiveMode(true);
                        // conditionally enable swipe to refresh
                        if (theSwipeRefresher != null && theWebView != null) {
                            theWebView.loadUrl("javascript: window.Android.setSwipeRefresher(document.querySelector('.n-noteList-Container').scrollTop)");
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setToImmersiveMode(!softKeyboardOpen);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            mHideHandler.removeMessages(0);
            mHideHandler.sendEmptyMessageDelayed(0, 300);
        }
        else mHideHandler.removeMessages(0);
    }

    public void setToImmersiveMode(boolean choice) {
        // set to immersive but also allow resizing of window when keyboard is out
        if (choice) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private static class HideHandler extends Handler {
        private final WeakReference<ImmersiveAppCompatActivity> mActivity;

        HideHandler(ImmersiveAppCompatActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@SuppressWarnings("NullableProblems") Message msg) {
            ImmersiveAppCompatActivity activity = mActivity.get();
            if(activity != null) activity.setToImmersiveMode(!softKeyboardOpen);
        }
    }
}