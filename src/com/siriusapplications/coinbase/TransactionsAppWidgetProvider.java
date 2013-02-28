package com.siriusapplications.coinbase;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.siriusapplications.coinbase.UpdateWidgetBalanceService.WidgetUpdater;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TransactionsAppWidgetProvider extends AppWidgetProvider {
  
  public static class TransactionsWidgetUpdater implements WidgetUpdater {

    @Override
    public void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, String balance) {
      
      Intent intent = new Intent(context, TransactionsRemoteViewsService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
      
      RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_transactions);
      rv.setRemoteAdapter(appWidgetId, R.id.widget_list, intent);
      rv.setEmptyView(R.id.widget_list, R.id.widget_empty);
      
      Intent intentTemplate = new Intent(context, TransactionDetailsActivity.class);
      PendingIntent pendingIntentTemplate = PendingIntent.getActivity(context, 0, intentTemplate, 0);
      rv.setPendingIntentTemplate(R.id.widget_list, pendingIntentTemplate);
      
      rv.setTextViewText(R.id.widget_balance, balance);
      
      WidgetCommon.bindButtons(context, rv, appWidgetId);

      Log.i("Coinbase", "Updating widget " + appWidgetId + " with balance " + balance);
      manager.updateAppWidget(appWidgetId, rv); 
    }
    
  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    
    for (int i = 0; i < appWidgetIds.length; ++i) {
      
      if(!appWidgetManager.getAppWidgetInfo(appWidgetIds[i]).provider.getClassName().equals(getClass().getName())) {
        // Not for us
        continue;
      }
      
      // First, update the widget immediately without balance
      new TransactionsWidgetUpdater().updateWidget(context, appWidgetManager, appWidgetIds[i], null);
      
      // Then, start the service to update the widget with balance
      Intent service = new Intent(context, UpdateWidgetBalanceService.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_UPDATER_CLASS, TransactionsWidgetUpdater.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_WIDGET_ID, appWidgetIds[i]);
      context.startService(service);
    }
  }
}
