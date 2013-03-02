package com.siriusapplications.coinbase;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class TransactionDetailsActivity extends SherlockFragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setTitle(R.string.transactiondetails_title);

    if(savedInstanceState == null) {

      Fragment f = new TransactionDetailsFragment();
      
      Bundle args = null;
      if(getIntent().getData() == null) {
        args = getIntent().getExtras();
      } else {
        args = new Bundle();
        args.putParcelable("data", getIntent().getData());
      }
      
      f.setArguments(args);

      getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
    }
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if(item.getItemId() == android.R.id.home) {
      // Action bar up button
      Intent intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
    
    return false;
  }
}
