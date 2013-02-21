package com.siriusapplications.coinbase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.content.Intent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.siriusapplications.coinbase.api.LoginManager;

public class CoinbaseActivity extends SherlockFragmentActivity {

  /** This activity requires authentication */
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface RequiresAuthentication { }

  @Override
  public void onResume() {

    super.onResume();

    if(getClass().isAnnotationPresent(RequiresAuthentication.class)) {
      // Check authentication status
      if(!LoginManager.getInstance().isSignedIn(this)) {

        // Not signed in.
        // First check if there are any accounts available to sign in to:
        boolean success = LoginManager.getInstance().switchActiveAccount(this, 0);

        if(!success) {
          // Not signed in - open login activity.
          Intent intent = new Intent(this, LoginActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);

          finish();
        } else {
          // Now signed in, continue with Activity initialization
        }
      }
    }
  }
}
