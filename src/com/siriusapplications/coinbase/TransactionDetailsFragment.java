package com.siriusapplications.coinbase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.siriusapplications.coinbase.api.RpcManager;
import com.siriusapplications.coinbase.db.TransactionsDatabase;
import com.siriusapplications.coinbase.db.TransactionsDatabase.TransactionEntry;

public class TransactionDetailsFragment extends Fragment {

  public static final String EXTRA_ID = "id";

  private static enum ActionType {
    RESEND,
    COMPLETE,
    CANCEL;
  }

  private class TakeActionTask extends AsyncTask<Object, Void, String> {

    ProgressDialog mDialog;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();

      mDialog = ProgressDialog.show(getActivity(), null, getString(R.string.transactiondetails_progress));
    }

    @Override
    protected String doInBackground(Object... params) {

      ActionType type = (ActionType) params[0];
      String transactionID = (String) params[1];

      String url = String.format("transactions/%s/", transactionID);

      try {

        JSONObject result = null;

        switch(type) {
        case RESEND:
          result = RpcManager.getInstance().callPut(getActivity(), url + "resend_request", null);
          break;
        case COMPLETE:
          result = RpcManager.getInstance().callPut(getActivity(), url + "complete_request", null);
          break;
        case CANCEL:
          result = RpcManager.getInstance().callDelete(getActivity(), url + "cancel_request", null);
          break;
        }

        boolean success = result.getBoolean("success");

        if(success) {
          return null;
        } else {
          return result.optString("error");
        }
      } catch(Exception e) {
        // An error
        e.printStackTrace();
        return e.getMessage();
      }
    }

