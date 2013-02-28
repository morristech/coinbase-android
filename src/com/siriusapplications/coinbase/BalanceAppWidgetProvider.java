package com.siriusapplications.coinbase;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.siriusapplications.coinbase.UpdateWidgetBalanceService.WidgetUpdater;

public class BalanceAppWidgetProvider extends AppWidgetProvider {

  public static class BalanceWidgetUpdater implements WidgetUpdater {

    @Override
    public void updateWidget(Context context, AppWidgetManager manager,
        int appWidgetId, String balance) {

      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_balance);

      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        setKeyguardPadding(context, manager, appWidgetId, views);
      }

      views.setTextViewText(R.id.widget_balance, balance);

      manager.updateAppWidget(appWidgetId, views);
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

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    for (int i = 0; i < appWidgetIds.length; i++) {

      // First, update the widget immediately without balance
      new BalanceWidgetUpdater().updateWidget(context, appWidgetManager, appWidgetIds[i], null);
      
      // Then, start the service to update the widget with balance
      Intent service = new Intent(context, UpdateWidgetBalanceService.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_UPDATER_CLASS, BalanceWidgetUpdater.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_WIDGET_ID, appWidgetIds[i]);
      context.startService(service);
    }
  }
}
