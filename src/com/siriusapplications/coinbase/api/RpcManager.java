package com.siriusapplications.coinbase.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.util.Log;


public class RpcManager {

  private static RpcManager INSTANCE = null;

  public static RpcManager getInstance() {

    if(INSTANCE == null) {
      INSTANCE = new RpcManager();
    }

    return INSTANCE;
  }
  
  private static enum RequestVerb {
    GET,
    POST,
    PUT,
    DELETE;
  }

  private RpcManager() {

  }
  
  private static final String BASE_URL = LoginManager.CLIENT_BASEURL + "/api/v1/";

  public JSONObject callGet(Context context, String method) throws IOException, JSONException {

    return call(context, method, RequestVerb.GET, null, true);
  }

  public JSONObject callGet(Context context, String method, Collection<BasicNameValuePair> params) throws IOException, JSONException {

    return call(context, method, RequestVerb.GET, params, true);
  }
  
  public JSONObject callPost(Context context, String method, Collection<BasicNameValuePair> params) throws IOException, JSONException {
    
    return call(context, method, RequestVerb.POST, params, true);
  }
  
  public JSONObject callPut(Context context, String method, Collection<BasicNameValuePair> params) throws IOException, JSONException {
    
    return call(context, method, RequestVerb.PUT, params, true);
  }
  
  public JSONObject callDelete(Context context, String method, Collection<BasicNameValuePair> params) throws IOException, JSONException {
    
    return call(context, method, RequestVerb.DELETE, params, true);
  }

  private JSONObject call(Context context, String method, RequestVerb verb, Collection<BasicNameValuePair> params, boolean retry) throws IOException, JSONException {

    DefaultHttpClient client = new DefaultHttpClient();

    String url = BASE_URL + method;

    HttpUriRequest request = null;

    if(verb == RequestVerb.POST || verb == RequestVerb.PUT || verb == RequestVerb.DELETE) {
      
      // Post body is used.
      switch(verb) {
      case POST:
        request = new HttpPost(url);
        break;
      case PUT:
        request = new HttpPut(url);
        break;
      case DELETE:
        request = new HttpDelete(url);
        break;
      default:
        throw new RuntimeException("RequestVerb not implemented: " + verb);
      }
      
      List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
      
      if(params != null) {
        parametersBody.addAll(params);
      }
      
      ((HttpEntityEnclosingRequestBase) request).setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));
    } else {
      
      // URL parameters are used.
      if(params != null) {
        List<BasicNameValuePair> paramsList = (params instanceof List<?>) ? (List<BasicNameValuePair>)params : new ArrayList<BasicNameValuePair>(params);
        url = url + "?" + URLEncodedUtils.format(paramsList, "UTF-8");
      }
      
      request = new HttpGet(url);
    }
    
    String accessToken = LoginManager.getInstance().getAccessToken(context);
    request.addHeader("Authorization", String.format("Bearer %s", accessToken));

    HttpResponse response = client.execute(request);
    int code = response.getStatusLine().getStatusCode();

    if(code == 401 && retry) {
      
      // Authentication error.
      // This may be caused by an outdated access token - attempt to fetch a new one
      // before failing.
      LoginManager.getInstance().refreshAccessToken(context);
      return call(context, method, verb, params, false);
    } else if(code != 200) {

      throw new IOException("HTTP response " + code + " to request " + method);
    }

    JSONObject content = new JSONObject(new JSONTokener(EntityUtils.toString(response.getEntity())));
    return content;
  }

  public String getUsername(Context context){
    JSONObject response;
    try {
      response = callGet(context, "transactions");
      return response.getJSONObject("current_user").getString("email");
    } catch (IOException e) {
      Log.e("Coinbase", "I/O error refreshing transactions.");
      e.printStackTrace();
      return null;
    } catch (JSONException e) {
      // Malformed response from Coinbase.
      Log.e("Coinbase", "Could not parse JSON response from Coinbase, aborting refresh of transactions.");
      e.printStackTrace();
      return null;
    }
  }
}
