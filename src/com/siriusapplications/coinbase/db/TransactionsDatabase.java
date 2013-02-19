package com.siriusapplications.coinbase.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * SQLite database that is synced with transactions.
 */
public class TransactionsDatabase extends SQLiteOpenHelper {

  public static class TransactionEntry implements BaseColumns {

    public static final String TABLE_NAME = "coinbaseTransactions";
    public static final String COLUMN_NAME_JSON = "json";
    public static final String COLUMN_NAME_TIME = "timestamp";
  }

  public static final String TEXT_TYPE = " TEXT";
  public static final String INTEGER_TYPE = " INTEGER";
  public static final String COMMA_SEP = ",";
  public static final String SQL_CREATE_ENTRIES =
      "CREATE TABLE " + TransactionEntry.TABLE_NAME + " (" +
          TransactionEntry._ID + " TEXT PRIMARY KEY," +
          TransactionEntry.COLUMN_NAME_JSON + TEXT_TYPE + COMMA_SEP +
          TransactionEntry.COLUMN_NAME_TIME + INTEGER_TYPE +
          ")";

  public static final String SQL_DELETE_ENTRIES =
      "DROP TABLE IF EXISTS " + TransactionEntry.TABLE_NAME;

  public static final int DATABASE_VERSION = 3;
  public static final String DATABASE_NAME = "transactions";

  public TransactionsDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_ENTRIES);
  }
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    // No old versions yet
    db.execSQL(SQL_DELETE_ENTRIES);
    onCreate(db);
  }
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }
}
