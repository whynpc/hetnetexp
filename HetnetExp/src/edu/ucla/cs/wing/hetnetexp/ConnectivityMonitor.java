package edu.ucla.cs.wing.hetnetexp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityMonitor extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle.containsKey("networkInfo")) {
			NetworkInfo networkInfo = (NetworkInfo) bundle.get("networkInfo");
			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				Log.d("hetnet", networkInfo.toString());
				if (BackgroundService.getInstance() != null) {
					BackgroundService.getInstance().onMobileDataChange(
							networkInfo.isConnectedOrConnecting());
				}				
			}

		}

	}

}
