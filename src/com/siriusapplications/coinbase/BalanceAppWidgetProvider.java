package com.siriusapplications.coinbase;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

public class BalanceAppWidgetProvider extends AppWidgetProvider {

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    for (int i = 0; i < appWidgetIds.length; i++) {
      int appWidgetId = appWidgetIds[i];

      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balance);

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        setKeyguardPadding(context, appWidgetManager, appWidgetId, views);
      }

      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private void setKeyguardPadding(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views) {

    Bundle myOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
    int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
    boolean isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

    if(isKeyguard) {
      int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
      views.setViewPadding(R.id.widget_outer, padding, padding, padding, padding);
    }
  }
}
