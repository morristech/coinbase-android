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

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

  private enum TransferType {
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
      mDialog = ProgressDialog.show(getActivity(), null, getString(R.string.transfer_progress));
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
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
      } else {
        
        Utils.showMessageDialog(getFragmentManager(), (String) result[1]);
      }
    }
  }

  private class LoadReceiveAddressTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {

      try {

        JSONObject response = RpcManager.getInstance().callGet(getActivity(), "account/receive_address");

        String address = response.optString("address");
        
        if(address != null) {
          // Save balance in preferences
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
      int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
      setReceiveAddress(prefs.getString(String.format(Constants.KEY_ACCOUNT_RECEIVE_ADDRESS, activeAccount), null));
    }

    @Override
    protected void onPostExecute(String result) {

      setReceiveAddress(result);
    }

  }

  private Spinner mTransferTypeView;
  private Button mSubmitSend, mSubmitEmail, mSubmitQr, mSubmitNfc, mGenerateAddress;
  private EditText mAmountView, mNotesView, mRecipientView;
  private TextView mReceiveAddress;
  private ImageView mReceiveAddressBarcode;

  private int mTransferType;
  private String mAmount, mNotes, mRecipient;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
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
    mRecipientView = (EditText) view.findViewById(R.id.transfer_money_recipient);
    
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
        
        startTransferTask(TransferType.SEND, mAmount, mNotes, mRecipient);
      }
    });
    
    mSubmitQr.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        
        String mReceiveAddress = "test";
        String requestUri = String.format("bitcoin:%s", mReceiveAddress);
        boolean hasAmount = false;
        
        if(mAmount != null && !"".equals(mAmount)) {
          requestUri += "?amount=" + mAmount;
        }
        
        if(mNotes != null && !"".equals(mNotes)) {
          if(hasAmount) {
            requestUri += "&";
          } else {
            requestUri += "?";
          }
          
          requestUri += "message=" + mNotes;
        }
        
        DisplayQrCodeFragment f = new DisplayQrCodeFragment();
        Bundle args = new Bundle();
        args.putString("data", requestUri);
        f.setArguments(args);
        f.show(getFragmentManager(), "qrrequest");
      }
    });
    
    new LoadReceiveAddressTask().execute();

    return view;
  }

  private void setReceiveAddress(String address) {
    
    if(address == null) {
      mReceiveAddress.setText(null);
      mReceiveAddressBarcode.setImageDrawable(null);
      return;
    }
    
    mReceiveAddress.setText(address);
    
    try {
      int size = (int)(getResources().getDisplayMetrics().density * 128);
      Bitmap barcode = Utils.createBarcode("bitcoin:" + address, BarcodeFormat.QR_CODE,
          size, size);
      mReceiveAddressBarcode.setImageBitmap(barcode);
      
    } catch (WriterException e) {
      // Could not generate barcode
      e.printStackTrace();
    }
  }
  
  private void initializeTypeSpinner() {

    ArrayAdapter<TransferType> arrayAdapter = new ArrayAdapter<TransferType>(
        getActivity(), R.layout.fragment_transfer_type, Arrays.asList(TransferType.values())) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(getActivity().getString(TransferType.values()[position].getName()));
        return view;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        view.setText(getActivity().getString(TransferType.values()[position].getName()));
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
  
  private void startTransferTask(TransferType type, String amount, String notes, String toFrom) {
    
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
      JSONObject response = RpcManager.getInstance().callPost(getActivity(), 
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
}
