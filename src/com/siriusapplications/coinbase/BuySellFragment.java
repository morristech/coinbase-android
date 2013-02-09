package com.siriusapplications.coinbase;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BuySellFragment extends Fragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_buysell, container, false);
    
    return view;
  }
}
