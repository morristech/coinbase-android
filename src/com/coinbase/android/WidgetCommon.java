package com.coinbase.android;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.coinbase.android.R;

public class WidgetCommon {

  public static void bindButtons(Context context, RemoteViews rv, int appWidgetId) {
    
    // Main menu
    Intent mainMenu = new Intent(context, MainActivity.class);
    mainMenu.setAction(MainActivity.ACTION_TRANSACTIONS);
    PendingIntent pMainMenu = PendingIntent.getActivity(context, 0, mainMenu, 0);
    rv.setOnClickPendingIntent(R.id.widget_icon, pMainMenu);
    
    // Scan
    Intent scan = new Intent(context, MainActivity.class);
    scan.setAction(MainActivity.ACTION_SCAN);
    PendingIntent pScan = PendingIntent.getActivity(context, 0, scan, 0);
    rv.setOnClickPendingIntent(R.id.widget_scan, pScan);
    
    // Refresh
    Intent refresh = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
    refresh.setPackage(context.getPackageName());
    PendingIntent pRefresh = PendingIntent.getBroadcast(context, appWidgetId, refresh, 0);
    rv.setOnClickPendingIntent(R.id.widget_refresh, pRefresh);
    
    // Transfer
    Intent transfer = new Intent(context, MainActivity.class);
    transfer.setAction(MainActivity.ACTION_TRANSFER);
    PendingIntent pTransfer = PendingIntent.getActivity(context, 0, transfer, 0);
    rv.setOnClickPendingIntent(R.id.widget_transfer, pTransfer);
  }
}
