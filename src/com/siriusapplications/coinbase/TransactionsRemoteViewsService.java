package com.siriusapplications.coinbase;

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.siriusapplications.coinbase.db.TransactionsDatabase;
import com.siriusapplications.coinbase.db.TransactionsDatabase.TransactionEntry;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TransactionsRemoteViewsService extends RemoteViewsService {
  
  public static final String WIDGET_TRANSACTION_LIMIT = "10";

  public class TransactionsRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;

    SQLiteDatabase mDatabase;
    Cursor mCursor;

    public TransactionsRemoteViewsFactory(Context context) {
      mContext = context;
    }

    @Override
    public void onCreate() {

      TransactionsDatabase database = new TransactionsDatabase(mContext);
      mDatabase = database.getReadableDatabase();

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);

      mCursor = mDatabase.query(TransactionsDatabase.TransactionEntry.TABLE_NAME,
          null, TransactionEntry.COLUMN_NAME_ACCOUNT + " = ?", new String[] { Integer.toString(activeAccount) }, null, null, null, WIDGET_TRANSACTION_LIMIT);
    }

    @Override
    public int getCount() {
      return mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
      return mCursor.getLong(mCursor.getColumnIndex(TransactionEntry.COLUMN_NAME_NUMERIC_ID));
    }

    @Override
    public RemoteViews getLoadingView() {
      return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {

      try {

        mCursor.moveToPosition(position);
        JSONObject item = new JSONObject(new JSONTokener(mCursor.getString(mCursor.getColumnIndex(TransactionEntry.COLUMN_NAME_JSON))));

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_transactions_item);

        // Amount:
        String amount = item.getJSONObject("amount").getString("amount");
        String balanceString = Utils.formatCurrencyAmount(amount);

        int sign = new BigDecimal(amount).compareTo(BigDecimal.ZERO);
        int color = sign == -1 ? R.color.transaction_negative : (sign == 0 ? R.color.transaction_neutral : R.color.transaction_positive);

        rv.setTextViewText(R.id.transaction_amount, balanceString);
        rv.setTextColor(R.id.transaction_amount, mContext.getResources().getColor(color));
        
        // Currency:
        rv.setTextViewText(R.id.transaction_currency, item.getJSONObject("amount").getString("currency"));
        
        // Title:
        rv.setTextViewText(R.id.transaction_title, Utils.generateTransactionSummary(mContext, item));
        
        // Status:
        String status = item.optString("status", getString(R.string.transaction_status_error));

        String readable = status;
        int background = R.drawable.transaction_unknown;
        if("complete".equals(status)) {
          readable = getString(R.string.transaction_status_complete);
          background = R.drawable.transaction_complete;
        } else if("pending".equals(status)) {
          readable = getString(R.string.transaction_status_pending);
          background = R.drawable.transaction_pending;
        }

        rv.setTextViewText(R.id.transaction_status, readable);
        rv.setInt(R.id.transaction_status, "setBackgroundResource", background);

        return rv;
      } catch(JSONException e) {
        // Database corruption
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public int getViewTypeCount() {
      return 1;
    }

    @Override
    public boolean hasStableIds() {
      return false;
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {

      mCursor.close();
      mDatabase.close();
    }


  }

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {

    return new TransactionsRemoteViewsFactory(this);
  }

}
