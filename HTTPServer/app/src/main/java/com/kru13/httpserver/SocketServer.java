package com.kru13.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	public final int port = 12345;
	boolean bRunning;
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
	public void run() {
        try {
			serverSocket = new ServerSocket(port);
			bRunning = true;
        	while(bRunning)
			{
				Log.d("SERVER", "Creating Socket");
				Socket s = serverSocket.accept();
				new ClientThread(s).start();
			}

        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }
    }

}
