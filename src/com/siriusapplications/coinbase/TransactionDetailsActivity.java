package com.siriusapplications.coinbase;

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
      f.setArguments(getIntent().getExtras());

      getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
    }
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    if(item.getItemId() == android.R.id.home) {
      // Action bar up button
      finish();
    }
    
    return false;
  }
}
