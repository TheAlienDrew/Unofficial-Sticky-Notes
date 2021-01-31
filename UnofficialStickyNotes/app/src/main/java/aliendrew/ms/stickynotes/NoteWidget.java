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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.google.common.collect.Maps;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link NoteWidgetConfigureActivity NoteWidgetConfigureActivity}
 */
public class NoteWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // note themes (variable map)
        Map<String, Integer> LIGHT_THEME_INTEGERS = Maps.newHashMap();
        Map<String, Integer> DARK_THEME_INTEGERS = Maps.newHashMap();
        Map<String, Integer> DARK_TIMESTAMP_INTEGERS = Maps.newHashMap();
        LIGHT_THEME_INTEGERS.put("Yellow", R.drawable.note_yellow_light_layout);
        LIGHT_THEME_INTEGERS.put("Green", R.drawable.note_green_light_layout);
        LIGHT_THEME_INTEGERS.put("Pink", R.drawable.note_pink_light_layout);
        LIGHT_THEME_INTEGERS.put("Purple", R.drawable.note_purple_light_layout);
        LIGHT_THEME_INTEGERS.put("Blue", R.drawable.note_blue_light_layout);
        LIGHT_THEME_INTEGERS.put("Gray", R.drawable.note_gray_light_layout);
        LIGHT_THEME_INTEGERS.put("Charcoal", R.drawable.note_charcoal_light_layout);
        DARK_THEME_INTEGERS.put("Yellow", R.drawable.note_yellow_dark_layout);
        DARK_THEME_INTEGERS.put("Green", R.drawable.note_green_dark_layout);
        DARK_THEME_INTEGERS.put("Pink", R.drawable.note_pink_dark_layout);
        DARK_THEME_INTEGERS.put("Purple", R.drawable.note_purple_dark_layout);
        DARK_THEME_INTEGERS.put("Blue", R.drawable.note_blue_dark_layout);
        DARK_THEME_INTEGERS.put("Gray", R.drawable.note_gray_dark_layout);
        DARK_THEME_INTEGERS.put("Charcoal", R.drawable.note_charcoal_dark_layout);
        DARK_TIMESTAMP_INTEGERS.put("Yellow", R.color.yellowDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Green", R.color.greenDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Pink", R.color.pinkDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Purple", R.color.purpleDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Blue", R.color.blueDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Gray", R.color.grayDarkStroke);
        DARK_TIMESTAMP_INTEGERS.put("Charcoal", R.color.darkNoteTimestamp);

        String noteDataJSON = NoteWidgetConfigureActivity.loadNotePref(context, appWidgetId);
        JSONObject noteData;
        String noteId;
        String noteColor;
        String noteTimestamp;
        String noteContent;
        Integer noteLayoutColor;
        int noteTimestampColor;
        int noteContentTextColor;
        int noteContentLinkColor;
        try {
            noteData = new JSONObject(noteDataJSON);
            noteId = noteData.getString("id");
            noteColor = noteData.getString("color");
            noteTimestamp = noteData.getString("timestamp");
            noteContent = noteData.getString("content");

            // based on system theme being used
            if ((Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES) {
                // system is dark
                noteLayoutColor = DARK_THEME_INTEGERS.get(noteColor);
                noteTimestampColor = DARK_TIMESTAMP_INTEGERS.get(noteColor);
                noteContentTextColor = R.color.darkNoteContentText;
                noteContentLinkColor = R.color.darkNoteContentLink;
            } else { // system is light
                // Charcoal has special circumstance theme settings
                boolean isCharcoal = noteColor.equals("Charcoal");

                noteLayoutColor = LIGHT_THEME_INTEGERS.get(noteColor);
                noteTimestampColor = isCharcoal ? R.color.darkNoteTimestamp : R.color.lightNoteTimestamp;
                noteContentTextColor = isCharcoal ? R.color.charcoalLightContentText : R.color.lightNoteContentText;
                noteContentLinkColor = isCharcoal ? R.color.charcoalLightContentLink : R.color.lightNoteContentLink;
            }
        } catch (JSONException e) {
            noteData = null;
            noteId = null;
            noteColor = "invalid";
            noteTimestamp = "ERROR";
            noteContent = "<p><b>Contents of note couldn't load!</b></p>";
            noteLayoutColor = R.drawable.note_invalid_layout;
            noteTimestampColor = R.color.lightNoteTimestamp;
            noteContentTextColor = R.color.invalidContentText;
            noteContentLinkColor = R.color.invalidContentLink;
        }

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_widget);

        // bullets need to be added on for older android versions due to their small size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // using some formatting trickery to get the bullet the right size without making a hassle out of Bitmaps or Drawables
            noteContent = noteContent.replaceAll("<li>", "<li><small><sup><sub>&#9679</sub></sup></small> ");
        }

        // Html string needs to be converted based on build
        final Spanned htmlSpannable; // noteContentHTML
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            htmlSpannable = Html.fromHtml(noteContent, Html.FROM_HTML_MODE_COMPACT);
        } else {
            htmlSpannable = Html.fromHtml(noteContent);
        }
        // fixes bullet formatting
        final int bulletColor = ContextCompat.getColor(context, noteContentTextColor);
        final SpannableStringBuilder noteContentHTML = new SpannableStringBuilder(htmlSpannable);
        final BulletSpan[] bulletSpans = noteContentHTML.getSpans(0, noteContentHTML.length(), BulletSpan.class);
        for (BulletSpan it: bulletSpans) {
            final int start = noteContentHTML.getSpanStart(it);
            final int end = noteContentHTML.getSpanEnd(it);
            noteContentHTML.removeSpan(it);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                noteContentHTML.setSpan(new BulletSpan(25, bulletColor,7), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                //noteContentHTML.setSpan(new BulletSpan(25, bulletColor), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                // NOTE: this method below (and it's counter-part not too far above) seems to help make the bullets look normal
                noteContentHTML.setSpan(new LeadingMarginSpan.Standard(0,30), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        // set layouts, colors, and strings in the RemoteViews
        views.setTextViewText(R.id.appwidget_timestamp, noteTimestamp);
        views.setTextViewText(R.id.appwidget_content, noteContentHTML);
        views.setInt(R.id.appwidget_stroke, "setBackgroundResource", noteLayoutColor);
        views.setInt(R.id.appwidget_timestamp, "setTextColor", ContextCompat.getColor(context, noteTimestampColor));
        views.setInt(R.id.appwidget_content, "setTextColor", ContextCompat.getColor(context, noteContentTextColor));
        views.setInt(R.id.appwidget_content, "setLinkTextColor", ContextCompat.getColor(context, noteContentLinkColor));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            NoteWidgetConfigureActivity.deleteNotePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}