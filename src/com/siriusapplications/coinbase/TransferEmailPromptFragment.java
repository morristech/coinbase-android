package com.siriusapplications.coinbase;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.siriusapplications.coinbase.TransferFragment.TransferType;

public class TransferEmailPromptFragment extends DialogFragment {
  
  private SimpleCursorAdapter mAutocompleteAdapter;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final TransferType type = (TransferType) getArguments().getSerializable("type");
    final String amount = getArguments().getString("amount"),
        notes = getArguments().getString("notes");

    int messageResource = R.string.transfer_email_prompt_text;
    String message = String.format(getString(messageResource), amount);

    View view = View.inflate(getActivity(), R.layout.transfer_email_prompt, null);
    TextView messageView = (TextView) view.findViewById(R.id.transfer_email_prompt_text);
    final AutoCompleteTextView field = (AutoCompleteTextView) view.findViewById(R.id.transfer_email_prompt_field);

    mAutocompleteAdapter = Utils.getEmailAutocompleteAdapter(getActivity());
    field.setAdapter(mAutocompleteAdapter);
    field.setThreshold(0);
    
    messageView.setText(message);
    
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      messageView.setTextColor(Color.WHITE);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {

        // Complete transfer
        TransferFragment parent = getActivity() == null ? null : ((MainActivity) getActivity()).getTransferFragment();

        if(parent != null) {
          parent.startTransferTask(type, amount, notes, field.getText().toString());
        }
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        // User cancelled the dialog
      }
    });

    return builder.create();
  }

  @Override
  public void onStop() {
    super.onStop();
    
    Utils.disposeOfEmailAutocompleteAdapter(mAutocompleteAdapter);
  }

}
