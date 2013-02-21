package com.siriusapplications.coinbase;

import com.siriusapplications.coinbase.api.LoginManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class AccountsFragment extends DialogFragment {

  int selected = -1;
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.accounts_title);
    
    final String[] accounts = LoginManager.getInstance().getAccounts(getActivity());
    selected = LoginManager.getInstance().getSelectedAccountIndex(getActivity());
    
    String[] items = new String[accounts.length + 1];
    System.arraycopy(accounts, 0, items, 0, accounts.length);
    items[accounts.length] = getActivity().getString(R.string.accounts_new);
    
    builder.setSingleChoiceItems(items, 
        selected, new DialogInterface.OnClickListener() {
      
      public void onClick(DialogInterface dialog, int which) {
        
        if(which == accounts.length) {
          // New account
          MainActivity activity = (MainActivity) getActivity();
          activity.addAccount();
          dialog.dismiss();
          return;
        }
       
        selected = which;
      }
    })
    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        
        // Change accounts
        MainActivity activity = (MainActivity) getActivity();
        activity.changeAccount(selected);
      }
    })
    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        // User cancelled the dialog
      }
    });
    return builder.create();
  }
}
