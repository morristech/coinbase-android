package com.siriusapplications.coinbase;

import java.math.BigDecimal;
import java.text.DecimalFormat;

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
  
  public static final String formatCurrencyAmount(String amount) {
    return formatCurrencyAmount(amount, false);
  }
  
  public static final String formatCurrencyAmount(String amount, boolean ignoreSign) {
    
    BigDecimal balanceNumber = new BigDecimal(amount);
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(4);
    df.setMinimumFractionDigits(4);
    df.setGroupingUsed(false);
    
    if(ignoreSign && balanceNumber.compareTo(BigDecimal.ZERO) == -1) {
      balanceNumber = balanceNumber.multiply(new BigDecimal(-1));
    }
    
    return df.format(balanceNumber);
  }
}