    @Override
    protected void onPostExecute(String result) {

      mDialog.dismiss();

      if(getActivity() == null) {
        return;
      }

      if(result != null) {

        Log.i("Coinbase", "Transacation action not successful.");
        Toast.makeText(getActivity(),
            String.format(getActivity().getString(R.string.transactiondetails_action_error), result), Toast.LENGTH_SHORT).show();
      } else {

        // Return to main activity and refresh
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
      }
    }

  }

  @SuppressLint("SimpleDateFormat")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    // Inflate base layout
    ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_transactiondetails, container, false);

    // Get user ID
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String currentUserId = prefs.getString(String.format(Constants.KEY_ACCOUNT_ID, activeAccount), null);

    // Fetch transaction JSON from database
    final String transactionId = getArguments().getString(EXTRA_ID);
    TransactionsDatabase dbHelper = new TransactionsDatabase(getActivity());
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor c = db.query(TransactionEntry.TABLE_NAME, new String[] { TransactionEntry.COLUMN_NAME_JSON },
        TransactionEntry._ID + " = ?", new String[] { transactionId }, null, null, null);
    c.moveToFirst();

    JSONObject data;
    try {
      data = new JSONObject(new JSONTokener(c.getString(c.getColumnIndex(TransactionEntry.COLUMN_NAME_JSON))));

      c.close();

      // Fill views
      TextView amount = (TextView) view.findViewById(R.id.transactiondetails_amount),
          amountLabel = (TextView) view.findViewById(R.id.transactiondetails_label_amount),
          from = (TextView) view.findViewById(R.id.transactiondetails_from),
          to = (TextView) view.findViewById(R.id.transactiondetails_to),
          date = (TextView) view.findViewById(R.id.transactiondetails_date),
          status = (TextView) view.findViewById(R.id.transactiondetails_status),
          notes = (TextView) view.findViewById(R.id.transactiondetails_notes);
      Button resend = (Button) view.findViewById(R.id.transactiondetails_request_resend),
          cancel = (Button) view.findViewById(R.id.transactiondetails_request_cancel),
          send = (Button) view.findViewById(R.id.transactiondetails_request_send),
          decline = (Button) view.findViewById(R.id.transactiondetails_request_decline);

      boolean sentToMe = data.optJSONObject("sender") == null || !currentUserId.equals(data.getJSONObject("sender").optString("id"));

      // Amount
      String amountText = Utils.formatCurrencyAmount(data.getJSONObject("amount").getString("amount"), true) + " " +
          data.getJSONObject("amount").getString("currency");
      amount.setText(amountText);
      amountLabel.setText(sentToMe ? R.string.transactiondetails_amountreceived : R.string.transactiondetails_amountsent);

      // To / From
      String recipient = getName(data.optJSONObject("recipient"), data.optString("recipient_address"), currentUserId);
      from.setText(getName(data.optJSONObject("sender"), null, currentUserId));
      to.setText(recipient);

      // Date
      SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy, 'at' hh:mma zzz");
      try {
        date.setText(dateFormat.format(ISO8601.toCalendar(data.optString("created_at")).getTime()));
      } catch (ParseException e) {
        date.setText(null);
      }

      // Status
      String transactionStatus = data.optString("status", getString(R.string.transaction_status_error));
      String readable = transactionStatus;
      int background = R.drawable.transaction_unknown;
      if("complete".equals(transactionStatus)) {
        readable = getString(R.string.transaction_status_complete);
        background = R.drawable.transaction_complete;
      } else if("pending".equals(transactionStatus)) {
        readable = getString(R.string.transaction_status_pending);
        background = R.drawable.transaction_pending;
      }
      status.setText(readable);
      status.setBackgroundResource(background);

      // Notes
      String notesText = data.optString("notes");
      notes.setText("null".equals(notesText) ? null : notesText);

      // Buttons
      boolean isBuy = data.optJSONObject("sender") != null && "transfers@coinbase.com".equals(data.optJSONObject("sender").optString("email"));
      boolean isSell = data.optJSONObject("recipient") != null && "transfers@coinbase.com".equals(data.optJSONObject("recipient").optString("email"));
      boolean senderOrRecipientIsExternal = data.optJSONObject("sender") == null || data.optJSONObject("recipient") == null;
      if(isBuy || isSell || senderOrRecipientIsExternal || !"pending".equals(transactionStatus)) {
        cancel.setVisibility(View.GONE);
        resend.setVisibility(View.GONE);
        send.setVisibility(View.GONE);
        decline.setVisibility(View.GONE);
      } else if(sentToMe) {

        cancel.setVisibility(View.VISIBLE);
        resend.setVisibility(View.VISIBLE);
        send.setVisibility(View.GONE);
        decline.setVisibility(View.GONE);
      } else {

        cancel.setVisibility(View.GONE);
        resend.setVisibility(View.GONE);
        send.setVisibility(View.VISIBLE);
        decline.setVisibility(View.VISIBLE);

        send.setText(String.format(getString(R.string.transactiondetails_request_send), amountText, recipient));
      }

      cancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          new TakeActionTask().execute(ActionType.CANCEL, transactionId);
        }
      });

      resend.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          new TakeActionTask().execute(ActionType.RESEND, transactionId);
        }
      });

      send.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          new TakeActionTask().execute(ActionType.COMPLETE, transactionId);
        }
      });

      decline.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          new TakeActionTask().execute(ActionType.CANCEL, transactionId);
        }
      });

    } catch (JSONException e) {
      Toast.makeText(getActivity(), R.string.transactiondetails_error, Toast.LENGTH_LONG).show();
      e.printStackTrace();
      getActivity().finish();
    } finally {

      c.close();
      db.close();
    }

    return view;
  }

  private String getName(JSONObject person, String address, String currentUserId) {

    String name = person == null ? null : person.optString("name");
    String email = person == null ? null : person.optString("email");

    if(person != null && currentUserId.equals(person.optString("id"))) {
      return getString(R.string.transaction_user_you);
    }

    if(name != null) {

      String addition = "";

      if(!name.equals(email)) {
        addition = (email == null ? "" : String.format(" (%s)", email));
      }

      return name + addition;
    } else if(email != null) {
      return email;
    } else if(address != null) {
      return address;
    } else {
      return getString(R.string.transaction_user_external);
    }
  }

}
