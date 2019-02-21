package com.kru13.httpserver;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class HttpServerActivity extends Activity implements OnClickListener
{

	private SocketServer s;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);
        
        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);

		TextView IpAddress = (TextView)findViewById(R.id.ipAddress);
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
		IpAddress.setText("Your Device IP Address: " + ipAddress);
         
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1)
		{
			s = new SocketServer();
			s.start();

            TextView serverState = (TextView)findViewById(R.id.serverState);
            serverState.setText(getString(R.string.server_run_text));

			Toast.makeText(this, getString(R.string.server_run_text), Toast.LENGTH_SHORT).show();
		}
		if (v.getId() == R.id.button2)
		{
			s.close();
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
}
