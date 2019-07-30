package com.kru13.httpserver;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.FileNameMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.kru13.httpserver.SocketServer.sendMessage;


public class ClientHandler extends Thread{

    private ServerSocket serverSocket;
    private Handler handler;
    private HttpServerActivity activity;
    public static DataOutputStream stream;
    private ByteArrayOutputStream imageBuffer;
    private boolean closeSocket = true;

    public ClientHandler(ServerSocket serverSocket, Handler handler, HttpServerActivity activity){
        this.serverSocket = serverSocket;
        this.handler = handler;
        this.activity = activity;

        imageBuffer = new ByteArrayOutputStream();
    }

    public void run()
    {
        boolean bRunning;
        try
        {
            bRunning = true;
            while (bRunning)
            {
                Log.d("SERVER", "Socket Waiting for connection");
                Log.d("SERVER", "Socket Accepted");

                Socket s = serverSocket.accept();

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
                if(!request.isEmpty())
                {
                    Log.d("SERVER", "out: " + request.toString());
                    String data[] = request.get(0).split(" ");

                    String fileName = data[1];

                    if (data[0].toUpperCase().equals("POST"))
                    {
                        if (fileName.equals("/upload/upload"))
                        {

                        }
                    }
                    if (data[0].toUpperCase().equals("GET"))
                    {
                        Log.d("SERVER", "File exist");
                        // writing image async
                        if (fileName.equals("/camera/snapshot"))
                        {
                            out.write("HTTP/1.0 200 Ok\n");
                            out.write("Date: " + Calendar.getInstance().getTime() + "\n");
                            out.write("Content-Length: " + String.valueOf(activity.getPicture().length) + "\n");
                            out.write("Content-Type: image/jpeg\n");

                            out.write("\n");
                            out.flush();

                            o.write(activity.getPicture());
                            o.flush();

                            sendMessage(handler, "Requested: camera snapshot ; Size: " + String.valueOf(getFormattedSize(activity.getPicture().length)), Long.valueOf(activity.getPicture().length));
                        }
                        else if (fileName.equals("/camera/stream"))
                        {
                            stream = new DataOutputStream(s.getOutputStream());
                            if (stream != null)
                            {
                                try
                                {
                                    Log.d("onPreviewFrame", "stream succ");
                                    stream.write(("HTTP/1.0 200 OK\n" +
                                            "Server: localhost/12345\n" +
                                            "Cache-Control:  no-cache\n" +
                                            "Cache-Control:  private\n" +
                                            "Content-Type: multipart/x-mixed-replace;boundary=--boundary\n" ).getBytes());

                                    stream.flush();

                                    closeSocket = false;
                                    Log.d("onPreviewFrame", "stream created");

                                    sendMessage(handler, "Requested: camera stream image ; Size: " + String.valueOf(getFormattedSize(activity.getPicture().length)), Long.valueOf(activity.getPicture().length));

                                    sendStreamData();
                                }
                                catch (IOException e)
                                {
                                    Log.d("ERROR:", e.getLocalizedMessage());
                                }
                            }
                        }
                        else if (fileName.equals("/camera"))
                        {
                            String html = "<html><head><title>Camera</title></head><body><h1>Camera</h1><p><a href=\"camera/stream\">Stream</a></p><p><a href=\"camera/snapshot\">Snapshot</a></p></body></html>";
                            out.write("HTTP/1.1 200 OK");
                            out.write("Content-type: text/html\n");
                            out.write("Content-Length:" + String.valueOf(html.length()) + "\n");
                            out.write("\n");
                            out.write(html);
                            out.flush();

                            sendMessage(handler, "Requested: camera; Size: " + String.valueOf(getFormattedSize(html.length())), Long.valueOf(html.length()));

                        }
                        else if (fileName.equals("/cgi-bin"))
                        {
                            String html = "<html><head><title>cgi-bin</title></head><body><h1>cgi-bin</h1>" +
                                    "<p><a href=\"cgi-bin/uptime\">uptime</a></p>" +
                                    "<p><a href=\"cgi-bin/uptime%20-a\">uptime -a</a></p>" +
                                    "<p><a href=\"cgi-bin/pm%20list%20packages\">pm list packages</a></p>" +
                                    "</body></html>";
                            out.write("HTTP/1.1 200 OK");
                            out.write("Content-type: text/html\n");
                            out.write("Content-Length:" + String.valueOf(html.length()) + "\n");
                            out.write("\n");
                            out.write(html);
                            out.flush();

                            sendMessage(handler, "Requested: cgi-bin; Size: " + String.valueOf(getFormattedSize(html.length())), Long.valueOf(html.length()));
                        }
                        else if (fileName.contains("cgi-bin")) {
                            String commands[] = fileName.split("/");

                            String command = commands[2];

                            if (commands.length < 3) {
                                sendMessage(handler, "Requested: cgi-bin without command", Long.valueOf(0));
                                break;
                            }

                            for (int i = 3; i < commands.length; i++)
                            {
                                command += "/" + commands[i];
                            }

                            command = command.replace("%20", " ");

                            try
                            {
                                String html = "<html><head><title>cgi-bin: " + command + "</title></head><body><h1>cgi-bin: " + command + "</h1>";

                                Process process = Runtime.getRuntime().exec(command);
                                BufferedReader bufferedReader = new BufferedReader(
                                        new InputStreamReader(process.getInputStream()));

                                String line;
                                while ((line = bufferedReader.readLine()) != null)
                                {
                                    html += "<p>" + line + "</p>";
                                }

                                BufferedReader errinput = new BufferedReader(new InputStreamReader(
                                        process.getErrorStream()));

                                while ((line = errinput.readLine()) != null)
                                {
                                    html += "<p>" + line + "</p>";
                                }


                                html +="</html>";

                                out.write("HTTP/1.0 200 Ok\n");
                                out.write("Date: " + Calendar.getInstance().getTime() + "\n");
                                out.write("Content-Length: " + String.valueOf(html.length()) + "qn");
                                out.write("Content-Type: text/html\n");
                                out.write("\n");
                                out.write(html);
                                out.flush();

                                sendMessage(handler, "Requested: cgi-bin command; Size: " + String.valueOf(getFormattedSize(html.length())), Long.valueOf(html.length()));

                            }
                            catch (Exception e)
                            {
                                Log.d("ProcessOutput", "just failed: " + e.getMessage());

                            }
                        }
                        else if (fileName.equals("/upload"))
                        {
                            String html = "<html><head><title>Upload file</title></head><body><h1>Upload file</h1>";
                            html += "<form method=\"post\" action=\"/upload/upload/\" enctype=\"multipart/form-data\">";
                            html += "File: <input type=\"file\" name=\"file\"><br>";
                            html += "Name: <input type=\"text\" name=\"name\"><br>";
                            html += "<input type=\"submit\" value=\"Submit\">";
                            html += "</form>";


                            out.write("HTTP/1.0 200 Ok\n");
                            out.write("Date: " + Calendar.getInstance().getTime() + "\n");
                            out.write("Content-Length: " + String.valueOf(html.length()) + "qn");
                            out.write("Content-Type: text/html\n");
                            out.write("\n");
                            out.write(html);
                            out.flush();
                        }
                        else if (fileName.equals("/upload/uploads"))
                        {

                        }
                        else
                        {
                            File outFile = new File(Environment.getExternalStorageDirectory().getPath() + "/HttpServer" + fileName);
                            if (outFile.isDirectory())
                            {
                                Log.d("FILE TYPE", "Folder");
                                String html = "";
                                html += "<html><head><title>Index of " + fileName + "</title></head><body><h1>Index of " + fileName + "</h1><table><tr><th width=\"200\">Name</th><th width=\"100\">Type</th><th width=\"170\">Last modified</th><th width=\"70\">Size</th></tr>";
                                File[] listOfFiles = outFile.listFiles();

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

                                sendMessage(handler, "Requested folder: " +  fileName + " ; Size: " + String.valueOf(getFormattedSize(html.length())), outFile.length());
                            }
                            else if (outFile.exists())
                            {
                                out.write("HTTP/1.0 200 Ok\n");
                                out.write("Date: " + Calendar.getInstance().getTime() + "\n");
                                out.write("Content-Length: " + String.valueOf(outFile.length()) + "\n");

                                sendMessage(handler, "Requested File: " +  fileName + " ; Size: " + String.valueOf(getFormattedSize(outFile.length())), outFile.length());

                                out.write("\n");
                                out.flush();

                                byte[] buf = new byte[1024];
                                int len;
                                FileInputStream fis = new FileInputStream(outFile);
                                while ((len = fis.read(buf)) > 0)
                                {
                                    o.write(buf, 0, len);
                                }

                            }
                            else
                            {
                                Log.d("SERVER","File not found");
                            }
                        }
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

                        sendMessage(handler, "Error 404 ; Size: " +  String.valueOf(getFormattedSize(html.length())), Long.valueOf(html.length()));

                        Log.d("SERVER","bad request methode!");
                    }
                }

                if (closeSocket)
                {
                    s.close();

                    Log.d("SERVER", "Socket Closed");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            if (serverSocket != null && serverSocket.isClosed())
            {
                Log.d("SERVER", "Normal exit");
            }
            else
                {
                Log.d("SERVER", "Error");
                sendMessage(handler, "Socket server error occured", Long.valueOf(0));
                e.printStackTrace();
            }
        }
        finally
        {
            serverSocket = null;
            bRunning = false;
        }
    }

    private void sendStreamData()
    {
        if (stream != null)
        {
            try
            {
                byte[] baos = activity.getPicture();
                // buffer is a ByteArrayOutputStream
                imageBuffer.reset();
                imageBuffer.write(baos);
                imageBuffer.flush();
                // write the content header
                stream.write(("\n--boundary\n" +
                        "Content-type: image/jpeg\n" +
                        "Content-Length: " + imageBuffer.size() + "\n\n").getBytes());

                stream.write(imageBuffer.toByteArray());
                stream.write(("\n").getBytes());

                stream.flush();

                sendMessage(handler, "Requested: camera stream image ; Size: " + String.valueOf(getFormattedSize(activity.getPicture().length)), Long.valueOf(activity.getPicture().length));
                Log.d("onPreviewFrame", "succ");
            }
            catch (IOException e)
            {
                Log.d("onPreviewFrame error:  ", e.getLocalizedMessage());
            }
        }
        else
            {

            Log.d("onPreviewFrame", "null");
        }

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run() {
                sendStreamData();
            }
        }, 100);
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
}