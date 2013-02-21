package com.siriusapplications.coinbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.siriusapplications.coinbase.api.RpcManager;

public class BuySellFragment extends Fragment {

  private enum BuySellType {
    BUY(R.string.buysell_type_buy, "buy"),
    SELL(R.string.buysell_type_sell, "sell");

    private int mFriendlyName;
    private String mRequestType;

    private BuySellType(int friendlyName, String requestType) {

      mFriendlyName = friendlyName;
      mRequestType = requestType;
    }

    public String getRequestType() {

      return mRequestType;
    }

    public int getName() {

      return mFriendlyName;
    }
  }

  public static class ConfirmBuySellDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      final BuySellType type = (BuySellType) getArguments().getSerializable("type");
      final String amount1 = getArguments().getString("amount1"),
          amount2 = getArguments().getString("amount2"),
          amount2currency = getArguments().getString("amount2currency");

      int messageResource = type == BuySellType.BUY ? R.string.buysell_confirm_message_buy : R.string.buysell_confirm_message_sell;
      String message = String.format(getString(messageResource), amount1, amount2, amount2currency);

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(message)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {

          // Buy / sell!
          BuySellFragment parent = getActivity() == null ? null : ((MainActivity) getActivity()).getBuySellFragment();

          if(parent != null) {
            parent.startBuySellTask(type, amount1);
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

  private class DoBuySellTask extends AsyncTask<Object, Void, Object[]> {

    private ProgressDialog mDialog;

    @Override
    protected void onPreExecute() {

      super.onPreExecute();
      mDialog = ProgressDialog.show(mParent, null, getString(R.string.buysell_progress));
    }

    protected Object[] doInBackground(Object... params) {

      return doBuySell((BuySellType) params[0], (String) params[1]);
    }

    protected void onPostExecute(Object[] result) {

      mDialog.dismiss();

      boolean success = (Boolean) result[0];
      if(success) {

        BuySellType type = (BuySellType) result[2];

        int messageId = type == BuySellType.BUY ? R.string.buysell_success_buy : R.string.buysell_success_sell;
        String text = String.format(getString(messageId), (String) result[1]);
        Toast.makeText(mParent, text, Toast.LENGTH_SHORT).show();
        
        // Sync transactions
        mParent.refresh();
      } else {

        Utils.showMessageDialog(getFragmentManager(), (String) result[1]);
      }
    }
  }

  private class UpdatePriceTask extends AsyncTask<String, Void, String[]> {

    @Override
    protected void onPreExecute() {

      super.onPreExecute();
      mTotal.setText(null);
      mSubmitButton.setEnabled(false);
    }

    protected String[] doInBackground(String... params) {

      try {

        String amount = params[0], type = params[1];

        if(amount.isEmpty()) {
          return new String[] { null };
        }

        Collection<BasicNameValuePair> requestParams = new ArrayList<BasicNameValuePair>();
        requestParams.add(new BasicNameValuePair("qty", amount));

        JSONObject result = RpcManager.getInstance().callGet(mParent, "prices/" + type, requestParams);
        return new String[] { result.getString("amount"), result.getString("currency") };

      } catch (IOException e) {

        e.printStackTrace();
      } catch (JSONException e) {

        e.printStackTrace();
      }

      return null;
    }

    protected void onPostExecute(String[] result) {

      if(mTotal != null) {

        if(result == null) {

          mTotal.setText(R.string.buysell_total_error);
        } else {

          if(result[0] == null) {
            mTotal.setText(null);
          } else {

            mCurrentPrice = result[0];
            mCurrentPriceCurrency = result[1];
            mSubmitButton.setEnabled(true);
            mTotal.setText(String.format(getString(R.string.buysell_total), result[0], result[1]));
            return;
          }
        }
      }

      mCurrentPrice = null;
      mSubmitButton.setEnabled(false);
    }
  }

  private MainActivity mParent;
  
  private UpdatePriceTask mUpdatePriceTask;
  private String mCurrentPrice, mCurrentPriceCurrency;

  private Spinner mBuySellSpinner;
  private TextView mTypeText, mTotal;
  private Button mSubmitButton;
  private EditText mAmount;

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
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_buysell, container, false);

    mBuySellSpinner = (Spinner) view.findViewById(R.id.buysell_type);
    mBuySellSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {

        onTypeChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        // Will never happen.
        throw new RuntimeException("onNothingSelected triggered on buy / sell spinner");
      }
    });
    initializeTypeSpinner();

    mTypeText = (TextView) view.findViewById(R.id.buysell_type_text);
    mTotal = (TextView) view.findViewById(R.id.buysell_total);

    mSubmitButton = (Button) view.findViewById(R.id.buysell_submit);
    mSubmitButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        ConfirmBuySellDialogFragment dialog = new ConfirmBuySellDialogFragment();

        Bundle b = new Bundle();

        BuySellType type = BuySellType.values()[mBuySellSpinner.getSelectedItemPosition()];
        b.putSerializable("type", type);

        b.putString("amount1", mAmount.getText().toString());
        b.putString("amount2", mCurrentPrice);
        b.putString("amount2currency", mCurrentPriceCurrency);

        dialog.setArguments(b);

        dialog.show(getFragmentManager(), "confirm");
      }
    });

    mAmount = (EditText) view.findViewById(R.id.buysell_amount);
    mAmount.addTextChangedListener(new TextWatcher() {

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
          int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

        updatePrice();
      }
    });

    return view;
  }

  private void onTypeChanged() {

    BuySellType type = BuySellType.values()[mBuySellSpinner.getSelectedItemPosition()];

    int typeText = type == BuySellType.BUY ? R.string.buysell_type_buy_text : R.string.buysell_type_sell_text;
    mTypeText.setText(typeText);

    int submitLabel = type == BuySellType.BUY ? R.string.buysell_submit_buy : R.string.buysell_submit_sell;
    mSubmitButton.setText(submitLabel);

    updatePrice();
  }

  private void updatePrice() {

    BuySellType type = BuySellType.values()[mBuySellSpinner.getSelectedItemPosition()];

    if(mUpdatePriceTask != null) {

      mUpdatePriceTask.cancel(true);
    }

    mUpdatePriceTask = new UpdatePriceTask();
    mUpdatePriceTask.execute(mAmount.getText().toString(), type.getRequestType());
  }

  private void initializeTypeSpinner() {

    ArrayAdapter<BuySellType> arrayAdapter = new ArrayAdapter<BuySellType>(
        mParent, R.layout.fragment_transfer_type, Arrays.asList(BuySellType.values())) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(mParent.getString(BuySellType.values()[position].getName()));
        return view;
      }

      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {

        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        view.setText(mParent.getString(BuySellType.values()[position].getName()));
        return view;
      }
    };
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mBuySellSpinner.setAdapter(arrayAdapter);
  }

  protected void startBuySellTask(BuySellType type, String amount) {

    new DoBuySellTask().execute(type, amount);
  }

  private Object[] doBuySell(BuySellType type, String amount) {

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("qty", amount));

    try {
      JSONObject response = RpcManager.getInstance().callPost(mParent, type.getRequestType() + "s", params);

      boolean success = response.getBoolean("success");

      if(success) {

        return new Object[] { true, amount, type };
      } else {

        String errorMessage = response.getJSONArray("errors").optString(0);
        return new Object[] { false, String.format(getString(R.string.buysell_error_api), errorMessage) };
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // There was an exception
    return new Object[] { false, getString(R.string.buysell_error_exception) };
  }

  public void refresh() {
    // Nothing to refresh in this fragment
  }
}
