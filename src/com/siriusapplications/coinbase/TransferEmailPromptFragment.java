package com.siriusapplications.coinbase;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.siriusapplications.coinbase.TransferFragment.TransferType;

public class TransferEmailPromptFragment extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final TransferType type = (TransferType) getArguments().getSerializable("type");
    final String amount = getArguments().getString("amount"),
        notes = getArguments().getString("notes");

    int messageResource = R.string.transfer_email_prompt_text;
    String message = String.format(getString(messageResource), amount);

    View view = View.inflate(getActivity(), R.layout.transfer_email_prompt, null);
    TextView messageView = (TextView) view.findViewById(R.id.transfer_email_prompt_text);
    final EditText field = (EditText) view.findViewById(R.id.transfer_email_prompt_field);

    messageView.setText(message);

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

}
