package com.siriusapplications.coinbase;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class DisplayQrCodeFragment extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
    
//    byte[] data = QRCode.from(getArguments().getString("data")).stream().toByteArray();
//    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//    ImageView view = new ImageView(getActivity());
//    view.setImageBitmap(bitmap);
//    b.setView(view);
    
    b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
    
    return b.create();
  }

}
