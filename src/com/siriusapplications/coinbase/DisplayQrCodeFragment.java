package com.siriusapplications.coinbase;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

public class DisplayQrCodeFragment extends DialogFragment {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
    
    String contents = getArguments().getString("data");
    
    Bitmap bitmap;
    try {
      bitmap = Utils.createBarcode(contents, BarcodeFormat.QR_CODE, 512, 512);
    } catch (WriterException e) {
      e.printStackTrace();
      return null;
    }
    
    ImageView view = new ImageView(getActivity());
    view.setImageBitmap(bitmap);
    b.setView(view);
    
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      // Make sure dialog has white background so QR code is legible
      view.setBackgroundColor(Color.WHITE);
    }
    
    b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
    
    return b.create();
  }

}
