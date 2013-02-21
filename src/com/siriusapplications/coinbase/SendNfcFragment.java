package com.siriusapplications.coinbase;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SendNfcFragment extends DialogFragment {
  
  private static final boolean IS_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
    
    boolean nfcSuppored = IS_SUPPORTED && NfcAdapter.getDefaultAdapter(getActivity()) != null;

    b.setMessage(nfcSuppored ? R.string.transfer_nfc_ready : R.string.transfer_nfc_failure);

    b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });

    return b.create();
  }
  
  @Override
  public void onStart() {
    super.onStart();
    
    if(IS_SUPPORTED) {
      startNfc(getArguments().getString("data"));
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    
    if(IS_SUPPORTED) {
      stopNfc();
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private void startNfc(String uri) {

    if(getActivity() != null && NfcAdapter.getDefaultAdapter(getActivity()) != null) {
      
      NdefMessage message = new NdefMessage(new NdefRecord[] { NdefRecord.createUri(uri) });
      NfcAdapter.getDefaultAdapter(getActivity()).setNdefPushMessage(message, getActivity());
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private void stopNfc() {

    if(getActivity() != null && NfcAdapter.getDefaultAdapter(getActivity()) != null) {
      NfcAdapter.getDefaultAdapter(getActivity()).setNdefPushMessage(null, getActivity());
    }
  }
}
