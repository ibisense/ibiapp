package com.ibisense.ibiapp;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.io.Console;
import java.io.PrintWriter;

import static com.ibisense.ibiapp.AppManagerConfiguration.*;

public class AppManager {
  final static String BASE_URL = "https://ibi.io/v1";
  final static String IBISENSE_LOGIN_RESOURCE = "/get/security/signin/";
  final static String IBISENSE_CREATE_RESOURCE = "/add/$MASTER_KEY/app/id/";
  final static String IBISENSE_DEPLOY_RESOURCE = "/post/$MASTER_KEY/app/contents/$app_uid";
  final static String IBISENSE_REMOVE_RESOURCE = "/delete/$MASTER_KEY/app/id/$app_uid/";
  final static String IBISENSE_UPDATE_RESOURCE = "/update/$MASTER_KEY/app/id/";

  final static int MAX_RETRIES = 5;

  @SuppressWarnings("unchecked")
  public static void init(String currentDir, String name,
      AppManagerConfiguration config) {
    try {
      String masterKey = authenticate();
      String baseDir = currentDir + File.separator + name;
      File theDir = new File(baseDir);
      if (currentDir.equals(name) && new File(".ibisense").exists()) {
        System.out
            .println("It seems that the application with such name already exist");
        System.exit(-1);
      }
      if (!theDir.exists()) {
        boolean result = theDir.mkdir();
        if (!result) {
          System.out.println("Failed to create application directory");
          System.exit(-1);
        }
      } else {
        System.out
            .println("It seems that the application with such name already exist");
        System.exit(-1);
      }

      new File(baseDir + File.separator + config.getJavascriptFolder()).mkdir();
      new File(baseDir + File.separator + config.getCssFolder()).mkdir();
      new File(baseDir + File.separator + config.getImageFolder()).mkdir();
      new File(baseDir + File.separator + ".ibisense").mkdir();

      if (config.getStoreCredentials()) {
        PrintWriter writer = new PrintWriter(baseDir + File.separator
            + ".ibisense/apikey", "UTF-8");
        writer.println(masterKey);
        writer.close();
      } else {
        Console cons = System.console();
        String storeCredentials = cons.readLine("%s ",
            "Would you like to store API key in .ibisense/apikey file? [Y/N]:");
        if (storeCredentials != null
            && storeCredentials.toLowerCase().equals("y")) {
          if (!Helpers.isWindowsOS()) {
            Set<PosixFilePermission> perms = PosixFilePermissions
                .fromString("rw-------");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(perms);
            Files.createFile(
                Paths.get(baseDir + File.separator + ".ibisense/apikey"), attr);
          }
          PrintWriter writer = new PrintWriter(baseDir + File.separator
              + ".ibisense/apikey", "UTF-8");
          writer.println(masterKey);
          writer.close();
          if (Helpers.isWindowsOS()) {
            new File(baseDir + File.separator + ".ibisense/apikey")
                .setReadable(true, true);
            new File(baseDir + File.separator + ".ibisense/apikey")
                .setWritable(true, true);
          }
        }
      }

      String librariesTemplate = "";
      for (Library lib : config.getListOfLibraries()) {
        try {

          URL url = new URL(lib.url);
          ReadableByteChannel rbc = Channels.newChannel(url.openStream());
          FileOutputStream fos = new FileOutputStream(baseDir + File.separator
              + config.getJavascriptFolder() + File.separator + lib.libName);
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          librariesTemplate += "<script src='" + config.getJavascriptFolder()
              + "/" + lib.libName + "'></script>";
          fos.close();
        } catch (MalformedURLException mue) {
          System.out.println("Invalid url. Skipping");
        } catch (IOException ioe) {
          System.out.println("Network problem. Skipping");
        } catch (Exception e) {
          System.out.println("Unknown erro. Skipping");
        }
      }

      String stylesheetsTemplate = "";
      for (Stylesheet css : config.getListOfStylesheets()) {
        try {

          URL url = new URL(css.url);
          ReadableByteChannel rbc = Channels.newChannel(url.openStream());
          FileOutputStream fos = new FileOutputStream(baseDir + File.separator
              + config.getCssFolder() + File.separator + css.cssName);
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          stylesheetsTemplate += "<link rel='stylesheet' href='"
              + config.getCssFolder() + "/" + css.cssName + "'>";
          fos.close();
        } catch (MalformedURLException mue) {
          System.out.println("Invalid url. Skipping");
        } catch (IOException ioe) {
          System.out.println("Network problem. Skipping");
        } catch (Exception e) {
          System.out.println("Unknown error. Skipping");
        }
      }
      String template = config.getTemplate()
          .replace("{{libraries}}", librariesTemplate)
          .replace("{{stylesheets}}", stylesheetsTemplate);

      {
        PrintWriter writer = new PrintWriter(baseDir + File.separator
            + "index.html", "UTF-8");
        writer.println(template);
        writer.close();
      }
      Console cons = System.console();
      String auid = Helpers.RandomString.nextString();
      System.out.println("Provide meta information for you application: ");
      System.out.println("Application name: " + name);
      System.out.println("Application id: " + auid);
      System.out.println("Application url: https://apps.ibisense.net/" + auid
          + "/");
      System.out.println("Application version: 0.0.1");
      String desc = cons
          .readLine(
              "%s",
              "Provide short application description \n(press Enter to skip, default empty): ");
      String permission = cons
          .readLine("%s",
              "Choose permissions 1) public 2) owner-only (default) 3) group-wide: ");

      boolean gp = false, op = true, pp = false;
      if (permission.equals("1")) {
        gp = true;
        pp = true;
      }
      if (permission.equals("3")) {
        gp = true;
      }

      JSONObject jsonObject = new JSONObject();

      jsonObject.put("AUID", auid);
      jsonObject.put("name", name);
      jsonObject.put("description", desc);
      jsonObject.put("version", "0.0.0");
      jsonObject.put("url", "https://apps.ibisense.com/" + auid + "/");
      jsonObject.put("is_owner", "true");
      jsonObject.put("group_wide", gp);
      jsonObject.put("owner_only", op);
      jsonObject.put("public", pp);

      int retries = 0;
      while (retries < MAX_RETRIES) {
        try {
          JSONObject result = createApp(jsonObject, masterKey);
          PrintWriter writer = new PrintWriter(baseDir + File.separator
              + ".ibisense/app.raw.meta", "UTF-8");
          writer.println("AUID::" + result.get("AUID"));
          writer.println("name::" + result.get("name"));
          writer.println("description::" + result.get("description"));
          writer.println("version::" + result.get("version"));
          writer.println("url::" + "https://apps.ibisense.net/"
              + result.get("AUID") + "/");
          writer.println("is_onwer::" + result.get("is_onwer"));
          writer.println("group_wide::" + result.get("group_wide"));
          writer.println("owner_only::" + result.get("owner_only"));
          writer.println("public::" + result.get("public"));
          writer.close();
          break;
        } catch (Exception e) {
          e.printStackTrace();
        }
        retries++;
      }
      if (retries >= MAX_RETRIES) {
        System.out.println("Could not create app after " + MAX_RETRIES
            + " retries");
        System.exit(-1);
      } else {
        System.out.println("Application was created. Now deploying ...");
        deploy(currentDir, name, config);
      }
    } catch (InvalidCredentialsException ce) {
      System.out.println("Invalid credentials");
    } catch (Exception e) {

    }
  }

