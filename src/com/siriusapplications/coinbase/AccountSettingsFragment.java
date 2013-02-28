package com.siriusapplications.coinbase;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.siriusapplications.coinbase.api.RpcManager;

public class AccountSettingsFragment extends Fragment {

  private class RefreshSettingsTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... params) {

      try {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mParent);
        int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
        JSONObject userInfo = RpcManager.getInstance().callGet(mParent, "users").getJSONArray("users").getJSONObject(0).getJSONObject("user");

        Editor e = prefs.edit();

        e.putString(String.format(Constants.KEY_ACCOUNT_NAME, activeAccount), userInfo.getString("email"));
        e.putString(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount), userInfo.getString("native_currency"));
        e.putString(String.format(Constants.KEY_ACCOUNT_FULL_NAME, activeAccount), userInfo.getString("name"));
        e.putString(String.format(Constants.KEY_ACCOUNT_TIME_ZONE, activeAccount), userInfo.getString("time_zone"));

        e.commit(); 
        
        return true;
      } catch (JSONException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }

      return false;
    }

  }

  MainActivity mParent;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    mParent = (MainActivity) activity;
  }

  public void setParent(MainActivity parent) {

    mParent = parent;
  }

  public void refresh() {

    new RefreshSettingsTask().execute();
  }
}
