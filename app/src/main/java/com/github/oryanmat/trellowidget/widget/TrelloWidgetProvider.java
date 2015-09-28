package com.github.oryanmat.trellowidget.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.github.oryanmat.trellowidget.R;
import com.github.oryanmat.trellowidget.TrelloWidget;
import com.github.oryanmat.trellowidget.activity.CardActivity;
import com.github.oryanmat.trellowidget.model.BoardList;
import com.github.oryanmat.trellowidget.util.PrefUtil;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS;
import static com.github.oryanmat.trellowidget.util.RemoteViewsUtil.setImageViewColor;
import static com.github.oryanmat.trellowidget.util.RemoteViewsUtil.setTextView;

public class TrelloWidgetProvider extends AppWidgetProvider {
    private static final String REFRESH_ACTION = "com.github.oryanmat.trellowidget.refreshAction";
    public static final String WIDGET_ID = "com.github.oryanmat.trellowidget.widgetId";
    public static final String CARD_EXTRA = "com.github.oryanmat.trellowidget.card";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        BoardList list = TrelloWidget.getList(context, appWidgetId);
        Intent intent = getRemoteAdapterIntent(context, appWidgetId);
        PendingIntent pendingIntent = getRefreshPendingIntent(context, appWidgetId);
        @ColorInt int color = PrefUtil.getForegroundColor(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.trello_widget);
        setTextView(views, R.id.list_title, list.name, color);
        views.setOnClickPendingIntent(R.id.refreshButt, pendingIntent);
        views.setPendingIntentTemplate(R.id.card_list, getCardPendingIntent(context));
        setImageViewColor(views, R.id.refreshButt, color);
        setImageViewColor(views, R.id.divider, color);
        views.setRemoteAdapter(R.id.card_list, intent);
        views.setEmptyView(R.id.card_list, R.id.empty_card_list);
        views.setTextColor(R.id.empty_card_list, color);
        setImageViewColor(views, R.id.background_image,
                PrefUtil.getBackgroundColor(context));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private Intent getRemoteAdapterIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        return intent;
    }

    private PendingIntent getRefreshPendingIntent(Context context, int appWidgetId) {
        Intent refreshIntent = new Intent(context, TrelloWidgetProvider.class);
        refreshIntent.setAction(REFRESH_ACTION);
        refreshIntent.putExtra(WIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, 0);
    }

    private PendingIntent getCardPendingIntent(Context context) {
        Intent intent = new Intent(context, CardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(REFRESH_ACTION)) {
            notifyDataChanged(context, intent.getIntExtra(WIDGET_ID, 0));
        }
    }

    public static void updateWidgetsData(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName compName = new ComponentName(context, TrelloWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(compName);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.card_list);
    }

    public static void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName compName = new ComponentName(context, TrelloWidgetProvider.class);
        sendUpdateBroadcast(context, appWidgetManager.getAppWidgetIds(compName));
    }

    public static void updateWidget(Context context, int appWidgetId) {
        sendUpdateBroadcast(context, appWidgetId);
        notifyDataChanged(context, appWidgetId);
    }

    private static void sendUpdateBroadcast(Context context, int... appWidgetIds) {
        Intent intent = new Intent(context, TrelloWidgetProvider.class);
        intent.setAction(ACTION_APPWIDGET_UPDATE);
        intent.putExtra(EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    private static void notifyDataChanged(Context context, int... appWidgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.card_list);
    }
}
