package com.siriusapplications.coinbase;

import java.util.Arrays;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class TransferFragment extends Fragment {
  
  private enum TransferType {
    SEND(R.string.transfer_send_money),
    REQUEST(R.string.transfer_request_money);
    
    private int mFriendlyName;
    
    private TransferType(int friendlyName) {
      
      mFriendlyName = friendlyName;
    }
    
    public int getName() {
      
      return mFriendlyName;
    }
  }
  
  private Spinner mTransferType;

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_transfer, container, false);
    
    mTransferType = (Spinner) view.findViewById(R.id.transfer_money_type);
    initializeTypeSpinner();
    
    return view;
  }

  private void initializeTypeSpinner() {
    
    ArrayAdapter<TransferType> arrayAdapter = new ArrayAdapter<TransferType>(
        getActivity(), R.layout.fragment_transfer_type, Arrays.asList(TransferType.values())) {

          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setText(getActivity().getString(TransferType.values()[position].getName()));
            return view;
          }

          @Override
          public View getDropDownView(int position, View convertView, ViewGroup parent) {
            
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setText(getActivity().getString(TransferType.values()[position].getName()));
            return view;
          }
    };
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mTransferType.setAdapter(arrayAdapter);
  }
}
