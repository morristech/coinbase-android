package com.siriusapplications.coinbase;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class Utils {

  private Utils() { }
  
  public static final void showMessageDialog(FragmentManager m, String message) {
    
    MessageDialogFragment fragment = new MessageDialogFragment();
    Bundle args = new Bundle();
    args.putString(MessageDialogFragment.ARG_MESSAGE, message);
    fragment.setArguments(args);
    fragment.show(m, "Utils.showMessageDialog");
  }
}
