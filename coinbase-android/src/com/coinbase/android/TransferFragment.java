package com.coinbase.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.coinbase.android.Utils.CurrencyType;
import com.coinbase.android.db.TransactionsDatabase;
import com.coinbase.android.db.TransactionsDatabase.EmailEntry;
import com.coinbase.api.RpcManager;

public class TransferFragment extends Fragment {

  protected enum TransferType {
    SEND(R.string.transfer_send_money, "send"),
    REQUEST(R.string.transfer_request_money, "request");

    private int mFriendlyName;
    private String mRequestName;

    private TransferType(int friendlyName, String requestName) {

      mFriendlyName = friendlyName;
      mRequestName = requestName;
    }

    public int getName() {

      return mFriendlyName;
    }

    public String getRequestName() {

      return mRequestName;
    }
  }

  private class DoTransferTask extends AsyncTask<Object, Void, Object[]> {

    private ProgressDialog mDialog;

    @Override
    protected void onPreExecute() {

      super.onPreExecute();
      mDialog = ProgressDialog.show(mParent, null, getString(R.string.transfer_progress));
    }

    protected Object[] doInBackground(Object... params) {

      return doTransfer((TransferType) params[0], (String) params[1], (String) params[2], (String) params[3]);
    }

    protected void onPostExecute(Object[] result) {

      mDialog.dismiss();

      boolean success = (Boolean) result[0];
      if(success) {

        TransferType type = (TransferType) result[2];

        int messageId = type == TransferType.SEND ? R.string.transfer_success_send : R.string.transfer_success_request;
        String text = String.format(getString(messageId), (String) result[1], (String) result[3]);
        Toast.makeText(mParent, text, Toast.LENGTH_SHORT).show();

        // Sync transactions
        mParent.refresh();
      } else {

        Utils.showMessageDialog(getFragmentManager(), (String) result[1]);
      }
    }
  }

  private class LoadReceiveAddressTask extends AsyncTask<Boolean, Void, String> {

    @Override
    protected String doInBackground(Boolean... params) {

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
      int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);

      try {

        boolean shouldGenerateNew = params[0];
        String address;

        if(shouldGenerateNew) {

          JSONObject response = RpcManager.getInstance().callPost(mParent, "account/generate_receive_address", null);

          address = response.optString("address");

        } else {

          JSONObject response = RpcManager.getInstance().callGet(mParent, "account/receive_address");

          address = response.optString("address");
        }

        if(address != null) {
          // Save balance in preferences
          Editor editor = prefs.edit();
          editor.putString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), address);
          editor.commit();
        }

