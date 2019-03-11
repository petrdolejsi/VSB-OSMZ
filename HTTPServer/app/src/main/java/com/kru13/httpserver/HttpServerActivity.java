package com.kru13.httpserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class HttpServerActivity extends Activity implements OnClickListener
{

	private SocketServer s;
	private Camera mCamera;
	private CameraPreview mPreview;

	private Handler cameraHandler = new Handler();

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

		mCamera = getCameraInstance();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
	}

	public void takePicture(){
		//mCamera.setDisplayOrientation(90);
		mCamera.takePicture(null, null, mPicture);

		cameraHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				takePicture();
			}
		}, 1000);
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

			takePicture();
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

	public  Camera getCameraInstance(){
		try {
		    mCamera = Camera.open(); // attempt to get a Camera instance
			mCamera.setDisplayOrientation(90);
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
		}
		return mCamera; // returns null if camera is unavailable
	}

	private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile();
			if (pictureFile == null){
				Log.d(TAG, "Error creating media file, check storage permissions");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);

                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                ExifInterface exif=new ExifInterface(pictureFile.toString());

                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){
                    realImage= rotate(realImage, 90);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
                    realImage= rotate(realImage, 270);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
                    realImage= rotate(realImage, 180);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")){
                    realImage= rotate(realImage, 90);
                }

                boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);

				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
			mCamera.startPreview();
		}
	};

	private static File getOutputMediaFile(){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		File mediaFile;
		mediaFile = new File(Environment.getExternalStorageDirectory().getPath() + "/HttpServer/camera" + File.separator +
				"camera.jpg");

        return mediaFile;
	}

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }
}
