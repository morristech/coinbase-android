package com.siriusapplications.coinbase;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.siriusapplications.coinbase.api.LoginManager;

public class LoginActivity extends CoinbaseActivity {

  private static final String REDIRECT_URL = "http://example.com/coinbase-redirect";

  Button mLoginButton;
  WebView mLoginWebView;
  RelativeLayout mLoginForm;

  private class OAuthCodeTask extends AsyncTask<String, Void, String> {
    
    ProgressDialog mDialog;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      mDialog = ProgressDialog.show(LoginActivity.this, null, getString(R.string.login_progress));
    }

    @Override
    protected String doInBackground(String... params) {
      return LoginManager.getInstance().addAccountOAuth(LoginActivity.this, params[0], REDIRECT_URL);
    }

    protected void onPostExecute(String result) {
      
      mDialog.dismiss();
      
      if(result == null) {
        // Success!
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
      } else {
        // Failure.
        Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
    setContentView(R.layout.activity_login);
    setProgressBarIndeterminateVisibility(false); 

    mLoginButton = (Button) findViewById(R.id.login_button);
    mLoginWebView = (WebView) findViewById(R.id.login_webview);
    mLoginForm = (RelativeLayout) findViewById(R.id.login_form);

    mLoginButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        doLogin();
      }
    });
    
    // Load authorization URL before user clicks on the sign in button so that it loads quicker
    mLoginWebView.getSettings().setJavaScriptEnabled(true);
    mLoginWebView.getSettings().setSavePassword(false);
    
    // Clear cookies so that user is not already logged in if they want to add a new account
    CookieSyncManager.createInstance(this); 
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.removeAllCookie();
    
    mLoginWebView.loadUrl(LoginManager.getInstance().generateOAuthUrl(REDIRECT_URL));
    
    mLoginWebView.setWebViewClient(new WebViewClient() {
      
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        
        if(url.startsWith(REDIRECT_URL)) {
          // OAuth redirect - we will handle this.
          String oauthCode = Uri.parse(url).getQueryParameter("code");
          new OAuthCodeTask().execute(oauthCode);
          setLayoutMode(false); // Switch back to the login form.
          return true;
        } else if(!url.contains("oauth") && !url.contains("signin") && !url.contains("signup")) {
          
          // Do not allow leaving the login page.
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
          return true;
        }
        
        return false;
      }
    });

    onNewIntent(getIntent());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.activity_login, menu);
    return true;
  }
  
  /** Sets the login form or webview visible. */
  private void setLayoutMode(boolean webMode) {
    
    mLoginWebView.setVisibility(webMode ? View.VISIBLE : View.GONE);
    mLoginForm.setVisibility(webMode ? View.GONE : View.VISIBLE);
  }

  private void doLogin() {
    
    // Switch to WebView layout
    setLayoutMode(true);
  }
}
