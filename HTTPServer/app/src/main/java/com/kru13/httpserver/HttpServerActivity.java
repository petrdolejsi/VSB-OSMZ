package com.kru13.httpserver;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class HttpServerActivity extends Activity implements OnClickListener{

    public static String MESSAGE_STATUS = "MSG_STATUS";
    public static String DATA_STATUS = "DATA_STATUS";

    private SocketServer s;
    public static Handler handler;
    private RecyclerView mStatusRecyclerView;
    private StatusRecyclerAdapter mStatusAdapter;
    private Camera camera;
    private CameraPreview cameraPreview;
    private byte[] picture;
    private boolean running = false;

    public static final double usedData = 0;

    SharedPreferences sharedPreferences;

    public byte[] getPicture()
    {
        //return picture;

        Bitmap realImage = BitmapFactory.decodeByteArray(picture, 0, picture.length);

        realImage= rotate(realImage, 90);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        realImage.compress(Bitmap.CompressFormat.JPEG,50,stream);

        return stream.toByteArray();
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback()
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {

            picture = data;
            if (data != null)
            {
                camera.startPreview();
            }
        }
    };
    private Camera.PreviewCallback mPprev = new Camera.PreviewCallback()
    {

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera)
        {
            picture = convertoToJpeg(bytes, camera);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sharedPreferences = this.getSharedPreferences("com.kru13.httpserver", Context.MODE_PRIVATE);

        StrictMode.ThreadPolicy policy = new
        StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_http_server);

        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {

            }
            else
            {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

            }
        }

        TextView IpAddress = (TextView) findViewById(R.id.ipAddress);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if (ipAddress.equals("0.0.0.0"))
        {
            IpAddress.setText("No IP available");
        }
        else
        {
            IpAddress.setText("IP Address: " + ipAddress);
        }

        TextView serverState = (TextView)findViewById(R.id.serverState);
        serverState.setText(getString(R.string.server_stop_text));

        checkCameraHardware(this);

        mStatusRecyclerView = findViewById(R.id.status_recycler);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

        mStatusRecyclerView.setLayoutManager(mLayoutManager);

        mStatusAdapter = new StatusRecyclerAdapter(this, new ArrayList());
        mStatusRecyclerView.setAdapter(mStatusAdapter);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);

                Bundle data = msg.getData();

                String message = data.getString(MESSAGE_STATUS);
                Long size = data.getLong(DATA_STATUS);

                if (size > 0)
                {
                    TextView usedData = findViewById(R.id.usedData);
                    long usedDataValue = sharedPreferences.getLong("usedData",0);
                    usedDataValue += size;
                    usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));
                    sharedPreferences.edit().putLong("usedData", usedDataValue).commit();
                }

                mStatusAdapter.insert(message);
            }
        };

        camera = getCameraInstance();
        camera.setPreviewCallback(mPprev);
        Log.d("ServerActivity","have camera? " + (camera != null));
        cameraPreview = new CameraPreview(this, camera, mPicture);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        camera.startPreview();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        camera.stopPreview();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }


    @Override
    public void onClick(View v)
    {
        // TODO Auto-generated method stub
        if (v.getId() == R.id.button1)
        {
            s = new SocketServer(handler, this, camera);
            s.start();

            TextView serverState = (TextView)findViewById(R.id.serverState);
            serverState.setText(getString(R.string.server_run_text));

            Toast.makeText(this, getString(R.string.server_run_text), Toast.LENGTH_SHORT).show();

            running = true;

            takePicture();
        }
        if (v.getId() == R.id.button2)
        {
            try
            {
                s.close();
                s.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            TextView serverState = (TextView)findViewById(R.id.serverState);
            serverState.setText(getString(R.string.server_stop_text));

            running = false;

            Toast.makeText(this, getString(R.string.server_stop_text), Toast.LENGTH_SHORT).show();
        }
        if (v.getId() == R.id.delete_data)
        {
            TextView usedData = findViewById(R.id.usedData);
            long usedDataValue = 0;
            sharedPreferences.edit().putLong("usedData", usedDataValue).commit();
            usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.delete_data)
        {
            TextView usedData = findViewById(R.id.usedData);
            long usedDataValue = 0;
            sharedPreferences.edit().putLong("usedData", usedDataValue).commit();
            usedData.setText("Amount of used data: " + readableFileSize(usedDataValue));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean checkCameraHardware(Context context)
    {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA))
            {

            }
            else
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);

            }
        }
        return true;
    }

    public static Camera getCameraInstance()
    {
        Camera c = null;
        try
        {
            c = Camera.open();
            c.setDisplayOrientation(90);
        }
        catch (Exception e)
        {
            // Camera is not available (in use or does not exist)
        }
        Log.d("ServerActivity","Get camera ? " + (c != null));
        return c; // returns null if camera is unavailable
    }

    public void takePicture(){
        if (camera == null)
        {
            return;
        }

        if (!running)
        {
            return;
        }

        camera.takePicture(null, null, mPicture);

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run() {
                takePicture();
            }
        }, 500);
    }

    public byte[] convertoToJpeg(byte[] data, Camera camera)
    {

        YuvImage image = new YuvImage(data, ImageFormat.NV21,
                camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 20; //set quality
        image.compressToJpeg(new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height), quality, baos);//this line decreases the image quality

        return baos.toByteArray();
    }

    public static String readableFileSize(long size)
    {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static Bitmap rotate(Bitmap bitmap, int degree)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }
}