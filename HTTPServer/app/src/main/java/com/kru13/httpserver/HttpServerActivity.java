package com.kru13.httpserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.Date;

public class HttpServerActivity extends Activity implements OnClickListener
{

	private SocketServer s;

	public static final double usedData = 0;

	SharedPreferences sharedpreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_http_server);

		sharedpreferences = this.getSharedPreferences("com.kru13.httpserver", Context.MODE_PRIVATE);

		Button btn1 = (Button) findViewById(R.id.button1);
		Button btn2 = (Button) findViewById(R.id.button2);

		TextView IpAddress = (TextView) findViewById(R.id.ipAddress);
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
		if (ipAddress.equals("0.0.0.0"))
		{
			IpAddress.setText("No IP available");
		}
		else
		{
			IpAddress.setText("Your Device IP Address: " + ipAddress);
		}

		TextView log = (TextView) findViewById(R.id.log);
		log.setMovementMethod(new ScrollingMovementMethod());

		TextView usedData = findViewById(R.id.usedData);
		long usedDataValue = sharedpreferences.getLong("usedData",0);
		usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));

		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);

		Button deleteBtn = (Button) findViewById(R.id.deleteBtn);
		deleteBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				TextView usedData = findViewById(R.id.usedData);
				long usedDataValue = 0;
				sharedpreferences.edit().putLong("usedData", usedDataValue).commit();
				usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));

				TextView text = findViewById(R.id.log);
				text.setText("");
			}
		});

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.http_server, menu);
		return true;
	}

	public static String readableFileSize(long size) {
		if(size <= 0) return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1)
		{
			s = new SocketServer(messageHandler);
			s.start();

			TextView serverState = (TextView)findViewById(R.id.serverState);
			serverState.setText(getString(R.string.server_run_text));

			TextView text = findViewById(R.id.log);
			Date date = new Date();
			String dateString = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
			String typeOfRequest = "Start server";
			String nameOfSite = "";
			text.setText(dateString + "\t" + typeOfRequest +"\t" + nameOfSite + "\n" + text.getText());

			Toast.makeText(this, getString(R.string.server_run_text), Toast.LENGTH_SHORT).show();
		}
		if (v.getId() == R.id.button2 && (s != null && s.bRunning == true ))
		{
			s.close();
			TextView text = findViewById(R.id.log);
			Date date = new Date();
			String dateString = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
			String typeOfRequest = "Stop server";
			String nameOfSite = "";
			text.setText(dateString + "\t" + typeOfRequest +"\t" + nameOfSite + "\n" + text.getText());
			try
			{
				s.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			TextView serverState = (TextView)findViewById(R.id.serverState);
			serverState.setText(getString(R.string.server_stop_text));

			Toast.makeText(this, getString(R.string.server_stop_text), Toast.LENGTH_SHORT).show();
		}
	}

	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			TextView text = findViewById(R.id.log);

			Date date = new Date();
			String dateString = date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();

			String typeOfRequest = bundle.getString("REQUEST");

			String nameOfSite = bundle.getString("NAME");

			text.setText(dateString + "\t" + typeOfRequest +"\t" + nameOfSite + "\n" + text.getText());

			TextView usedData = findViewById(R.id.usedData);
			long usedDataValue = sharedpreferences.getLong("usedData",0);
			usedDataValue += bundle.getLong("SIZE");
			usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));
			sharedpreferences.edit().putLong("usedData", usedDataValue).commit();
		}
	};


}
