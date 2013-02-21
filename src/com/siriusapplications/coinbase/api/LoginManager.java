package com.siriusapplications.coinbase.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;

import com.siriusapplications.coinbase.Constants;
import com.siriusapplications.coinbase.R;
import com.siriusapplications.coinbase.api.RpcManager;
//import com.siriusapplications.coinbase.LoginActivity;

public class LoginManager {

  private static LoginManager INSTANCE = null;

  public static LoginManager getInstance() {

    if(INSTANCE == null) {
      INSTANCE = new LoginManager();
    }

    return INSTANCE;
  }

  // production
  protected static final String CLIENT_ID = "34183b03a3e1f0b74ee6aa8a6150e90125de2d6c1ee4ff7880c2b7e6e98b11f5";
  protected static final String CLIENT_SECRET = "2c481f46f9dc046b4b9a67e630041b9906c023d139fbc77a47053328b9d3122d";
  protected static final String CLIENT_BASEURL = "https://coinbase.com/";
  protected static final String CLIENT_REDIRECT = CLIENT_BASEURL + "/callback?app_scheme=coinbase.android";

  // development (adjust to your setup)
  //protected static final String CLIENT_ID = "b93a59f74763e8fd109c6f895ae8a74b495828d797e48a3e8cd276c6a6dab028";
  //protected static final String CLIENT_SECRET = "72a59bb02e2602232e0d217e4c537bcd4abba3be39ac67298f89bac01f91f2ec";
  //protected static final String CLIENT_BASEURL = "http://192.168.105.20:3000";
  //protected static final String CLIENT_REDIRECT = CLIENT_BASEURL + "/callback?app_scheme=coinbase.android";


  private LoginManager() {

  }

