package aliendrew.ms.stickynotes;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class DraftJSTempFixWebView extends WebView {
    public DraftJSTempFixWebView(Context context) {
        super(context);
    }

    public DraftJSTempFixWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DraftJSTempFixWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);

        outAttrs.inputType &= ~EditorInfo.TYPE_MASK_VARIATION; /* clear VARIATION type to be able to set new value */
        outAttrs.inputType |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD; /* VISIBLE_PASSWORD type prevents dynamic changes while still typing */

        return ic;
    }
}