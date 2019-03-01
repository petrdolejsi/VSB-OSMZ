package com.kru13.httpserver;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.FileNameMap;
import java.net.Socket;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ClientThread extends Thread
{

    Socket s;
    Handler messageHandler;
    boolean bRunning;

    public ClientThread(Socket s, Handler messageHandler) {
        this.s = s;
        this.messageHandler = messageHandler;
    }

    public static String getMimeType(String url)
    {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String type = fileNameMap.getContentTypeFor(url);

        return type;
    }

    public static String getFormattedSize(long value)
    {
        String output = String.valueOf(value);
        if (value > 1000000000)
        {
            output = String.valueOf(value / 1000000000) + "G";
        }
        if (value > 1000000)
        {
            output = String.valueOf(value / 1000000) + "M";
        }
        if (value > 1000)
        {
            output = String.valueOf(value / 1000) + "K";
        }
        return output;
    }

    public void run()
    {
        try
        {
            Log.d("SERVER", "Socket Waiting for connection");
            Log.d("SERVER", "Socket Accepted");

            OutputStream o = s.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            List<String> request = new ArrayList<>();
            String tmp;

            while (!(tmp = in.readLine()).isEmpty())
            {
                request.add(tmp);
                Log.d("SERVER CLIENT REQUEST", tmp);
            }

            String fileName = request.get(0).split(" ")[1];
            Log.d("REQUESTED FILE", fileName);

            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/HttpServer" + fileName);

            if (file.isDirectory())
            {
                Log.d("FILE TYPE", "Folder");
                String html = "";
                html += "<html><head><title>Index of " + fileName + "</title></head><body><h1>Index of " + fileName + "</h1><table><tr><th width=\"200\">Name</th><th width=\"100\">Type</th><th width=\"170\">Last modified</th><th width=\"70\">Size</th></tr>";
                File[] listOfFiles = file.listFiles();

                if (!fileName.equals("/"))
                {
                    html += "<tr><td><a href=\"../\">Parent Directory</a></td><td></td><td></td><td></td></tr>";
                }
                String folders = "";
                String files = "";
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                for (File tempFile : listOfFiles)
                {
                    if (tempFile.isDirectory())
                    {
                        folders += "<tr><td><a href=\"" + tempFile.getName() + "\">" + tempFile.getName() + "/</a></td><td>-</td><td>-</td><td>-</td></tr>";
                    }

                    if (tempFile.isFile())
                    {
                        int i = tempFile.getName().lastIndexOf('.');

                        files += "<tr><td><a href=\"" + fileName;

                        if (!fileName.equals("/"))
                        {
                            files += "/";
                        }

                        files += tempFile.getName() + "\">" + tempFile.getName() + "</a></td><td>" + tempFile.getName().substring(i + 1) + "</td><td>" + sdf.format(tempFile.lastModified()) + "</td><td>" + getFormattedSize(tempFile.length()) + "</td></tr>";
                    }
                }
                html += folders + files;
                html += "</table></body></html>\n";

                out.write("HTTP/1.1 200 OK");
                out.write("Content-type: text/html\n");
                out.write("Content-Length:" + String.valueOf(html.length()) + "\n");
                out.write("\n");
                out.write(html);
                out.flush();

                Message msg = messageHandler.obtainMessage();
                Bundle bndl = new Bundle();
                bndl.putString("REQUEST", "Directory");
                bndl.putString("NAME", fileName);
                bndl.putLong("SIZE", html.length());
                msg.setData(bndl);
                messageHandler.sendMessage(msg);

            }
            else if (file.exists())
            {
                Log.d("FILE TYPE", "File");
                out.write("HTTP/1.1 200 OK");
                int i = fileName.lastIndexOf('.');
                String extension = "";

                if (i > 0)
                {
                    extension = fileName.substring(i + 1);
                }

                out.write("Connection: close\n");
                out.write("Content-Type: " + getMimeType(fileName) + "\n");
                out.write("Content-Length:" + String.valueOf(file.length()) + "\n");
                out.write("\n");
                out.flush();

                FileInputStream fis = new FileInputStream(file);
                byte buffer[] = new byte[1024];
                int len;
                while ((len = fis.read(buffer, 0, 1024)) > 0)
                {
                    o.write(buffer, 0, len);
                }
                out.flush();
                s.close();

                Message msg = messageHandler.obtainMessage();
                Bundle bndl = new Bundle();
                bndl.putString("REQUEST", "File");
                bndl.putString("NAME", fileName);
                bndl.putLong("SIZE", file.length());
                msg.setData(bndl);
                messageHandler.sendMessage(msg);
            }
            else
            {
                Log.d("FILE TYPE", "404");
                String html = "";
                html += "HTTP/1.1 404 Not Found\n";
                html += "Content-type: text/html\n";
                html += "\n";
                html += "<html><head><title>Error 404</title></head><body><h1>Error 404 - file not found</h1></body></html>\n";
                out.write(html);
                out.flush();

                Message msg = messageHandler.obtainMessage();
                Bundle bndl = new Bundle();
                bndl.putString("REQUEST", "Error 404");
                bndl.putString("NAME", fileName);
                bndl.putLong("SIZE", html.length());
                msg.setData(bndl);
                messageHandler.sendMessage(msg);
            }

            s.close();
            Log.d("SERVER", "Socket Closed");
        }
        catch (IOException e)
        {
            if (s != null && s.isClosed())
            {
                Log.d("SERVER", "Normal exit");
            }
            else
            {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally
        {
            s = null;
            bRunning = false;
        }
    }

    private final Runnable mMessageSender = new Runnable() {
        public void run() {
            Message msg = messageHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("REQUEST", "test");
            msg.setData(bundle);
            messageHandler.sendMessage(msg);
        }
    };
}