  @SuppressWarnings("unchecked")
  public static void deploy(String currentDir, String name,
      AppManagerConfiguration config) {
    try {
      String baseDir = currentDir + File.separator + (name != null ? name : "");
      String workDir = baseDir;
      String zipfileDir = currentDir;
      File theDir = new File(baseDir);
      if (name == null && new File(".ibisense").exists()) {
        workDir = currentDir;
        zipfileDir = new File(currentDir).getParent();
      } else {
        if (!theDir.exists()) {
          System.out
              .println("It seems like this is not an Ibisense application directory");
          System.exit(-1);
        }
        if (!new File(baseDir + File.separator + ".ibisense/app.raw.meta")
            .exists()) {
          System.out
              .println("It seems like this is not an Ibisense application directory");
          System.exit(-1);
        }
      }

      BufferedReader br = new BufferedReader(new FileReader(workDir
          + File.separator + ".ibisense/app.raw.meta"));
      String line;
      String auid = null;
      String appurl = "";
      String version = "";
      String desc = "";
      boolean gp = false;
      boolean op = false;
      boolean pp = false;

      while ((line = br.readLine()) != null) {

        if (line.startsWith("public::")) {
          pp = Boolean.valueOf(line.replace("public::", "").trim());
        }
        if (line.startsWith("owner_only::")) {
          op = Boolean.valueOf(line.replace("owner_only::", "").trim());
        }
        if (line.startsWith("group_wide::")) {
          gp = Boolean.valueOf(line.replace("group_wide::", "").trim());
        }
        if (line.startsWith("description::")) {
          desc = line.replace("description::", "").trim();
        }
        if (line.startsWith("name::")) {
          name = line.replace("name::", "").trim();
        }
        if (line.startsWith("AUID::")) {
          auid = line.replace("AUID::", "").trim();
        }
        if (line.startsWith("url::")) {
          appurl = line.replace("url::", "").trim();
        }

        if (line.startsWith("version::")) {
          version = line.replace("version::", "").trim();
          if (version.equals("")) {
            version = "0.0.1";
          }

          String[] subversion = version.split("\\.");

          if (subversion.length != 3) {
            version = "0.0.1";
            subversion = version.split("\\.");
          }

          int[] subversionNum = new int[3];
          for (int i = 0; i < 3; i++) {
            try {
              subversionNum[i] = Integer.parseInt(subversion[i]);
            } catch (NumberFormatException e) {
              subversionNum[i] = 0;
            }
          }

          if (subversionNum[2] + 1 >= 10) {
            subversionNum[2] = 0;
            if (subversionNum[1] + 1 >= 10) {
              subversionNum[0] += 1;
              subversionNum[1] = 0;
            } else {
              subversionNum[1] += 1;
            }
          } else {
            subversionNum[2] += 1;
          }

          version = subversionNum[0] + "." + subversionNum[1] + "."
              + subversionNum[2];
        }
      }

      br.close();

      if (auid == null) {
        System.out
            .println("It seems like this application directory was corrupted... Cannot continue");
        System.exit(-1);
      }

      String masterKey = null;
      if (new File(baseDir + File.separator + ".ibisense/apikey").exists()) {
        br = new BufferedReader(new FileReader(baseDir + File.separator
            + ".ibisense/apikey"));
        masterKey = br.readLine();
        br.close();
      } else {
        masterKey = authenticate();
        Console cons = System.console();
        String storeCredentials = cons.readLine("%s ",
            "Would you like to store API key in .ibisense/apikey file? [Y/N]:");
        if (storeCredentials != null
            && storeCredentials.toLowerCase().equals("y")) {
          if (!Helpers.isWindowsOS()) {
            Set<PosixFilePermission> perms = PosixFilePermissions
                .fromString("rw-------");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                .asFileAttribute(perms);
            Files.createFile(
                Paths.get(baseDir + File.separator + ".ibisense/apikey"), attr);
          }
          PrintWriter writer = new PrintWriter(baseDir + File.separator
              + ".ibisense/apikey", "UTF-8");
          writer.println(masterKey);
          writer.close();
          if (Helpers.isWindowsOS()) {
            new File(baseDir + File.separator + ".ibisense/apikey")
                .setReadable(true, true);
            new File(baseDir + File.separator + ".ibisense/apikey")
                .setWritable(true, true);
          }
        }
      }

      try {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("AUID", auid);
        jsonObject.put("name", name);
        jsonObject.put("description", desc);
        jsonObject.put("version", version);
        jsonObject.put("url", appurl);
        jsonObject.put("is_owner", "true");
        jsonObject.put("group_wide", gp);
        jsonObject.put("owner_only", op);
        jsonObject.put("public", pp);

        int retries = 0;

        while (retries < MAX_RETRIES) {
          try {
            JSONObject result = updateApp(jsonObject, masterKey);
            PrintWriter writer = new PrintWriter(baseDir + File.separator
                + ".ibisense/app.raw.meta", "UTF-8");
            writer.println("AUID::" + result.get("AUID"));
            writer.println("name::" + result.get("name"));
            writer.println("description::" + result.get("description"));
            writer.println("version::" + result.get("version"));
            writer.println("url::" + "https://apps.ibisense.net/"
                + result.get("AUID") + "/");
            writer.println("is_onwer::" + result.get("is_onwer"));
            writer.println("group_wide::" + result.get("group_wide"));
            writer.println("owner_only::" + result.get("owner_only"));
            writer.println("public::" + result.get("public"));
            writer.close();
            break;
          } catch (ApplicationDoesNotExistException e) {
            System.out.println("Application does not exist");
            System.exit(-1);
          } catch (ServerErrorException e) {
            System.out.println("Server error. Retrying");
          } catch (IOException e) {
            System.out.println("Network error. Retrying");
          }
          retries++;
        }
        if (retries >= MAX_RETRIES) {
          System.out.println("Could not update app after " + MAX_RETRIES
              + " retries");

        }
      } catch (Exception e) {
        System.exit(-1);
      }

      Helpers.createZipFile(new File(workDir), new FileOutputStream(zipfileDir
          + File.separator + name + ".zip"));
      String url = BASE_URL
          + IBISENSE_DEPLOY_RESOURCE.replace("$MASTER_KEY", masterKey).replace(
              "$app_uid", auid);
      int response = Helpers.executeMultiPartRequest(url, new File(zipfileDir
          + File.separator + name + ".zip"), auid + ".zip");
      if (response == 200) {
        System.out.println("Current application version: " + version);
        System.out.println("The application was deployed at: " + appurl);
      } else {
        System.out.println("Please try again later (" + response + ")");
      }
      new File(zipfileDir + File.separator + name + ".zip").delete();
    } catch (ApplicationDoesNotExistException e) {
      System.out.println("Application does not exist");
    } catch (InvalidCredentialsException ce) {
      System.out.println("Invalid credentials");
    } catch (ServerErrorException se) {
      System.out.println("Server error: " + se.getMessage());
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public static void remove(String currentDir, String name,
      AppManagerConfiguration config) {
    try {
      String baseDir = currentDir + File.separator + (name != null ? name : "");
      String workDir = baseDir;
      String zipfileDir = currentDir;
      File theDir = new File(baseDir);
      if (name == null && new File(".ibisense").exists()) {
        workDir = currentDir;
        zipfileDir = new File(currentDir).getParent();
      } else {
        if (!theDir.exists()) {
          System.out
              .println("It seems like this is not an Ibisense application directory");
          System.exit(-1);
        }
        if (!new File(baseDir + File.separator + ".ibisense/app.raw.meta")
            .exists()) {
          System.out
              .println("It seems like this is not an Ibisense application directory");
          System.exit(-1);
        }
      }

      BufferedReader br = new BufferedReader(new FileReader(workDir
          + File.separator + ".ibisense/app.raw.meta"));
      String line;
      String auid = null;

      while ((line = br.readLine()) != null) {
        if (line.startsWith("AUID::")) {
          auid = line.replace("AUID::", "").trim();
          break;
        }
      }

      br.close();
      String masterKey = null;
      if (new File(baseDir + File.separator + ".ibisense/apikey").exists()) {
        br = new BufferedReader(new FileReader(baseDir + File.separator
            + ".ibisense/apikey"));
        masterKey = br.readLine();
        br.close();
      } else {
        masterKey = authenticate();
      }
      removeApp(masterKey, auid);
      new File(baseDir + File.separator + ".ibisense/apikey").delete();
      new File(baseDir + File.separator + ".ibisense/app.raw.meta").delete();
      System.out.println("Application was removed from the cloud");
    } catch (ApplicationDoesNotExistException e) {
      System.out.println("Application does not exist");
    } catch (InvalidCredentialsException ce) {
      System.out.println("Invalid credentials");
    } catch (ServerErrorException se) {
      System.out.println("Server error: " + se.getMessage());
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public static void update(String currentDir, String name,
      AppManagerConfiguration config) {

  }

  static String authenticate() throws ServerErrorException,
      InvalidCredentialsException, Exception {
    Console cons = System.console();
    String email = cons.readLine("%s ", "Email registered with Ibisense :");
    String pass = new String(cons.readPassword("%s ",
        "Your Ibisense password :"));

    try {
      URL obj = new URL(BASE_URL + IBISENSE_LOGIN_RESOURCE + "?email=" + email
          + "&password=" + pass);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {

        BufferedReader in = new BufferedReader(new InputStreamReader(
            con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }

        in.close();
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(response.toString());
        JSONObject data = (JSONObject) root.get("data");
        return data.get("master_key").toString();
      } else {
        throw new ServerErrorException("status_text:" + responseCode);
      }
    } catch (IOException e) {
      throw new ServerErrorException("status_text:" + e.getMessage());
    }
  }

  static void removeApp(String masterKey, String auid)
      throws ServerErrorException, InvalidCredentialsException, Exception {
    HttpURLConnection con = null;
    try {
      URL obj = new URL(BASE_URL
          + IBISENSE_REMOVE_RESOURCE.replace("$MASTER_KEY", masterKey).replace(
              "$app_uid", auid));
      con = (HttpURLConnection) obj.openConnection();

      if (con.getResponseCode() == HttpURLConnection.HTTP_OK)
        return;
      else if (con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
        throw new ApplicationDoesNotExistException("Application does not exist");
      else
        throw new ServerErrorException("Failed to remove application");

    } catch (IOException e) {
      throw new ServerErrorException("status_text:" + e.getMessage());
    } finally {
      if (con != null)
        con.disconnect();
    }
  }

  static JSONObject createApp(JSONObject json, String masterKey)
      throws ServerErrorException, InvalidCredentialsException, Exception {
    HttpURLConnection con = null;
    JSONObject data = null;
    try {
      URL obj = new URL(BASE_URL
          + IBISENSE_CREATE_RESOURCE.replace("$MASTER_KEY", masterKey));
      con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("POST");
      con.setDoOutput(true);
      OutputStream out = con.getOutputStream();
      JSONParser parser = new JSONParser();

      try {
        out.write(json.toString().getBytes(Charset.forName("UTF-8")));
      } catch (Exception e) {
        throw new ServerErrorException("Could not post JSON");
      }
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_CREATED) {

        BufferedReader in = new BufferedReader(new InputStreamReader(
            con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        JSONObject root = (JSONObject) parser.parse(response.toString());
        data = (JSONObject) root.get("data");
      } else {
        throw new ServerErrorException("status_text:" + responseCode);
      }
    } catch (IOException e) {
      throw new ServerErrorException("status_text:" + e.getMessage());
    } finally {
      if (con != null)
        con.disconnect();
    }
    return data;
  }

  static JSONObject updateApp(JSONObject json, String masterKey)
      throws ServerErrorException, InvalidCredentialsException, Exception {
    HttpURLConnection con = null;
    JSONObject data = null;
    try {
      URL obj = new URL(BASE_URL
          + IBISENSE_UPDATE_RESOURCE.replace("$MASTER_KEY", masterKey));
      con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("POST");
      con.setDoOutput(true);
      OutputStream out = con.getOutputStream();
      JSONParser parser = new JSONParser();

      try {
        out.write(json.toString().getBytes(Charset.forName("UTF-8")));
      } catch (Exception e) {
        throw new ServerErrorException("Could not post JSON");
      }
      int responseCode = con.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {

        BufferedReader in = new BufferedReader(new InputStreamReader(
            con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        JSONObject root = (JSONObject) parser.parse(response.toString());
        data = (JSONObject) root.get("data");
      } else if (con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
        throw new ApplicationDoesNotExistException("Application does not exist");
      } else {
        throw new ServerErrorException("status_text:" + responseCode);
      }
    } catch (IOException e) {
      throw new ServerErrorException("status_text:" + e.getMessage());
    } finally {
      if (con != null)
        con.disconnect();
    }
    return data;
  }

  @SuppressWarnings("serial")
  static class ServerErrorException extends Exception {
    public ServerErrorException() {
      super();
    }

    public ServerErrorException(String message) {
      super(message);
    }
  }

  @SuppressWarnings("serial")
  static class ApplicationDoesNotExistException extends Exception {
    public ApplicationDoesNotExistException() {
      super();
    }

    public ApplicationDoesNotExistException(String message) {
      super(message);
    }
  }

  @SuppressWarnings("serial")
  static class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException() {
      super();
    }

    public InvalidCredentialsException(String message) {
      super(message);
    }

  }
}
