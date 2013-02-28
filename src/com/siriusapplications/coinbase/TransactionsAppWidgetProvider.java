package com.siriusapplications.coinbase;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TransactionsAppWidgetProvider extends AppWidgetProvider {

  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    
    for (int i = 0; i < appWidgetIds.length; ++i) {

      Intent intent = new Intent(context, TransactionsRemoteViewsService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
      intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
      
      RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_transactions);
      rv.setRemoteAdapter(appWidgetIds[i], R.id.widget_list, intent);
      rv.setEmptyView(R.id.widget_list, R.id.widget_empty);
      
      Intent intentTemplate = new Intent(context, TransactionDetailsActivity.class);
      PendingIntent pendingIntentTemplate = PendingIntent.getActivity(context, 0, intentTemplate, 0);
      rv.setPendingIntentTemplate(R.id.widget_list, pendingIntentTemplate);

      appWidgetManager.updateAppWidget(appWidgetIds[i], rv);   
    }
    
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }
}
