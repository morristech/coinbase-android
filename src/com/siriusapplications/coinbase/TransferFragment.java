package com.siriusapplications.coinbase;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.siriusapplications.coinbase.api.RpcManager;

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
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
          int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
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

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
      int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
      setReceiveAddress(prefs.getString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), null));
    }

    @Override
    protected void onPostExecute(String result) {

      setReceiveAddress(result);
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
      String message = String.format(getString(messageResource), amount, toFrom);

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

  private MainActivity mParent;

  private Spinner mTransferTypeView;
  private Button mSubmitSend, mSubmitEmail, mSubmitQr, mSubmitNfc, mGenerateAddress;
  private EditText mAmountView, mNotesView;
  private AutoCompleteTextView mRecipientView;
  private TextView mReceiveAddress;
  private ImageView mReceiveAddressBarcode;
  
  private SimpleCursorAdapter mAutocompleteAdapter;

  private int mTransferType;
  private String mAmount, mNotes, mRecipient;

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
    mGenerateAddress = (Button) view.findViewById(R.id.transfer_address_generate);
    mReceiveAddressBarcode = (ImageView) view.findViewById(R.id.transfer_address_barcode);

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
        b.putString("amount", mAmount);
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
        b.putString("amount", mAmount);
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

    mGenerateAddress.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        new LoadReceiveAddressTask().execute(true);
      }
    });

    new LoadReceiveAddressTask().execute(false);

    return view;
  }
  
  private String generateRequestUri() {
    
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String receiveAddress = prefs.getString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), null);
    String requestUri = String.format("bitcoin:%s", receiveAddress);

    boolean hasAmount = false;

    if(mAmount != null && !"".equals(mAmount)) {
      requestUri += "?amount=" + mAmount;
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

    if(address == null) {
      mReceiveAddress.setText(null);
      mReceiveAddressBarcode.setImageDrawable(null);
      mReceiveAddressBarcode.setOnClickListener(null);
      return;
    }

    if(mReceiveAddress != null) {
      mReceiveAddress.setText(address);
    }

    if(mReceiveAddressBarcode != null) {
      try {
        int size = (int)(mParent.getResources().getDisplayMetrics().density * 128);
        Bitmap barcode = Utils.createBarcode("bitcoin:" + address, BarcodeFormat.QR_CODE,
            size, size);
        mReceiveAddressBarcode.setImageBitmap(barcode);

      } catch (WriterException e) {
        // Could not generate barcode
        e.printStackTrace();
      }
      
      mReceiveAddressBarcode.setOnClickListener(new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {

          DisplayQrCodeFragment f = new DisplayQrCodeFragment();
          Bundle args = new Bundle();
          args.putString("data", "bitcoin:" + address);
          f.setArguments(args);
          f.show(getFragmentManager(), "qrreceive");
        }
      });
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

    mAmount = amount;
    mNotes = message;
    mRecipient = address;
    mTransferType = 0;

    if(mTransferTypeView != null) {
      mTransferTypeView.setSelection(0); // SEND
      mAmountView.setText(amount);
      mNotesView.setText(message);
      mRecipientView.setText(address);
    }
  }

  public void refresh() {

    // Reload receive address
    new LoadReceiveAddressTask().execute(false);
  }
}
