package com.ibisense.ibiapp;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import static java.util.Arrays.*;

import java.io.FileNotFoundException;

public class Main {

  final static String VERSION = Main.class.getPackage()
      .getImplementationVersion();

  public static void help() {
    System.out
        .println("usage: java -jar ibiapp.jar [--version] [--config config.json]");
    System.out.println("                            <command> [<args>]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("   -h, --help     Print this help message");
    System.out
        .println("   -v, --version  Print version number of this application");
    System.out
        .println("   -c, --config   JSON configuration file. If omitted a");
    System.out.println("                  default configuration will be used.");

    System.out.println();
    System.out.println("Available commands are:");
    System.out
        .println("   init     Initalizes application folder structure and");
    System.out.println("            deploys it initial version to the cloud.");
    System.out
        .println("   checkout Checks out existing application from the Ibisense cloud ");
    System.out.println("   deploy   Deploys application to the cloud while");
    System.out.println("            incrementing its version number.");
    System.out.println("   remove   Removes application from the cloud while");
    System.out.println("            keeping the local copy of the files.");
    System.out.println();
    System.out.println("See 'java -jar ibiapp.jar help <command>' for more");
    System.out.println("information on a specific command.");
    System.out.println();
  }

  public static void usage(String type) {
    if (type == null) {
      help();
      System.exit(-1);
    } else if (type.equals("init")) {
      System.out.println("init: Initalizes application folder structure");
      System.out.println("and deploys its initial version to the cloud");
      System.out.println();
      System.out.println("usage: init <application name>");
      System.out.println();
      System.out.println("<application name> parameter is mandatory.");
      System.out.println("After successful execution of this command");
      System.out.println("folder with <application name> will be");
      System.out.println("created under current working directory.");
      System.out.println();
      System.out.println("Example:");
      System.out.println("java -jar ibiapp.jar init exampleapp");
      System.exit(-1);
    } else if (type.equals("deploy")) {
      System.out.println("deploy: Deploys application to the cloud while");
      System.out.println("incrementing its version number");
      System.out.println();
      System.out.println("usage: deploy [<application name>]");
      System.out.println();
      System.out.println("<application name> parameter is optional");
      System.out.println("if command is executed from within a folder");
      System.out.println("of an inialized application. If, however,");
      System.out.println("the command is executed not from within the");
      System.out.println("application folder a name of the application");
      System.out.println("must be specified (Note a folder with such name");
      System.out.println("must exist under the current working directory)");
      System.out.println();
      System.out.println("Example:");
      System.out.println("java -jar ibiapp.jar deploy exampleapp");
      System.exit(-1);
    } else if (type.equals("checkout")) {
      System.out
          .println("checkout: Checks out application from the Ibisense cloud");
      System.out.println();
      System.out.println("usage: checkout [<application name>]");
      System.out.println();
      System.out.println("<application name> parameter is mandatory");
      System.out
          .println("if the application folder exists its contents will be overriden.");
      System.out.println();
      System.out.println("Example:");
      System.out.println("java -jar ibiapp.jar checkout exampleapp");
      System.exit(-1);
    } else if (type.equals("remove")) {
      System.out.println("remove: Removes application from the cloud while");
      System.out.println("keeping the local copy of the files");
      System.out.println();
      System.out.println("usage: remove [<application name>]");
      System.out.println();
      System.out.println("<application name> parameter is optional if");
      System.out.println("command is executed from within a folder of");
      System.out.println("an inialized application. If, however,");
      System.out.println("the command is executed not from within");
      System.out.println("the application folder a name of the application");
      System.out.println("must be specified (Note a folder with such name");
      System.out.println("must exist under the current working directory)");
      System.out.println();
      System.out.println("Example:");
      System.out.println("java -jar ibiapp.jar remove exampleapp");
      System.exit(-1);
    } else {
      help();
      System.exit(-1);
    }
  }

  public static void main(String[] args) {
    try {
      OptionParser parser = new OptionParser();
      parser.acceptsAll(asList("help", "h")).withOptionalArg()
          .ofType(String.class);
      parser.acceptsAll(asList("config", "c")).withRequiredArg()
          .ofType(String.class);
      parser.acceptsAll(asList("version", "v"));

      OptionSet options = parser.parse(args);
      String[] commands = new String[options.nonOptionArguments().size()];
      for (int i = 0; i < commands.length; i++)
        commands[i] = options.nonOptionArguments().get(i).toString();
      String filename = "config.json";
      AppManagerConfiguration config = null;
      String baseDirectory = System.getProperty("user.dir");

      if (options.has("v") || options.has("version")) {
        System.out.println("ibiapp.jar version " + VERSION);
        System.exit(0);
      }

      if (options.has("h")) {
        usage((String) options.valueOf("h"));
      }

      if (options.has("help")) {
        usage((String) options.valueOf("help"));
      }

      if (options.has("c")) {
        filename = (String) options.valueOf("c");
        try {
          config = new AppManagerConfiguration(filename);
        } catch (FileNotFoundException e) {
          System.out.println("Could not locate or load config.json");
          usage("general");
        } catch (Exception e) {
          System.out.println("Could not read or parse config.json");
          usage("general");
        }
      } else {
        config = new AppManagerConfiguration(Main.class.getResourceAsStream("/"
            + filename));
      }

      if (commands.length == 0) {
        usage("general");
      }

      if (commands[0].equals("help")) {
        if (commands.length == 1) {
          usage("general");
        }
        usage(commands[1]);
      } else if (commands[0].equals("init")) {
        if (commands.length == 1) {
          usage("init");
        }
        String appNmae = commands[1];
        AppManager.init(baseDirectory, appNmae, config);
      } else if (commands[0].equals("checkout")) {
        System.out.println("Not implemented");
        System.exit(0);
      } else if (commands[0].equals("deploy")) {
        if (commands.length == 1)
          AppManager.deploy(baseDirectory, null, config);
        else
          AppManager.deploy(baseDirectory, (String) commands[1], config);
      } else if (commands[0].equals("remove")) {
        if (commands.length == 1)
          AppManager.remove(baseDirectory, null, config);
        else
          AppManager.remove(baseDirectory, (String) commands[1], config);
      } else {
        help();
      }
    } catch (Exception e) {
      help();
    }
  }
}
