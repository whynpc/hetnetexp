package edu.ucla.cs.wing.hetnetexp;


import java.util.LinkedList;
import java.util.Queue;

import edu.ucla.cs.wing.hetnetexp.EventLog.LogType;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private static Handler _handler;
	
	public static Handler getHandler() {
		return _handler;
	}
	
	
	private Queue<String> msgQueue = new LinkedList<String>();
	private static final int MSG_QUEUE_MAX_SIZE = 5;
	
	
	
	private EditText msgbox, editTextTraffic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		msgbox = (EditText) findViewById(R.id.msgbox);
		editTextTraffic = (EditText) findViewById(R.id.editTextTraffic);
		
		_handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case Msg.TASK_STATE:
					updateMsgbox((String) msg.obj);
					break;
				case Msg.LOCAL_BYTES:
					long bytes = (Long) msg.obj;
					bytes /= 1024;
					editTextTraffic.setText(String.valueOf(bytes));
					break;
				default:
					break;
				}
				
			}
		};
		
		startService(new Intent(this, BackgroundService.class));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		_handler = null;
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClickMobileDataOn(View view) {
		MobileInfo.getInstance().setMobileDataEnabled(true);
	}
	
	public void onClickMobileDataOff(View view) {
		MobileInfo.getInstance().setMobileDataEnabled(false);
		
	}
	
	public void onClickWifiOn(View view) {
		MobileInfo.getInstance().setWifiEnabled(true);		
	}
	
	public void onClickWifiOff(View view) {
		MobileInfo.getInstance().setWifiEnabled(false);
		
	}
	
	public void onClickUdpStart(View view) {
		BackgroundService.getInstance().startUdp();
	}
	
	public void onClickUdpStop(View view) {
		BackgroundService.getInstance().stopUdp();
	}
	
	public void onClickPingpongStart(View view) {			
		BackgroundService.getInstance().startPingpong();
	} 
	
	public void onClickPingpongStop(View view) {			
		BackgroundService.getInstance().stopPingpong();
	}
	
	public void onClickTraceStart(View view) {
		BackgroundService.getInstance().startTrace();
	}
	
	public void onClickTraceStop(View view) {
		BackgroundService.getInstance().stopTrace();		
	}
	
	public void onClickTrafficRefresh(View view) {
		sendMsg2Service(Msg.LOCAL_BYTES, null);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			this.startActivity(new Intent(MainActivity.this,
					SettingsActivity.class));
			break;

		default:
			break;
		}
		return true;
	}	
	
	private void updateMsgbox(String newMsg) {
		if (newMsg != null) {
			msgQueue.add(newMsg);
		}
		while (msgQueue.size() > MSG_QUEUE_MAX_SIZE) {
			msgQueue.poll();
		}
		StringBuilder sb = new StringBuilder();
		for (String msg : msgQueue) {
			sb.append(msg);
			sb.append('\n');
		}
		msgbox.setText(sb.toString());		
		
	}
	
	private void sendMsg2Service(int what, Object obj) {
		try {
			Handler client = BackgroundService.getInstance().getHandler();
			if (client != null) {
				Message msg = new Message();
				msg.what = what;
				msg.obj = obj;
				client.sendMessage(msg);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
