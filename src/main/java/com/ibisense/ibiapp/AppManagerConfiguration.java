package com.ibisense.ibiapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.io.IOUtils;

public class AppManagerConfiguration {

  Configuration config;

  static {
    javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
      new javax.net.ssl.X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(
            java.security.cert.X509Certificate[] certs, String authType) {
        }
      }
    };
    try {
      javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
          .getSocketFactory());
    } catch (Exception e) {
    }
  }

  public AppManagerConfiguration(String filename) throws IOException,
      FileNotFoundException, ParseException {
    config = new Configuration(filename);
  }

  public AppManagerConfiguration(InputStream in) throws IOException,
      FileNotFoundException, ParseException {
    config = new Configuration(in);
  }

  public List<Library> getListOfLibraries() {
    return config.libraries;
  }

  public List<Stylesheet> getListOfStylesheets() {
    return config.stylesheets;
  }

  public String getTemplate() {
    return config.template;
  }

  public String getJavascriptFolder() {
    return config.folders.jsFolder;
  }

  public String getCssFolder() {
    return config.folders.cssFolder;
  }

  public String getImageFolder() {
    return config.folders.imageFolder;
  }

  public String getIndexFile() {
    return config.folders.indexFile;
  }

  public boolean getStoreCredentials() {
    return config.storeCredentials;
  }

  class Configuration {
    /* Template configuration */
    public List<Library> libraries = new ArrayList<Library>();
    public List<Stylesheet> stylesheets = new ArrayList<Stylesheet>();
    public String template = "<html><body></body></html>";
    public FolderStructure folders;

    /* Application configuration */
    public boolean storeCredentials = false;

    public Configuration(String filename) throws IOException,
        FileNotFoundException, ParseException {
      this(new FileInputStream(filename));
    }

    public Configuration(InputStream in) throws IOException,
        FileNotFoundException, ParseException {
      JSONParser parser = new JSONParser();
      try {
        JSONObject root = (JSONObject) parser.parse(new InputStreamReader(in));
        /* Parse js libraries */
        JSONObject templateObj = (JSONObject) root.get("template");

        JSONArray libs = (JSONArray) templateObj.get("libraries");
        if (libs != null) {
          @SuppressWarnings("unchecked")
          Iterator<JSONObject> iter = (Iterator<JSONObject>) libs.iterator();
          while (iter.hasNext()) {
            JSONObject lib = iter.next();
            int order = Integer.parseInt(lib.get("include_order").toString());
            String name = (String) lib.get("name");
            String url = (String) lib.get("url");
            String libName = (String) lib.get("lib");
            addLibrary(order, name, url, libName);
          }
        }
        /* Parse css libraries */
        JSONArray css = (JSONArray) templateObj.get("stylesheets");
        if (css != null) {
          @SuppressWarnings("unchecked")
          Iterator<JSONObject> iter = css.iterator();
          while (iter.hasNext()) {
            JSONObject lib = iter.next();
            int order = Integer.parseInt(lib.get("include_order").toString());
            String name = (String) lib.get("name");
            String url = (String) lib.get("url");
            String libName = (String) lib.get("lib");
            addStylesheet(order, name, url, libName);
          }
        }

        /* Parse folder structure */
        JSONObject fs = (JSONObject) templateObj.get("folder_structure");
        if (fs != null) {
          folders = new FolderStructure((String) fs.get("javascripts"),
              (String) fs.get("stylesheets"), (String) fs.get("images"),
              (String) fs.get("index_file"));
        } else {
          folders = new FolderStructure();
        }

        if (templateObj.get("index_page") != null) {
          String templateFileName = templateObj.get("index_page").toString();
          File templateFile = new File(templateFileName);
          if (templateFile.exists()) {
            try {
              FileInputStream fisTargetFile = new FileInputStream(templateFile);
              template = IOUtils.toString(fisTargetFile, "UTF-8");
            } catch (Exception e) {
              try {

                template = IOUtils.toString(AppManagerConfiguration.class
                    .getResourceAsStream("/template.html"), "UTF-8");
              } catch (Exception ee) {
                ee.printStackTrace();
                System.exit(-1);
              }
            }
          } else {
            try {
              template = IOUtils.toString(AppManagerConfiguration.class
                  .getResourceAsStream("/template.html"), "UTF-8");
            } catch (Exception e) {
              e.printStackTrace();
              System.exit(-1);
            }
          }
        }

        JSONObject ibiapp = (JSONObject) root.get("ibiapp");
        if (ibiapp != null) {
          storeCredentials = (ibiapp.get("store_credentials") != null && ibiapp
              .get("store_credentials").toString().toLowerCase().equals("true")) ? true
              : false;
        }

      } catch (FileNotFoundException fe) {
        throw fe;
      } catch (IOException ioe) {
        throw ioe;
      } catch (ParseException pe) {
        throw pe;
      }
    }

    boolean addLibrary(int order, String name, String url, String lib) {
      return libraries.add(new Library(order, name, url, lib));
    }

    boolean addStylesheet(int order, String name, String url, String lib) {
      return stylesheets.add(new Stylesheet(order, name, url, lib));
    }
  }

  class FolderStructure {
    private final static String DEFAULT_JS_FOLDER = "js";
    private final static String DEFAULT_CSS_FOLDER = "css";
    private final static String DEFAULT_IMAGE_FOLDER = "img";
    private final static String DEFAULT_INDEX_FILE = "index.html";

    public String jsFolder = DEFAULT_JS_FOLDER;
    public String cssFolder = DEFAULT_CSS_FOLDER;
    public String imageFolder = DEFAULT_IMAGE_FOLDER;
    public String indexFile = DEFAULT_INDEX_FILE;

    public FolderStructure() {
    }

    public FolderStructure(String jsFolder, String cssFolder,
        String imageFolder, String indexFile) {
      if (isNotEmpty(jsFolder))
        this.jsFolder = jsFolder;
      if (isNotEmpty(cssFolder))
        this.cssFolder = cssFolder;
      if (isNotEmpty(imageFolder))
        this.imageFolder = imageFolder;
      if (isNotEmpty(indexFile))
        this.indexFile = indexFile;
    }

    private boolean isNotEmpty(String str) {
      if (str != null && !str.equals(""))
        return true;
      return false;
    }
  }

  static class Stylesheet {
    public int includeOrder;
    public String name;
    public String url;
    public String cssName;

    public Stylesheet(int includeOrder, String name, String url, String cssName) {
      this.name = name;
      this.url = url;
      this.includeOrder = includeOrder;
      this.cssName = cssName;
    }
  }

  static class Library {
    public int includeOrder;
    public String name;
    public String url;
    public String libName;

    public Library(int includeOrder, String name, String url, String libName) {
      this.name = name;
      this.url = url;
      this.includeOrder = includeOrder;
      this.libName = libName;
    }
  }

}