        return address;

      } catch (IOException e) {

        e.printStackTrace();
      } catch (JSONException e) {

        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPreExecute() {

      loadReceiveAddressFromPreferences();
    }

    @Override
    protected void onPostExecute(String result) {

      if(result == null) {

        loadReceiveAddressFromPreferences();
        return;
      }

      setReceiveAddress(result);
    }

  }

  private class ReloadContactsDatabaseTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {

      JSONArray contacts;

      // Fetch emails
      try {
        contacts = RpcManager.getInstance().callGet(mParent, "contacts", null).getJSONArray("contacts");

      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } catch (JSONException e) {
        e.printStackTrace();
        return null;
      } 

      TransactionsDatabase dbHelper = new TransactionsDatabase(mParent);
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
      int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);

      try {

        db.beginTransaction();

        // Remove all old emails
        db.delete(EmailEntry.TABLE_NAME, null, null);


        for(int i = 0; i < contacts.length(); i++) {

          String email = contacts.getJSONObject(i).getJSONObject("contact").getString("email");

          ContentValues emailValues = new ContentValues();
          emailValues.put(EmailEntry.COLUMN_NAME_EMAIL, email);
          emailValues.put(EmailEntry.COLUMN_NAME_ACCOUNT, activeAccount);
          db.insertWithOnConflict(EmailEntry.TABLE_NAME, null, emailValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        db.setTransactionSuccessful();

      } catch (JSONException e) {
        e.printStackTrace();
      } finally {

        db.endTransaction();
        db.close();
      }

      return null;
    }

  }

  public static class ConfirmTransferFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final TransferType type = (TransferType) getArguments().getSerializable("type");
      final String amount = getArguments().getString("amount"),
          toFrom = getArguments().getString("toFrom"),
          notes = getArguments().getString("notes");

      int messageResource = type == TransferType.REQUEST ? R.string.transfer_confirm_message_request : R.string.transfer_confirm_message_send;
      String message = String.format(getString(messageResource), Utils.formatCurrencyAmount(amount), toFrom);

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(message)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {

          // Complete transfer
          TransferFragment parent = getActivity() == null ? null : ((MainActivity) getActivity()).getTransferFragment();

          if(parent != null) {
            parent.startTransferTask(type, amount, notes, toFrom);
          }
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // User cancelled the dialog
        }
      });

      return builder.create();
    }
  }

  private class RefreshExchangeRateTask extends AsyncTask<Void, Void, JSONObject> {

    @Override
    protected JSONObject doInBackground(Void... params) {

      try {

        JSONObject exchangeRates = RpcManager.getInstance().callGet(mParent, "currencies/exchange_rates");
        return exchangeRates;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (JSONException e) {
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPostExecute(JSONObject result) {

      mNativeExchangeTask = null;

      if(result != null) {
        mNativeExchangeRates = result;
        mNativeExchangeRateTime = System.currentTimeMillis();
        doNativeCurrencyUpdate();
      }
    }


  }

  private static final int EXCHANGE_RATE_EXPIRE_TIME = 60000 * 5; // Expires in 5 minutes

  private MainActivity mParent;

  private Spinner mTransferTypeView, mTransferCurrencyView;
  private Button mSubmitSend, mSubmitEmail, mSubmitQr, mSubmitNfc, mCopyAddress;
  private EditText mAmountView, mNotesView;
  private AutoCompleteTextView mRecipientView;
  private TextView mReceiveAddress;

  private SimpleCursorAdapter mAutocompleteAdapter;

  private int mTransferType;
  private String mAmount, mNotes, mRecipient, mTransferCurrency;

  private TextView mNativeAmount;
  private long mNativeExchangeRateTime;
  private JSONObject mNativeExchangeRates;
  private RefreshExchangeRateTask mNativeExchangeTask;
  private String[] mCurrenciesArray = new String[] { "BTC" };
  private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onAttach(Activity activity) {

    super.onAttach(activity);
    mParent = (MainActivity) activity;
  }

  public void setParent(MainActivity activity) {

    mParent = activity;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    Utils.disposeOfEmailAutocompleteAdapter(mAutocompleteAdapter);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    prefs.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_transfer, container, false);

    mTransferTypeView = (Spinner) view.findViewById(R.id.transfer_money_type);
    mTransferTypeView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {

        onTypeChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        // Will never happen.
        throw new RuntimeException("onNothingSelected triggered on transfer type spinner");
      }
    });
    initializeTypeSpinner();

    mTransferCurrencyView = (Spinner) view.findViewById(R.id.transfer_money_currency);
    mTransferCurrencyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {

        onCurrencyChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        // Will never happen.
        throw new RuntimeException("onNothingSelected triggered on transfer currency spinner");
      }
    });
    initializeCurrencySpinner();

    mSubmitSend = (Button) view.findViewById(R.id.transfer_money_button_send);
    mSubmitEmail = (Button) view.findViewById(R.id.transfer_money_button_email);
    mSubmitQr = (Button) view.findViewById(R.id.transfer_money_button_qrcode);
    mSubmitNfc = (Button) view.findViewById(R.id.transfer_money_button_nfc);

    mAmountView = (EditText) view.findViewById(R.id.transfer_money_amt);
    mNotesView = (EditText) view.findViewById(R.id.transfer_money_notes);
    mRecipientView = (AutoCompleteTextView) view.findViewById(R.id.transfer_money_recipient);

    mAutocompleteAdapter = Utils.getEmailAutocompleteAdapter(mParent);
    mRecipientView.setAdapter(mAutocompleteAdapter);
    mRecipientView.setThreshold(0);

    mReceiveAddress = (TextView) view.findViewById(R.id.transfer_address);
    mCopyAddress = (Button) view.findViewById(R.id.transfer_address_copy);

    mNativeAmount = (TextView) view.findViewById(R.id.transfer_money_native);
    mNativeAmount.setText(null);

    mAmountView.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mAmount = s.toString();

        // Update native currency 
        updateNativeCurrency();
      }
    });

    mNotesView.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mNotes = s.toString();
      }
    });

    mRecipientView.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mRecipient = s.toString();
      }
    });

    int currencyIndex = Arrays.asList(mCurrenciesArray).indexOf(mTransferCurrency);
    mTransferCurrencyView.setSelection(currencyIndex == -1 ? 0 : currencyIndex);

    mTransferTypeView.setSelection(mTransferType);
    mAmountView.setText(mAmount);
    mNotesView.setText(mNotes);
    mRecipientView.setText(mRecipient);

    mSubmitSend.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        if("".equals(mAmount)) {

          // No amount entered
          Toast.makeText(mParent, R.string.transfer_amt_empty, Toast.LENGTH_SHORT).show();
          return;
        } else if("".equals(mRecipient)) {

          // No recipient entered
          Toast.makeText(mParent, R.string.transfer_recipient_empty, Toast.LENGTH_SHORT).show();
          return;
        }

        ConfirmTransferFragment dialog = new ConfirmTransferFragment();

        Bundle b = new Bundle();

        b.putSerializable("type", TransferType.values()[mTransferType]);
        b.putString("amount", getBtcAmount().toPlainString());
        b.putString("notes", mNotes);
        b.putString("toFrom", mRecipient);

        dialog.setArguments(b);

        dialog.show(getFragmentManager(), "confirm");
      }
    });

    mSubmitEmail.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        if("".equals(mAmount)) {

          // No amount entered
          Toast.makeText(mParent, R.string.transfer_amt_empty, Toast.LENGTH_SHORT).show();
          return;
        }

        TransferEmailPromptFragment dialog = new TransferEmailPromptFragment();

        Bundle b = new Bundle();

        b.putSerializable("type", TransferType.values()[mTransferType]);
        b.putString("amount", getBtcAmount().toPlainString());
        b.putString("notes", mNotes);

        dialog.setArguments(b);

        dialog.show(getFragmentManager(), "requestEmail");
      }
    });

    mSubmitQr.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        String requestUri = generateRequestUri();

        DisplayQrCodeFragment f = new DisplayQrCodeFragment();
        Bundle args = new Bundle();
        args.putString("data", requestUri);
        f.setArguments(args);
        f.show(getFragmentManager(), "qrrequest");
      }
    });

    mSubmitNfc.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        String requestUri = generateRequestUri();

        SendNfcFragment f = new SendNfcFragment();
        Bundle args = new Bundle();
        args.putString("data", requestUri);
        f.setArguments(args);
        f.show(getFragmentManager(), "nfcrequest");
      }
    });

    mCopyAddress.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        setClipboard(mReceiveAddress.getText().toString());
        Toast.makeText(mParent, R.string.transfer_address_copied, Toast.LENGTH_SHORT).show();
      }
    });

    loadReceiveAddressFromPreferences();

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
          String key) {

        int activeAccount = sharedPreferences.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
        if(key.equals(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount))) {
          // Refresh native currency dropdown
          initializeCurrencySpinner();
        }
      }
    };
    prefs.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

    return view;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void setClipboard(String text) {

    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {

      android.content.ClipboardManager clipboard = 
          (android.content.ClipboardManager) mParent.getSystemService(Context.CLIPBOARD_SERVICE); 
      ClipData clip = ClipData.newPlainText("Coinbase", text);
      clipboard.setPrimaryClip(clip); 
    } else {

      android.text.ClipboardManager clipboard =
          (android.text.ClipboardManager) mParent.getSystemService(Context.CLIPBOARD_SERVICE); 
      clipboard.setText(text);
    }
  }

  private void updateNativeCurrency() {

    if(mNativeExchangeRates == null || 
        (System.currentTimeMillis() - mNativeExchangeRateTime) > EXCHANGE_RATE_EXPIRE_TIME) {

      // Need to fetch exchange rate again
      if(mNativeExchangeTask != null) {
        return;
      }

      refreshExchangeRate();
    } else {
      doNativeCurrencyUpdate();
    }
  }

  private void doNativeCurrencyUpdate() {

    if(mAmount == null || "".equals(mAmount) || ".".equals(mAmount)) {
      mNativeAmount.setText(null);
      return;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String nativeCurrency = prefs.getString(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount),
        "usd").toLowerCase(Locale.CANADA);

    boolean fromBitcoin = "BTC".equalsIgnoreCase(mTransferCurrency);
    String format = fromBitcoin ? "%s_to_" + nativeCurrency.toLowerCase(Locale.CANADA) : "%s_to_btc";
    String key = String.format(format, mTransferCurrency.toLowerCase(Locale.CANADA));
    String resultCurrency = fromBitcoin ? nativeCurrency : "BTC";

    BigDecimal amount = new BigDecimal(mAmount);
    BigDecimal result = amount.multiply(new BigDecimal(mNativeExchangeRates.optString(key, "0")));
    mNativeAmount.setText(String.format(mParent.getString(R.string.transfer_amt_native), Utils.formatCurrencyAmount(result, false, CurrencyType.TRADITIONAL),
        resultCurrency.toUpperCase(Locale.CANADA)));
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void refreshExchangeRate() {
    mNativeExchangeTask = new RefreshExchangeRateTask();

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
      mNativeExchangeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    } else {
      mNativeExchangeTask.execute();
    }
  }

  private void loadReceiveAddressFromPreferences() {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    setReceiveAddress(prefs.getString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), null));
  }

  private BigDecimal getBtcAmount() {

    if(mAmount == null || "".equals(mAmount)) {
      return null;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String nativeCurrency = prefs.getString(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount),
        "usd").toLowerCase(Locale.CANADA);

    boolean fromBitcoin = "BTC".equalsIgnoreCase(mTransferCurrency);
    String format = fromBitcoin ? "%s_to_" + nativeCurrency.toLowerCase(Locale.CANADA) : "%s_to_btc";
    String key = String.format(format, mTransferCurrency.toLowerCase(Locale.CANADA));

    BigDecimal amount = new BigDecimal(mAmount);
    BigDecimal result = amount.multiply(new BigDecimal(mNativeExchangeRates.optString(key, "0")));
    return result;
  }

  private String generateRequestUri() {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String receiveAddress = prefs.getString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), null);
    String requestUri = String.format("bitcoin:%s", receiveAddress);

    boolean hasAmount = false;

    if(mAmount != null && !"".equals(mAmount)) {
      requestUri += "?amount=" + getBtcAmount();
      hasAmount = true;
    }

    if(mNotes != null && !"".equals(mNotes)) {
      if(hasAmount) {
        requestUri += "&";
      } else {
        requestUri += "?";
      }

      requestUri += "message=" + mNotes;
    }

    return requestUri;
  }

  private void setReceiveAddress(final String address) {

    if(mReceiveAddress != null) {
      mReceiveAddress.setText(address);
    }
  }

  private void initializeTypeSpinner() {

    ArrayAdapter<TransferType> arrayAdapter = new ArrayAdapter<TransferType>(
        mParent, R.layout.fragment_transfer_type, Arrays.asList(TransferType.values())) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(mParent.getString(TransferType.values()[position].getName()));
        return view;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        view.setText(mParent.getString(TransferType.values()[position].getName()));
        return view;
      }
    };
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mTransferTypeView.setAdapter(arrayAdapter);
  }

  private void initializeCurrencySpinner() {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String nativeCurrency = prefs.getString(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount),
        "usd").toUpperCase(Locale.CANADA);

    mCurrenciesArray = new String[] {
        "BTC",
        nativeCurrency,
    };

    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
        mParent, R.layout.fragment_transfer_currency, Arrays.asList(mCurrenciesArray)) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(mCurrenciesArray[position]);
        return view;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        view.setText(mCurrenciesArray[position]);
        return view;
      }
    };
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mTransferCurrencyView.setAdapter(arrayAdapter);
  }

  private void onTypeChanged() {

    TransferType type = (TransferType) mTransferTypeView.getSelectedItem();
    mTransferType = mTransferTypeView.getSelectedItemPosition();
    boolean isSend = type == TransferType.SEND;

    mSubmitSend.setVisibility(isSend ? View.VISIBLE : View.GONE);
    mSubmitEmail.setVisibility(isSend ? View.GONE : View.VISIBLE);
    mSubmitQr.setVisibility(isSend ? View.GONE : View.VISIBLE);
    mSubmitNfc.setVisibility(isSend ? View.GONE : View.VISIBLE);
    mRecipientView.setVisibility(isSend ? View.VISIBLE : View.GONE);
  }

  private void onCurrencyChanged() {

    String currency = (String) mTransferCurrencyView.getSelectedItem();
    mTransferCurrency = currency;

    updateNativeCurrency();
  }

  protected void startTransferTask(TransferType type, String amount, String notes, String toFrom) {

    new DoTransferTask().execute(type, amount, notes, toFrom);
  }

  private Object[] doTransfer(TransferType type, String amount, String notes, String toFrom) {

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("transaction[amount]", amount));

    if(notes != null && !"".equals(notes)) {
      params.add(new BasicNameValuePair("transaction[notes]", notes));
    }

    params.add(new BasicNameValuePair(
        String.format("transaction[%s]", type == TransferType.SEND ? "to" : "from"), toFrom));

    try {
      JSONObject response = RpcManager.getInstance().callPost(mParent, 
          String.format("transactions/%s_money", type.getRequestName()), params);

      boolean success = response.getBoolean("success");

      if(success) {

        return new Object[] { true, amount, type, toFrom };
      } else {

        JSONArray errors = response.getJSONArray("errors");
        String errorMessage = "";

        for(int i = 0; i < errors.length(); i++) {
          errorMessage += (errorMessage.equals("") ? "" : "\n") + errors.getString(i);
        }
        return new Object[] { false, String.format(getString(R.string.transfer_error_api), errorMessage) };
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // There was an exception
    return new Object[] { false, getString(R.string.transfer_error_exception) };
  }

  public void fillFormForBitcoinUri(Uri uri) {

    String address = uri.getSchemeSpecificPart().split("\\?")[0];
    String amount = null, label = null, message = null;

    // Parse query
    String query = uri.getQuery();
    if(query != null) {
      try {
        for (String param : query.split("&")) {
          String pair[] = param.split("=");
          String key;
          key = URLDecoder.decode(pair[0], "UTF-8");
          String value = null;
          if (pair.length > 1) {
            value = URLDecoder.decode(pair[1], "UTF-8");
          }

          if("amount".equals(key)) {
            amount = value;
          } else if("label".equals(key)) {
            label = value;
          } else if("message".equals(key)) {
            message = value;
          }
        }
      } catch (UnsupportedEncodingException e) {
        // Will never happen
        throw new RuntimeException(e);
      }
    }

    if(address == null) {

      Log.e("Coinbase", "bitcoin: URI had no address (" + uri + ")");
      return;
    }

    amount = amount.replaceAll("[^0-9\\.]", "");

    mAmount = amount;
    mNotes = message;
    mRecipient = address;
    mTransferType = 0;
    mTransferCurrency = "BTC";

    if(mTransferTypeView != null) {
      mTransferTypeView.setSelection(0); // SEND
      mTransferCurrencyView.setSelection(0); // BTC is always first
      mAmountView.setText(amount);
      mNotesView.setText(message);
      mRecipientView.setText(address);
    }
  }

  public void switchType(boolean isRequest) {

    mTransferType = isRequest ? 1 : 0;

    if(mTransferTypeView != null) {
      mTransferTypeView.setSelection(mTransferType);
    }
  }

  public void refresh() {

    // Reload receive address
    new LoadReceiveAddressTask().execute(false);

    // Reload contacts
    new ReloadContactsDatabaseTask().execute();
  }
}
