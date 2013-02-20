package com.siriusapplications.coinbase;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

import android.net.Uri;
import android.os.Bundle;
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
import android.widget.Spinner;
import android.widget.TextView;

public class TransferFragment extends Fragment {

  private enum TransferType {
    SEND(R.string.transfer_send_money),
    REQUEST(R.string.transfer_request_money);

    private int mFriendlyName;

    private TransferType(int friendlyName) {

      mFriendlyName = friendlyName;
    }

    public int getName() {

      return mFriendlyName;
    }
  }

  private Spinner mTransferTypeView;
  private Button mSubmitSend, mSubmitEmail, mSubmitQr, mSubmitNfc;
  private EditText mAmountView, mNotesView, mRecipientView;

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

    return view;
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
