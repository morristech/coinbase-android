package com.siriusapplications.coinbase;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Hashtable;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

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

  /** Based off of ZXing Android client code */
  public static Bitmap createBarcode(String contents, BarcodeFormat format,
      int desiredWidth, int desiredHeight) throws WriterException {

    Hashtable<EncodeHintType,Object> hints = new Hashtable<EncodeHintType,Object>(2);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    MultiFormatWriter writer = new MultiFormatWriter();    
    BitMatrix result = writer.encode(contents, format, desiredWidth, desiredHeight, hints);
    
    int width = result.getWidth();
    int height = result.getHeight();
    int fgColor = 0xFF000000;
    int bgColor = 0x00FFFFFF;
    int[] pixels = new int[width * height];
    
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        pixels[offset + x] = result.get(x, y) ? fgColor : bgColor;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }
}
