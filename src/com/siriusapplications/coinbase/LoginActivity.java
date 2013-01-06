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
  EditText mLoginUsername, mLoginPassword;

  private class LoginTask extends AsyncTask<String, Void, String> {

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
      
      return LoginManager.getInstance().addAccount(LoginActivity.this, params[0], params[1]);
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
    mLoginUsername = (EditText) findViewById(R.id.login_username);
    mLoginPassword = (EditText) findViewById(R.id.login_password);

    mLoginButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        doLogin();
      }
    });

    setUiLoadingState(false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.activity_login, menu);
    return true;
  }

  private void setUiLoadingState(boolean loading) {

    mLoginButton.setEnabled(!loading);
    mLoginUsername.setEnabled(!loading);
    mLoginPassword.setEnabled(!loading);
    setProgressBarIndeterminateVisibility(loading);
  }

  private boolean verifyInput() {

    boolean success = true;

    if("".equals(mLoginUsername.getText().toString().trim())) {

      mLoginUsername.setError(getString(R.string.login_error_username_empty));
      success = false;
    }

    if("".equals(mLoginPassword.getText().toString().trim())) {

      mLoginPassword.setError(getString(R.string.login_error_password_empty));
      success = false;
    }

    return success;
  }

  private void doLogin() {

    if(!verifyInput()) {
      return;
    }

    setUiLoadingState(true);
    mLoginProgress.setVisibility(View.VISIBLE);
    mLoginProgress.setText(R.string.login_progress);
    
    String username = mLoginUsername.getText().toString();
    String password = mLoginPassword.getText().toString();
    
    new LoginTask().execute(username, password);
  }
}
