package com.coinbase.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * SQLite database that is synced with transactions.
 * Also holds email addresses used for auto-complete.
 */
public class TransactionsDatabase extends SQLiteOpenHelper {

  public static class TransactionEntry implements BaseColumns {

    public static final String TABLE_NAME = "coinbaseTransactions";
    public static final String COLUMN_NAME_JSON = "json";
    public static final String COLUMN_NAME_ACCOUNT = "account";
    public static final String COLUMN_NAME_TIME = "timestamp";
    public static final String COLUMN_NAME_NUMERIC_ID = "numeric_id";
  }

  public static class EmailEntry {

    public static final String TABLE_NAME = "coinbaseEmails";
    public static final String COLUMN_NAME_EMAIL = "_id";
    public static final String COLUMN_NAME_ACCOUNT = "account";
  }

  public static final String TEXT_TYPE = " TEXT";
  public static final String INTEGER_TYPE = " INTEGER";
  public static final String COMMA_SEP = ",";
  
  public static final String SQL_CREATE_ENTRIES =
      "CREATE TABLE " + TransactionEntry.TABLE_NAME + " (" +
          TransactionEntry.COLUMN_NAME_NUMERIC_ID + INTEGER_TYPE + " PRIMARY KEY AUTOINCREMENT NOT NULL" + COMMA_SEP +
          TransactionEntry._ID + " TEXT," +
          TransactionEntry.COLUMN_NAME_JSON + TEXT_TYPE + COMMA_SEP +
          TransactionEntry.COLUMN_NAME_ACCOUNT + INTEGER_TYPE + COMMA_SEP +
          TransactionEntry.COLUMN_NAME_TIME + INTEGER_TYPE +
          ")";
  
  public static final String SQL_CREATE_ENTRIES_EMAIL =
      "CREATE TABLE " + EmailEntry.TABLE_NAME + " (" +
          EmailEntry.COLUMN_NAME_EMAIL + " TEXT PRIMARY KEY," +
          EmailEntry.COLUMN_NAME_ACCOUNT + INTEGER_TYPE +
          ")";

  public static final String SQL_DELETE_ENTRIES =
      "DROP TABLE IF EXISTS " + TransactionEntry.TABLE_NAME;

  public static final String SQL_DELETE_ENTRIES_EMAIL =
      "DROP TABLE IF EXISTS " + EmailEntry.TABLE_NAME;

  public static final int DATABASE_VERSION = 7;
  public static final String DATABASE_NAME = "transactions";

  public TransactionsDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SQL_CREATE_ENTRIES);
    db.execSQL(SQL_CREATE_ENTRIES_EMAIL);
  }
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    // Transactions will be re-synced; just wipe the database for now
    db.execSQL(SQL_DELETE_ENTRIES);
    db.execSQL(SQL_DELETE_ENTRIES_EMAIL);
    onCreate(db);
  }
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onUpgrade(db, oldVersion, newVersion);
  }
}
