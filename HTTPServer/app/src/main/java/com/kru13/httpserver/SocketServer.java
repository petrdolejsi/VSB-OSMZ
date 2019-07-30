package com.kru13.httpserver;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;

import static com.kru13.httpserver.HttpServerActivity.DATA_STATUS;
import static com.kru13.httpserver.HttpServerActivity.MESSAGE_STATUS;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    private ClientHandler serverHandler;
    private HttpServerActivity activity;
    private Handler handler;
    private final int port = 12345;
    private Camera camera;

    public SocketServer(Handler handler, HttpServerActivity activity, Camera camera)
    {
        this.handler = handler;
        this.activity = activity;
        this.camera = camera;
    }

    public void close()
    {
        try
        {
            if(!serverSocket.isClosed())
            {

                serverSocket.close();
                sendMessage(handler, "Socket server exited", Long.valueOf(0));
            }
        }
        catch (IOException e)
        {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
    }

    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(port);
            serverHandler = new ClientHandler(serverSocket, handler, activity);
            serverHandler.start();

            sendMessage(handler, "Socket server connected", Long.valueOf(0));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    public static void sendMessage(Handler handler, String message, Long size)
    {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_STATUS, message);
        bundle.putLong(DATA_STATUS, size);

        msg.setData(bundle);

        handler.sendMessage(msg);
    }
}