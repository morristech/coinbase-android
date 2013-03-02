package com.siriusapplications.coinbase;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.client.android.Intents;
import com.justinschultz.pusherclient.Pusher;
import com.siriusapplications.coinbase.CoinbaseActivity.RequiresAuthentication;
import com.siriusapplications.coinbase.api.LoginManager;
import com.siriusapplications.coinbase.pusher.CoinbasePusherListener;
import com.slidingmenu.lib.SlidingMenu;

@RequiresAuthentication
public class MainActivity extends CoinbaseActivity {

  public static final String ACTION_SCAN = "com.siriusapplications.coinbase.MainActivity.ACTION_SCAN";
  public static final String ACTION_TRANSFER = "com.siriusapplications.coinbase.MainActivity.ACTION_TRANSFER";
  public static final String ACTION_TRANSACTIONS = "com.siriusapplications.coinbase.MainActivity.ACTION_TRANSACTIONS";

  public static class SignOutFragment extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.sign_out_confirm);

      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // Sign out
          ((MainActivity) getActivity()).changeAccount(-1);
        }
      });

      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // Dismiss
        }
      });

      return builder.create();
    }
  }
  
  private enum SlidingMenuMode {
    NORMAL,
    PINNED,
    FAKE_GINGERBREAD_COMPAT;
  }
  
  private static final int FRAGMENT_INDEX_TRANSACTIONS = 0;
  private static final int FRAGMENT_INDEX_TRANSFER = 1;
  private static final int FRAGMENT_INDEX_BUYSELL = 2;
  private static final int FRAGMENT_INDEX_ACCOUNT = 3;

  private int[] mFragmentTitles = new int[] {
      R.string.title_transactions,
      R.string.title_transfer,
      R.string.title_buysell,
      R.string.title_account,
  };
  private int[] mFragmentIcons = new int[] {
      R.drawable.ic_action_transactions,
      R.drawable.ic_action_transfer,
      R.drawable.ic_action_buysell,
      R.drawable.ic_action_account
  };

  SectionsPagerAdapter mSectionsPagerAdapter;
  CustomViewPager mViewPager;
  TransactionsFragment mTransactionsFragment;
  BuySellFragment mBuySellFragment;
  TransferFragment mTransferFragment;
  AccountSettingsFragment mSettingsFragment;
  SlidingMenu mSlidingMenu;
  Pusher mPusher;
  MenuItem mRefreshItem;
  boolean mRefreshItemState = false;
  SlidingMenuMode mSlidingMenuMode = SlidingMenuMode.NORMAL;
  boolean mSlidingMenuCompatShowing = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      mSlidingMenuMode = SlidingMenuMode.FAKE_GINGERBREAD_COMPAT;
    } else {
      mSlidingMenuMode = 
          getResources().getBoolean(R.bool.pin_sliding_menu) ? SlidingMenuMode.PINNED : SlidingMenuMode.NORMAL;
    }

    mTransactionsFragment = new TransactionsFragment();
    mBuySellFragment = new BuySellFragment();
    mTransferFragment = new TransferFragment();
    mSettingsFragment = new AccountSettingsFragment();

    mTransactionsFragment.setParent(this);
    mBuySellFragment.setParent(this);
    mTransferFragment.setParent(this);
    mSettingsFragment.setParent(this);

    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager
    mViewPager = (CustomViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setPagingEnabled(false);
    mViewPager.setOffscreenPageLimit(mFragmentTitles.length); // Keep all fragment views initialized, for smooth scrolling

    // configure the SlidingMenu
    mSlidingMenu = new SlidingMenu(this);
    mSlidingMenu.setMode(SlidingMenu.LEFT);
    mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
    mSlidingMenu.setShadowWidthRes(R.dimen.main_menu_shadow_width);
    mSlidingMenu.setShadowDrawable(R.drawable.main_menu_shadow);
    mSlidingMenu.setBehindWidthRes(R.dimen.main_menu_width);
    mSlidingMenu.setFadeDegree(0f);
    mSlidingMenu.setBehindScrollScale(0);
    mSlidingMenu.setSlidingEnabled(mSlidingMenuMode == SlidingMenuMode.NORMAL);

    if(mSlidingMenuMode != SlidingMenuMode.FAKE_GINGERBREAD_COMPAT) {

      mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
    }
    
    ListView slidingList;
    if(mSlidingMenuMode == SlidingMenuMode.NORMAL) {

      findViewById(android.R.id.list).setVisibility(View.GONE);
      mSlidingMenu.setMenu(R.layout.activity_main_menu);
      slidingList = (ListView) mSlidingMenu.findViewById(android.R.id.list);
    } else {
      slidingList = (ListView) findViewById(android.R.id.list);
      
      if(mSlidingMenuMode == SlidingMenuMode.FAKE_GINGERBREAD_COMPAT) {
        findViewById(R.id.main_gingerbread_compat_overlay).setVisibility(View.GONE);
        findViewById(R.id.main_gingerbread_compat_overlay).setOnClickListener(new View.OnClickListener() {
          
          @Override
          public void onClick(View v) {
            // Close sliding menu
            hideSlidingMenu();
          }
        });
      }
    }

    mSlidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {

      @Override
      public void onClose() {
        updateTitle();
      }
    });

    mSlidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {

      @Override
      public void onOpen() {
        updateTitle();
      }
    });

    // Set up Sliding Menu list
    slidingList.setAdapter(new SectionsListAdapter());
    slidingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
          long arg3) {

        switchTo(arg2);
      }
    });

    // Refresh everything on app launch
    new Thread(new Runnable() {
      public void run() {
        runOnUiThread(new Runnable() {
          public void run() {
            refresh();
          }
        });
      }
    }).start();

    getSupportActionBar().setDisplayHomeAsUpEnabled(!(mSlidingMenuMode == SlidingMenuMode.PINNED));
    switchTo(0);

    onNewIntent(getIntent());
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    
    // Update title, in case restoring the instance state has changed the current fragment
    updateTitle();
  }

  private void switchTo(int index) {


    mViewPager.setCurrentItem(index, false);
    updateTitle();

    hideSlidingMenu();
  }
  
  private boolean isSlidingMenuShowing() {
    
    if(mSlidingMenuMode == SlidingMenuMode.FAKE_GINGERBREAD_COMPAT) {
      return mSlidingMenuCompatShowing;
    } else {
      return mSlidingMenu.isMenuShowing();
    }
  }
  
  private void showSlidingMenu() {
    
    if(mSlidingMenuMode == SlidingMenuMode.FAKE_GINGERBREAD_COMPAT) {
      findViewById(R.id.main_gingerbread_compat_overlay).setVisibility(View.VISIBLE);
      findViewById(R.id.main_gingerbread_compat_overlay).bringToFront();
      mSlidingMenuCompatShowing = true;
      updateTitle();
    } else {
      mSlidingMenu.showMenu();
    }
  }
  
  private void hideSlidingMenu() {
    
    if(mSlidingMenuMode == SlidingMenuMode.FAKE_GINGERBREAD_COMPAT) {
      findViewById(R.id.main_gingerbread_compat_overlay).setVisibility(View.GONE);
      mSlidingMenuCompatShowing = false;
      updateTitle();
    } else {
      if(mSlidingMenu != null) {
        mSlidingMenu.showContent();
      }
    }
  }

  private void updateTitle() {
    
    getSupportActionBar().setDisplayHomeAsUpEnabled(mSlidingMenuMode != SlidingMenuMode.PINNED &&
        !isSlidingMenuShowing());

    if((mSlidingMenu != null && isSlidingMenuShowing()) || mSlidingMenuMode == SlidingMenuMode.PINNED) {
      getSupportActionBar().setTitle(R.string.app_name);
    } else {
      getSupportActionBar().setTitle(mFragmentTitles[mViewPager.getCurrentItem()]);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    // Screen width may have changed so this needs to be set again
    mSlidingMenu.setBehindWidthRes(R.dimen.main_menu_width);
  }

  @Override
  protected void onNewIntent(Intent intent) {

    super.onNewIntent(intent);
    setIntent(intent);

    if(intent.getData() != null && "bitcoin".equals(intent.getData().getScheme())) {
      // Handle bitcoin: URI
      switchTo(FRAGMENT_INDEX_TRANSFER);
      mTransferFragment.fillFormForBitcoinUri(getIntent().getData());
    } else if(ACTION_SCAN.equals(intent.getAction())) {
      // Scan barcode
      startBarcodeScan();
    } else if(ACTION_TRANSFER.equals(intent.getAction())) {

      switchTo(FRAGMENT_INDEX_TRANSFER);
    } else if(ACTION_TRANSACTIONS.equals(intent.getAction())) {

      switchTo(FRAGMENT_INDEX_TRANSACTIONS);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.activity_main, menu);
    mRefreshItem = menu.findItem(R.id.menu_refresh);
    setRefreshButtonAnimated(mRefreshItemState);
    return true;
  }

  @Override
  public void onResume() {
    super.onResume();

    // Connect to pusher
    new Thread(new Runnable() {
      public void run() {

        // mPusher = new Pusher(CoinbasePusherListener.API_KEY);
        // mPusher.setPusherListener(new CoinbasePusherListener(mPusher, MainActivity.this));
        // mPusher.connect();
      }
    }).start();
  }

  @Override
  protected void onPause() {
    super.onPause();

    if(mPusher != null) {
      mPusher.disconnect();
      mPusher = null;
    }
  }

  public void openTransferMenu(boolean isRequest) {

    switchTo(FRAGMENT_INDEX_TRANSFER);
    mTransferFragment.switchType(isRequest);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch(item.getItemId()) {
    case R.id.menu_accounts:
      new AccountsFragment().show(getSupportFragmentManager(), "accounts");
      return true;
    case R.id.menu_sign_out:
      new SignOutFragment().show(getSupportFragmentManager(), "signOut");
      return true;
    case R.id.menu_about:
      startActivity(new Intent(this, AboutActivity.class));
      return true;
    case R.id.menu_barcode:
      startBarcodeScan();
      return true;
    case R.id.menu_refresh:
      refresh();
      return true;
    case android.R.id.home:
      showSlidingMenu();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
   * sections of the app.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      switch (i) {
      case FRAGMENT_INDEX_TRANSACTIONS: return mTransactionsFragment;
      case FRAGMENT_INDEX_TRANSFER: return mTransferFragment;
      case FRAGMENT_INDEX_BUYSELL: return mBuySellFragment;
      case FRAGMENT_INDEX_ACCOUNT: return mSettingsFragment;
      }
      return null;
    }

    @Override
    public int getCount() {
      return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return getString(mFragmentTitles[position]).toUpperCase(Locale.CANADA);
    }
  }

  public class SectionsListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return 4;
    }

    @Override
    public Object getItem(int position) {
      return position;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      if(convertView == null) {
        convertView = View.inflate(MainActivity.this, R.layout.activity_main_menu_item, null);
      }

      String name = getString(mFragmentTitles[position]);

      ((TextView) convertView.findViewById(R.id.main_menu_item_title)).setText(name);

      ImageView icon = (ImageView) convertView.findViewById(R.id.main_menu_item_icon);
      icon.setImageResource(mFragmentIcons[position]);

      return convertView;
    }

  }

  public void changeAccount(int account) {

    if(account == -1) {

      // Delete current account
      LoginManager.getInstance().deleteCurrentAccount(this);
    } else {

      // Change active account
      LoginManager.getInstance().switchActiveAccount(this, account);
    }

    finish();
    startActivity(new Intent(this, MainActivity.class));
  }

  public void addAccount() {

    startActivity(new Intent(this, LoginActivity.class));
    finish();
  }

  public void startBarcodeScan() {

    Intent intent = new Intent(this, com.google.zxing.client.android.CaptureActivity.class);
    intent.setAction(Intents.Scan.ACTION);
    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    startActivityForResult(intent, 0);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 0) {
      /*
       * Barcode scan
       */
      if (resultCode == RESULT_OK) {

        String contents = intent.getStringExtra("SCAN_RESULT");
        String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

        Uri uri = Uri.parse(contents);
        if(uri != null && "bitcoin".equals(uri.getScheme())) {
          // Is bitcoin URI
          mViewPager.setCurrentItem(FRAGMENT_INDEX_TRANSFER); // Switch to transfer fragment
          mTransferFragment.fillFormForBitcoinUri(uri);
        }

      } else if (resultCode == RESULT_CANCELED) {
        // Barcode scan was cancelled
      }
    } else if(requestCode == 1) {
      /*
       * Transaction details
       */
      if(resultCode == RESULT_OK) {
        // Refresh needed
        refresh();
      }
    }
  }

  public BuySellFragment getBuySellFragment() {
    return mBuySellFragment;
  }

  public TransferFragment getTransferFragment() {
    return mTransferFragment;
  }

  public AccountSettingsFragment getAccountSettingsFragment() {
    return mSettingsFragment;
  }

  public void setRefreshButtonAnimated(boolean animated) {

    mRefreshItemState = animated;

    if(mRefreshItem == null) {
      return;
    }

    if(animated) {
      mRefreshItem.setEnabled(false);
      mRefreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
    } else {
      mRefreshItem.setEnabled(true);
      mRefreshItem.setActionView(null);
    }
  }

  public void refresh() {
    mTransactionsFragment.refresh();
    mBuySellFragment.refresh();
    mTransferFragment.refresh();
    mSettingsFragment.refresh();
  }
}
