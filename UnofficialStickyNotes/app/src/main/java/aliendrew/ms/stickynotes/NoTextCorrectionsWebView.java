package aliendrew.ms.stickynotes;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class NoTextCorrectionsWebView extends WebView {

    public NoTextCorrectionsWebView(Context context) {
        super(context);
    }

    public NoTextCorrectionsWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoTextCorrectionsWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);

        // VISIBLE_PASSWORD type prevents dynamic changes while still typing
        // NO_SUGGESTIONS disables suggestions for most keyboards
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

        return ic;
    }
}