package com.coinbase.android.pusher;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.coinbase.api.LoginManager;
import com.coinbase.api.RpcManager;
import com.justinschultz.pusherclient.ChannelListener;
import com.justinschultz.pusherclient.Pusher;
import com.justinschultz.pusherclient.Pusher.Channel;
import com.justinschultz.pusherclient.PusherListener;

public class CoinbasePusherListener implements PusherListener {
  
  public static final String API_KEY = "057b12fc5917720115c1";
  public static final String PUSHER_CHANNEL = "private-transactions-50760d55328486020000005b";
  
  Pusher mPusher;
  Context mContext;
  Channel channel;
  
  public CoinbasePusherListener(Pusher pusher, Context context) {
    
    mPusher = pusher;
    mContext = context;
  }

  @Override
  public void onConnect(String socketId) {
      System.out.println("Pusher connected. Socket Id is: " + socketId);
      
      try {
        JSONObject response = RpcManager.getInstance().callPost(mContext, "pusher", null);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      channel = mPusher.subscribe(PUSHER_CHANNEL, "057b12fc5917720115c1:29727d7eaf7a1e40961e23a6eb12fa09690449ce7bedfb624f7a549935fc1859");
      System.out.println("Subscribed to channel: " + channel);

      channel.bind("update", new ChannelListener() {
          @Override
          public void onMessage(String message) {
              System.out.println("Received bound channel message: " + message);
          }
      });
  }

  @Override
  public void onMessage(String message) {
      System.out.println("Received message from Pusher: " + message);
  }

  @Override
  public void onDisconnect() {
      System.out.println("Pusher disconnected.");
  }

}
