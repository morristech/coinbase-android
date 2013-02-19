package com.siriusapplications.coinbase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class TransactionDetailsActivity extends FragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if(savedInstanceState == null) {

      Fragment f = new TransactionDetailsFragment();
      f.setArguments(getIntent().getExtras());

      getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
    }
  }
}
