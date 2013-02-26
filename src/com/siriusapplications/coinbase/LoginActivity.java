package com.siriusapplications.coinbase;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.siriusapplications.coinbase.api.LoginManager;

public class LoginActivity extends CoinbaseActivity {

  TextView mLoginProgress;
  Button mLoginButton;

  private class OAuthCodeTask extends AsyncTask<String, Void, String> {

    protected void onPostExecute(String result) {
      setUiLoadingState(false);
      
      if(result == null) {
        // Success!
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
      } else {
        // Failure.
        mLoginProgress.setText(result);
      }
    }

    @Override
    protected String doInBackground(String... params) {
      return LoginManager.getInstance().finishOAuthHandsake(LoginActivity.this, params[0]);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
    setContentView(R.layout.activity_login);
    setProgressBarIndeterminateVisibility(false); 

    mLoginButton = (Button) findViewById(R.id.login_button);
    mLoginProgress = (TextView) findViewById(R.id.login_progress_text);

    mLoginButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        doLogin();
      }
    });

    setUiLoadingState(false);

    onNewIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {

    super.onNewIntent(intent);
    setIntent(intent);

    if(intent.getData() != null && "x-com.coinbase.android".equals(intent.getData().getScheme())) {
      String oauthCode = intent.getData().getQueryParameter("code");
      new OAuthCodeTask().execute(oauthCode);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.activity_login, menu);
    return true;
  }

  private void setUiLoadingState(boolean loading) {

    mLoginButton.setEnabled(!loading);
    setProgressBarIndeterminateVisibility(loading);
  }

  private void doLogin() {

    setUiLoadingState(true);
    mLoginProgress.setVisibility(View.VISIBLE);
    mLoginProgress.setText(R.string.login_progress);
    
    LoginManager.getInstance().beginOAuthHandshake(LoginActivity.this);
    finish();
  }
}