  public boolean isSignedIn(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    return prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1) > -1;
  }

  public String[] getAccounts(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int numAccounts = prefs.getInt(Constants.KEY_MAX_ACCOUNT, -1) + 1;

    List<String> accounts = new ArrayList<String>();
    for(int i = 0; i < numAccounts; i++) {

      String account = prefs.getString(String.format(Constants.KEY_ACCOUNT_NAME, i), null);
      if(account != null) {
        accounts.add(account);
      }
    }

    return accounts.toArray(new String[0]);
  }

  public boolean switchActiveAccount(Context context, int index) {
    return switchActiveAccount(context, index, null);
  }

  public boolean switchActiveAccount(Context context, int index, Editor e) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int numAccounts = prefs.getInt(Constants.KEY_MAX_ACCOUNT, -1) + 1;
    
    if(e == null) {
      e = prefs.edit();
    }
    
    int currentIndex = 0;
    for(int i = 0; i < numAccounts; i++) {

      String account = prefs.getString(String.format(Constants.KEY_ACCOUNT_NAME, i), null);
      if(account != null) {

        if(currentIndex == index) {

          e.putInt(Constants.KEY_ACTIVE_ACCOUNT, i);
          e.commit();
          return true;
        }

        currentIndex++;
      }
    }
    
    e.commit();

    return false;
  }

  public int getSelectedAccountIndex(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int numAccounts = prefs.getInt(Constants.KEY_MAX_ACCOUNT, -1) + 1;
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);

    int id = 0;
    for(int i = 0; i < numAccounts; i++) {

      if(i == activeAccount) {
        return id;
      }

      String account = prefs.getString(String.format(Constants.KEY_ACCOUNT_NAME, i), null);
      if(account != null) {
        id++;
      }
    }

    return -1;
  }

  public String getAccessToken(Context context) {

    if(!isSignedIn(context)) {
      return null;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);

    return prefs.getString(String.format(Constants.KEY_ACCOUNT_ACCESS_TOKEN, activeAccount), null);
  }

  public void refreshAccessToken(Context context) {

    Log.i("Coinbase", "Refreshing access token...");

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    String refreshToken = prefs.getString(String.format(Constants.KEY_ACCOUNT_REFRESH_TOKEN, activeAccount), null);

    List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
    parametersBody.add(new BasicNameValuePair("grant_type", "refresh_token"));
    parametersBody.add(new BasicNameValuePair("refresh_token", refreshToken));

    String[] newTokens;

    try {
      newTokens = doTokenRequest(context, parametersBody);
    } catch(Exception e) {

      e.printStackTrace();
      Log.e("Coinbase", "Could not fetch new access token!");
      return;
    }

    if(newTokens == null) {

      // Authentication error.
      Log.e("Coinbase", "Authentication error when fetching new access token.");
      return;
    }

    Editor e = prefs.edit();

    e.putString(String.format(Constants.KEY_ACCOUNT_ACCESS_TOKEN, activeAccount), newTokens[0]);
    e.putString(String.format(Constants.KEY_ACCOUNT_REFRESH_TOKEN, activeAccount), newTokens[1]);

    e.commit();
  }

  // start three legged oauth handshake
  public String beginOAuthHandshake(Context context){
    String baseUrl = CLIENT_BASEURL + "/oauth/authorize";
    String redirectUrl = null;
    try{
      redirectUrl = URLEncoder.encode(CLIENT_REDIRECT, "utf-8");
    } catch(Exception e) { }
    String authorizeUrl = baseUrl + "?response_type=code&client_id=" + CLIENT_ID + "&redirect_uri=" + redirectUrl;
    Uri authUrl = Uri.parse(authorizeUrl);
    context.startActivity(new Intent(Intent.ACTION_VIEW, authUrl));
    //((LoginActivity)(context)).finish();
    return null;
  }

  // end three legged oauth handshake. (code to tokens)
  public String finishOAuthHandsake(Context context, String code){
    List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
    parametersBody.add(new BasicNameValuePair("grant_type", "authorization_code"));
    parametersBody.add(new BasicNameValuePair("redirect_uri", CLIENT_REDIRECT));
    parametersBody.add(new BasicNameValuePair("code", code));

    try {
      String[] tokens = doTokenRequest(context, parametersBody);

      if(tokens == null) {
        return context.getString(R.string.login_error_auth);
      }

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      Editor e = prefs.edit();

      int accountId = prefs.getInt(Constants.KEY_MAX_ACCOUNT, -1) + 1;
      e.putInt(Constants.KEY_MAX_ACCOUNT, accountId);
      e.putInt(Constants.KEY_ACTIVE_ACCOUNT, accountId);

      e.putString(String.format(Constants.KEY_ACCOUNT_ACCESS_TOKEN, accountId), tokens[0]);
      e.putString(String.format(Constants.KEY_ACCOUNT_REFRESH_TOKEN, accountId), tokens[1]);

      e.commit();

      e.putString(String.format(Constants.KEY_ACCOUNT_NAME, accountId), RpcManager.getInstance().getUsername(context));
      e.commit();

      return null;

    } catch (IOException e) {
      e.printStackTrace();
      return context.getString(R.string.login_error_io);
    } catch (ParseException e) {
      e.printStackTrace();
      return context.getString(R.string.login_error_io);
    } catch (JSONException e) {
      e.printStackTrace();
      return context.getString(R.string.login_error_io);
    }
  }

  private String[] doTokenRequest(Context context, Collection<BasicNameValuePair> params) throws IOException, JSONException {

    DefaultHttpClient client = new DefaultHttpClient();

    String baseUrl = CLIENT_BASEURL + "/oauth/token";

    HttpPost oauthPost = new HttpPost(baseUrl);
    List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
    parametersBody.add(new BasicNameValuePair("client_id", CLIENT_ID));
    parametersBody.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
    parametersBody.addAll(params);
    oauthPost.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));

    HttpResponse response = client.execute(oauthPost);
    int code = response.getStatusLine().getStatusCode();

    if(code == 401) {

      Log.e("Coinbase", "Authentication error.");
      return null;
    } else if(code != 200) {

      throw new IOException("Got HTTP response code " + code);
    }

    JSONObject content = new JSONObject(new JSONTokener(EntityUtils.toString(response.getEntity())));

    String accessToken = content.getString("access_token");
    String refreshToken = content.getString("refresh_token");

    return new String[] { accessToken, refreshToken };
  }

  public String getSelectedAccountName(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int activeAccount = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    return prefs.getString(String.format(Constants.KEY_ACCOUNT_NAME, activeAccount), null);
  }

  public void deleteCurrentAccount(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    Editor e = prefs.edit();

    int accountId = prefs.getInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    e.remove(String.format(Constants.KEY_ACCOUNT_ACCESS_TOKEN, accountId));
    e.remove(String.format(Constants.KEY_ACCOUNT_REFRESH_TOKEN, accountId));
    e.remove(String.format(Constants.KEY_ACCOUNT_NAME, accountId));

    // If there are any other accounts, switch to them
    // Otherwise log out completely
    boolean success = switchActiveAccount(context, 0, e);

    if(!success) {
      e.putInt(Constants.KEY_ACTIVE_ACCOUNT, -1);
    }

    e.commit();
  }
}
