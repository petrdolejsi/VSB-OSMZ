package com.kru13.httpserver;

import android.os.Environment;
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
import java.util.ArrayList;
import java.util.List;

public class ClientThread extends Thread {

    Socket s;
    boolean bRunning;

    public ClientThread(Socket s) {
        this.s = s;
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

            while (!(tmp = in.readLine()).isEmpty()) {
                request.add(tmp);
                Log.d("SERVER CLIENT REQUEST", tmp);
            }

            String fileName = request.get(0).split(" ")[1];
            Log.d("REQUESTED FILE", fileName);

            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/HttpServer" + fileName);

            if (file.isDirectory()) {
                Log.d("FILE TYPE", "Folder");
                String html = "";
                html += "<html><body><h1>Index of " + fileName + "</h1><table><tr><th width=\"100\">Type</th><th width=\"200\">Name</th><th width=\"70\">Size</th></tr>";
                File[] listOfFiles = file.listFiles();

                if (!fileName.equals("/"))
                {
                    html += "<tr><td></td><td><a href=\"../\">Parent Directory</a></td><td></td></tr>";
                }
                for (File tempFile : listOfFiles) {
                    if (tempFile.isDirectory())
                    {
                        html += "<tr><td></td><td><a href=\"" + tempFile.getName() + "\">" + tempFile.getName() + "</a></td><td></td></tr>";
                    }
                }
                for (File tempFile : listOfFiles) {
                    if (tempFile.isFile()) {
                        html += "<tr><td>" + getMimeType(tempFile.getName()) + "</td><td><a href=\"" + tempFile.getName() + "\">" + tempFile.getName() + "</a></td><td>" + getFormattedSize(tempFile.length()) + "</td></tr>";
                    }
                }
                html += "</table></body></html>\n";


                out.write("HTTP/1.1 200 OK");
                out.write("Content-type: text/html\n");
                out.write("Content-Length:" + String.valueOf(html.length()) + "\n");
                out.write("\n");
                out.write(html);
                out.flush();

            } else if (file.exists()) {
                Log.d("FILE TYPE", "File");
                out.write("HTTP/1.1 200 OK");
                int i = fileName.lastIndexOf('.');
                String extension = "";
                if (i > 0) {
                    extension = fileName.substring(i + 1);
                }
                if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("html") || extension.equals("htm")) {
                    out.write("Connection: close\n");
                    out.write("Content-Type: " + getMimeType(fileName) + "\n");
                    out.write("Content-Length:" + String.valueOf(file.length()) + "\n");
                    out.write("\n");
                    out.flush();

                    FileInputStream fis = new FileInputStream(file);
                    byte buffer[] = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer, 0, 1024)) > 0) {
                        o.write(buffer, 0, len);
                    }
                    out.flush();
                    s.close();

                } else {
                    out.write("\n");
                    out.write("<html><body><h1>Unknown extension</h1></body></html>\n");
                }
                out.flush();
            } else {
                Log.d("FILE TYPE", "404");
                out.write("HTTP/1.1 404 Not Found\n");
                out.write("Content-type: text/html\n");
                out.write("\n");
                out.write("<html><body><h1>Error 404</h1></body></html>\n");
                out.flush();
            }

            s.close();
            Log.d("SERVER", "Socket Closed");
        }
        catch (IOException e) {
            if (s != null && s.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        }
        finally {
            s = null;
            bRunning = false;
        }
    }
}
