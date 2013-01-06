package com.siriusapplications.coinbase.db;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * SQLite database that is synced with transactions.
 * Supports full text search using the SQLite FTS extension.
 */
public class TransactionsDatabase {
    private static final String TAG = "TransactionsDatabase";

    // Columns
    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_OWNER_ID = "owner_id";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_AMOUNT_VALUE = "amount_value";
    public static final String KEY_AMOUNT_CURRENCY = "amount_currency";
    public static final String KEY_IS_REQUEST = "is_request";
    public static final String KEY_STATUS = "status";

    public static final String KEY_SENDER_ID = "sender_id";
    public static final String KEY_SENDER_NAME = "sender_name";
    public static final String KEY_SENDER_EMAIL = "sender_email";
    public static final String KEY_SENDER_IS_BITCOIN_ADDRESS = "sender_is_bitcoin_address";
    public static final String KEY_SENDER_BITCOIN_ADDRESS = "sender_bitcoin_address";
    
    public static final String KEY_RECIPIENT_ID = "recipient_id";
    public static final String KEY_RECIPIENT_NAME = "recipient_name";
    public static final String KEY_RECIPIENT_EMAIL = "recipient_email";
    public static final String KEY_RECIPIENT_IS_BITCOIN_ADDRESS = "recipient_is_bitcoin_address";
    public static final String KEY_RECIPIENT_BITCOIN_ADDRESS = "recipient_bitcoin_address";

    private static final String DATABASE_NAME = "transactions";
    private static final String TABLE_NAME = "transactions";
    private static final int DATABASE_VERSION = 1;

    private final TransactionsOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMap = buildColumnMap();

    public TransactionsDatabase(Context context) {
        mDatabaseOpenHelper = new TransactionsOpenHelper(context);
    }

    private static HashMap<String,String> buildColumnMap() {
        HashMap<String,String> map = new HashMap<String,String>();

        map.put(KEY_ID, KEY_ID);
        map.put(KEY_OWNER_ID, KEY_OWNER_ID);
        map.put(KEY_CREATED_AT, KEY_CREATED_AT);
        map.put(KEY_AMOUNT_VALUE, KEY_AMOUNT_VALUE);
        map.put(KEY_AMOUNT_CURRENCY, KEY_AMOUNT_CURRENCY);
        map.put(KEY_IS_REQUEST, KEY_IS_REQUEST);
        map.put(KEY_STATUS, KEY_STATUS);
        
        map.put(KEY_SENDER_ID, KEY_SENDER_ID);
        map.put(KEY_SENDER_NAME, KEY_SENDER_NAME);
        map.put(KEY_SENDER_EMAIL, KEY_SENDER_EMAIL);
        map.put(KEY_SENDER_IS_BITCOIN_ADDRESS, KEY_SENDER_IS_BITCOIN_ADDRESS);
        map.put(KEY_SENDER_BITCOIN_ADDRESS, KEY_SENDER_BITCOIN_ADDRESS);
        
        map.put(KEY_RECIPIENT_ID, KEY_RECIPIENT_ID);
        map.put(KEY_RECIPIENT_NAME, KEY_RECIPIENT_NAME);
        map.put(KEY_RECIPIENT_EMAIL, KEY_RECIPIENT_EMAIL);
        map.put(KEY_RECIPIENT_IS_BITCOIN_ADDRESS, KEY_RECIPIENT_IS_BITCOIN_ADDRESS);
        map.put(KEY_RECIPIENT_BITCOIN_ADDRESS, KEY_RECIPIENT_BITCOIN_ADDRESS);
        
        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        
        // For SearchManager suggestions.
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    /** Performs a database query */
    public Cursor query(String selection, String[] selectionArgs, String[] columns) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(TABLE_NAME);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
          
            return null;
        } else if (!cursor.moveToFirst()) {
          
            cursor.close();
            return null;
        }
        return cursor;
    }

    private static class TransactionsOpenHelper extends SQLiteOpenHelper {

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        TransactionsOpenHelper(Context context) {
          
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
          
            mDatabase = db;
            
            String tableCreate =
                "CREATE TABLE " + TABLE_NAME + " ("
                
                + KEY_ID + " STRING PRIMARY KEY,"
                
                + ");";
            
            mDatabase.execSQL(tableCreate);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
          
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + " - wiping all data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

}
