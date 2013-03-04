coinbase-android
================

The official Android application for [Coinbase](https://coinbase.com/).

## Features
* Send/request bitcoin payments using email, QR codes, or NFC
* Buy and sell bitcoin right from your mobile phone
* View transaction history, details, and balance
* See prices in BTC or your native currency
* Support for multiple accounts
* 100% open source - contributions welcome
* Revoke [access](https://coinbase.com/account/integrations) remotely if you lose your phone.

<a href="https://dl.dropbox.com/u/1779882/Screenshot_2013-02-27-18-42-39.png"><img src="https://dl.dropbox.com/u/1779882/Screenshot_2013-02-27-18-42-39.png" width="250" /></a>

## Building

To build the Android app in [Eclipse](http://developer.android.com/sdk/index.html):

1.  `git clone git@github.com:coinbase/coinbase-android.git`
2.	Open Eclipse and go to File > Import... > Android > Existing Code into Android Workspace
3.	Choose the root directory of the cloned project
4.	Deselect all projects and only add: coinbase-android, library-actionbarsherlock, library-slidingmenu
5.  Go to Window > Android SDK Manager and check the box to install "Google APIs" under Android 4.2.2 (this is needed for SlidingMenu)
5. 	The project should now build!
