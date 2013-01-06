package com.siriusapplications.coinbase;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.siriusapplications.coinbase.api.RpcManager;

public class TransactionsFragment extends Fragment {

  private class LoadBalanceTask extends AsyncTask<Void, Void, String[]> {

    @Override
    protected String[] doInBackground(Void... params) {

      try {

        JSONObject result = RpcManager.getInstance().callGet(getActivity(), "account/balance");
        return new String[] { result.getString("amount"), result.getString("currency") };

      } catch (IOException e) {

        e.printStackTrace();
      } catch (JSONException e) {

        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPreExecute() {

      // TODO: Show action bar progress bar
      
      mBalanceText.setTextColor(getResources().getColor(R.color.wallet_balance_color_invalid));
    }

    @Override
    protected void onPostExecute(String[] result) {

      if(result == null) {
        // TODO: Handle errors here.
        mBalanceText.setTextColor(getResources().getColor(R.color.wallet_balance_color_invalid));
      } else {
        mBalanceText.setTextColor(getResources().getColor(R.color.wallet_balance_color));
        mBalanceText.setText(String.format(getActivity().getString(R.string.wallet_balance), Float.parseFloat(result[0])));
        mBalanceCurrency.setText(String.format(getActivity().getString(R.string.wallet_balance_currency), result[1]));
      }
    }

  }
  ListView mListView;
  TextView mBalanceText, mBalanceCurrency;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    
    // Inflate base layout
    View view = inflater.inflate(R.layout.fragment_transactions, container, false);

    mListView = (ListView) view.findViewById(android.R.id.list);
    
    // Inflate list header (which contains account balance)
    View headerView = inflater.inflate(R.layout.fragment_transactions_header, null, false);
    mListView.addHeaderView(headerView);
    
    mBalanceText = (TextView) headerView.findViewById(R.id.wallet_balance);
    mBalanceCurrency = (TextView) headerView.findViewById(R.id.wallet_balance_currency);

    return view;
  }

  @Override
  public void onResume() {

    super.onResume();

    // Reload balance
    new LoadBalanceTask().execute();
  }

}
