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

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public abstract class KeyboardCheckerAppCompatActivity extends AppCompatActivity {
    private static boolean softKeyboardOpen = false;
    public static boolean keepNavBar = false;
    public WebView theWebView;

    public static boolean keyboardVisible() {
        return softKeyboardOpen;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View theView = getWindow().getDecorView();

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
                    if (!softKeyboardOpen) softKeyboardOpen = true;
                } else {
                    // keyboard is closed
                    if (softKeyboardOpen) softKeyboardOpen = false;
                }
            }
        });
    }
}