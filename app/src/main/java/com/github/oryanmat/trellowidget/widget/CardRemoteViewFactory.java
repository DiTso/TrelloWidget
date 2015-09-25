package com.github.oryanmat.trellowidget.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.github.oryanmat.trellowidget.R;
import com.github.oryanmat.trellowidget.TrelloWidget;
import com.github.oryanmat.trellowidget.model.BoardList;
import com.github.oryanmat.trellowidget.model.Card;
import com.github.oryanmat.trellowidget.model.Label;
import com.github.oryanmat.trellowidget.util.LabelColors;
import com.github.oryanmat.trellowidget.util.PrefUtil;
import com.github.oryanmat.trellowidget.util.TrelloAPIUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.graphics.Color.alpha;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static com.github.oryanmat.trellowidget.util.RemoteViewsUtil.setImage;
import static com.github.oryanmat.trellowidget.util.RemoteViewsUtil.setImageViewColor;
import static com.github.oryanmat.trellowidget.util.RemoteViewsUtil.setTextView;

public class CardRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    static final String DATE_PARSE_ERROR = "Bad date";

    static final SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    static final SimpleDateFormat widgetFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    private final Context context;
    int appWidgetId;
    BoardList list;
    List<Card> cards = new ArrayList<>();
    @ColorInt
    int color;

    public CardRemoteViewFactory(Context context, int appWidgetId) {
        this.context = context;
        this.appWidgetId = appWidgetId;
        list = TrelloWidget.getList(context, appWidgetId);
        // it appears that trello returns dates in UTC time zone
        apiFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void onDataSetChanged() {
        this.cards = Arrays.asList(TrelloAPIUtil.instance.getCards(list).cards);
        color = PrefUtil.getForegroundColor(context);
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Card card = cards.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.card);
        setLabels(views, card);
        setTitle(views, card);
        setBadges(views, card);
        setDivider(views);
        setOnClickFillInIntent(card, views);

        return views;
    }

    private void setOnClickFillInIntent(Card card, RemoteViews views) {
        Intent intent = new Intent();
        intent.putExtra(TrelloWidgetProvider.CARD_EXTRA, card.id);
        views.setOnClickFillInIntent(R.id.card, intent);
    }

    private void setBadges(RemoteViews views, Card card) {
        setSubscribed(views, card);
        setVotes(views, card);
        setDescription(views, card);
        setComments(views, card);
        setChecklist(views, card);
        setDueDate(views, card);
        setAttachments(views, card);
    }

    private void setTitle(RemoteViews views, Card card) {
        setTextView(context, views, R.id.card_title, card.name, color, R.dimen.card_badges_text);
    }

    private void setSubscribed(RemoteViews views, Card card) {
        setBadge(views, R.id.subscribed, R.drawable.ic_visibility_white_24dp, card.badges.subscribed);
    }

    void setVotes(RemoteViews views, Card card) {
        setIntBadge(views, R.id.votes, R.id.vote_count,
                R.drawable.ic_thumb_up_white_24dp, card.badges.votes);
    }

    private void setDescription(RemoteViews views, Card card) {
        setBadge(views, R.id.desc, R.drawable.ic_subject_white_24dp, card.badges.description);
    }

    void setDueDate(RemoteViews views, Card card) {
        boolean visible = card.badges.due != null;
        String text = visible ? parseDate(card.badges.due) : "";
        setBadge(views, R.id.due, R.id.due_string,
                R.drawable.ic_access_time_white_24dp, text, visible);
    }

    void setChecklist(RemoteViews views, Card card) {
        String text = String.format("%d/%d", card.badges.checkItemsChecked, card.badges.checkItems);
        boolean visible = card.badges.checkItems > 0;
        setBadge(views, R.id.checklist, R.id.checklist_count,
                R.drawable.ic_check_box_white_24dp, text, visible);
    }

    void setComments(RemoteViews views, Card card) {
        setIntBadge(views, R.id.comments, R.id.comment_count,
                R.drawable.ic_chat_bubble_outline_white_24dp, card.badges.comments);
    }

    void setAttachments(RemoteViews views, Card card) {
        setIntBadge(views, R.id.attachment, R.id.attachment_count,
                R.drawable.ic_attachment_white_24dp, card.badges.attachments);
    }

    void setIntBadge(RemoteViews views, @IdRes int view, @IdRes int textView,
                     @DrawableRes int image, int value) {
        setBadge(views, view, textView, image, String.valueOf(value), value > 0);
    }

    void setBadge(RemoteViews views, @IdRes int view, @IdRes int textView,
                  @DrawableRes int image, String text, boolean visible) {
        setTextView(context, views, textView, text, color, R.dimen.card_badges_text);
        views.setViewVisibility(textView, visible ? View.VISIBLE : View.GONE);
        setBadge(views, view, image, visible);
    }

    void setBadge(RemoteViews views, @IdRes int view, @DrawableRes int image, boolean visible) {
        views.setViewVisibility(view, visible ? View.VISIBLE : View.GONE);
        setImageViewColor(views, view, color);
        setImage(context, views, view, image);
    }

    private void setLabels(RemoteViews views, Card card) {
        views.removeAllViews(R.id.labels_layout);

        if (card.labels != null && card.labels.length > 0) {
            for (Label label : card.labels) {
                setLabel(views, label);
            }
        }
    }

    private void setLabel(RemoteViews views, Label label) {
        int labelColor = LabelColors.colors.get(label.color);
        labelColor = Color.argb(alpha(color), red(labelColor), green(labelColor), blue(labelColor));
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.label);
        setImageViewColor(view, R.id.label, labelColor);
        setImage(context, view, R.id.label, R.drawable.label);
        views.addView(R.id.labels_layout, view);
    }

    private void setDivider(RemoteViews views) {
        setImageViewColor(views, R.id.list_item_divider, color);
    }

    private String parseDate(String date) {
        try {
            return widgetFormat.format(apiFormat.parse(date));
        } catch (ParseException e) {
            return DATE_PARSE_ERROR;
        }
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return cards.size();
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}