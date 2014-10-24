package com.ibisense.ibiapp;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Random;
import java.io.FileInputStream;
import java.util.zip.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

public class Helpers {
  static {
    System.setProperty("org.apache.commons.logging.Log",
        "org.apache.commons.logging.impl.NoOpLog");
  }

  public static boolean isWindowsOS() {
    return System.getProperty("os.name").startsWith("Windows");
  }

  public static void createZipFile(File srcDir, OutputStream out)
      throws IOException {

    List<String> fileList = listDirectory(srcDir);
    ZipOutputStream zout = new ZipOutputStream(out);

    zout.setLevel(9);

    for (String fileName : fileList) {
      File file = new File(srcDir.getParent(), fileName);
      String zipName = fileName;
      if (File.separatorChar != '/')
        zipName = fileName.replace(File.separatorChar, '/');
      zipName = zipName.substring(zipName.indexOf("/") + 1);
      if (zipName.equals(""))
        continue;
      ZipEntry ze;
      if (file.isFile()) {
        ze = new ZipEntry(zipName);
        ze.setTime(file.lastModified());
        zout.putNextEntry(ze);
        FileInputStream fin = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        for (int n; (n = fin.read(buffer)) > 0;)
          zout.write(buffer, 0, n);
        fin.close();
      } else {
        ze = new ZipEntry(zipName + '/');
        ze.setTime(file.lastModified());
        zout.putNextEntry(ze);
      }
    }
    zout.close();
  }

  public static List<String> listDirectory(File directory) throws IOException {

    Stack<String> stack = new Stack<String>();
    List<String> list = new ArrayList<String>();

    // If it's a file, just return itself
    if (directory.isFile()) {
      if (directory.canRead())
        list.add(directory.getName());
      return list;
    }

    String rootPath = directory.getParent();
    String root = directory.getName();
    stack.push(root);
    while (!stack.empty()) {
      String current = (String) stack.pop();
      File curDir = new File(rootPath + File.separator + current);
      String[] fileList = curDir.list();

      if (fileList != null) {
        for (String entry : fileList) {
          if (entry.equals(".ibisense"))
            continue;

          File f = new File(curDir, entry);
          if (f.isFile()) {
            if (f.canRead()) {
              list.add(current + File.separator + entry);
            } else {
              System.err.println("File " + f.getPath() + " is unreadable");
              throw new IOException("Can't read file: " + f.getPath());
            }
          } else if (f.isDirectory()) {
            list.add(current + File.separator + entry);
            stack.push(current + File.separator + entry);
          } else {
            throw new IOException("Unknown entry: " + f.getPath());
          }
        }
      }
    }

    return list;
  }

  private static int executeRequest(HttpRequestBase requestBase) {
    int code = 0;
    InputStream responseStream = null;
    HttpClient client = new DefaultHttpClient();
    try {
      HttpResponse response = client.execute(requestBase);
      if (response != null) {
        code = response.getStatusLine().getStatusCode();
      }
    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (responseStream != null) {
        try {
          responseStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    client.getConnectionManager().shutdown();
    return code;
  }

  public static int executeMultiPartRequest(String urlString, File file,
      String fileName) {

    HttpPost postRequest = new HttpPost(urlString);
    try {
      MultipartEntity multiPartEntity = new MultipartEntity();
      FileBody fileBody = new FileBody(file);
      multiPartEntity.addPart("file", fileBody);
      postRequest.setEntity(multiPartEntity);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return executeRequest(postRequest);
  }

  public static class RandomString {

    private static final char[] symbols = new char[36];

    static {
      for (int idx = 0; idx < 10; ++idx)
        symbols[idx] = (char) ('0' + idx);
      for (int idx = 10; idx < 36; ++idx)
        symbols[idx] = (char) ('a' + idx - 10);
    }

    private final static Random random = new Random();

    private final static char[] buf = new char[8];

    public static String nextString() {
      for (int idx = 0; idx < buf.length; ++idx)
        buf[idx] = symbols[random.nextInt(symbols.length)];
      return new String(buf);
    }

  }
}
