package pt.ulisboa.tecnico.classes.admin;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.NamingFrontend;
import pt.ulisboa.tecnico.classes.ServerEntry;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer;

import java.util.*;
import java.util.logging.Logger;

public class Admin {

  private static final String DUMP_CMD = "dump";
  private static final String EXIT_CMD = "exit";
  private static final String ACTIVATE_CMD = "activate";
  private static final String DEACTIVATE_CMD = "deactivate";
  private static final String GOSSIP_CMD = "gossip";
  private static final String ACTIVATE_GOSSIP_CMD = "activateGossip";
  private static final String DEACTIVATE_GOSSIP_CMD = "deactivateGossip";

  private static final String CLASS_SERVICE = "CLASS";

  public static DumpResponse tryDump(Logger logger, boolean debug, List<ClassesDefinitions.ServerEntry> protoAvailableServers, Map<ServerEntry, Integer> usedServers) {
    ServerEntry inUseServer = ServerEntry.chooseServerEntry(protoAvailableServers, usedServers);
    String classHost = inUseServer.getHost();
    int classPort = inUseServer.getPort();
    if (debug) logger.info("Host:" + classHost + ". Port:" + classPort);

    AdminFrontend classFrontend = new AdminFrontend(classHost, classPort);
    DumpResponse response = classFrontend.dump(DumpRequest.getDefaultInstance());
    classFrontend.close();
    return response;
  }

  public static AdminFrontend lookupNamingServer(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers){
    AdminFrontend frontend = null;
    ClassServerNamingServer.LookupRequest lookupRequest = ClassServerNamingServer.LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
    List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
    if (protoAvailableServers.size() != 0) {
      ServerEntry inUseServer = ServerEntry.chooseServerEntry(protoAvailableServers, usedServers);
      String  classHost = inUseServer.getHost();
      int classPort = inUseServer.getPort();
      frontend = new AdminFrontend(classHost, classPort);
    }
    return frontend;
  }

  public static void main(String[] args) {
    System.out.println(Admin.class.getSimpleName());
    final String namingHost = "localhost";
    final int namingPort = 5000;
    final boolean debug;
    final Logger LOGGER = Logger.getLogger(Admin.class.getName());

    List<String> qualifiers = new ArrayList<>();
    Map<ServerEntry, Integer> usedServers = new HashMap<ServerEntry, Integer>(); // map server entry to nr of times used

    if(args.length == 1){
      debug = true;
    }
    else{
      debug = false;
    }

    try (NamingFrontend namingFrontend = new NamingFrontend(namingHost, namingPort); Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.printf("%n> ");

        qualifiers.clear();

        String[] result = scanner.nextLine().split(" ");
        String cmd = result[0];

        // exit
        if (EXIT_CMD.equals(cmd)) {
          scanner.close();
          namingFrontend.close();
          break;
          // dump
        }
        else if (DUMP_CMD.equals(cmd)) {
          boolean reachedServer = false;
          boolean responseReceived = false;
          boolean requestAccepted = false;
          DumpResponse response = null;
          for (int i = 0; (i < 2) && (requestAccepted == false); i++) {
            reachedServer = false;
            responseReceived = false;
            ClassServerNamingServer.LookupRequest lookupRequest = ClassServerNamingServer.LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
            List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
            if (protoAvailableServers.size() == 0) {
              if (debug) LOGGER.info("No servers available");
              reachedServer = false;
              continue;
            }
            reachedServer = true;
            try {
              response = tryDump(LOGGER, debug, protoAvailableServers, usedServers);
              if (response.getCode().equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER)) {
                if (debug) LOGGER.info("Inactive server");
                responseReceived = true;
                requestAccepted = false;
                continue;
              }
              else {
                responseReceived = true;
                requestAccepted = true;
              }
            } catch (StatusRuntimeException e) {
              if (Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()) { // If the timeout time has expired
                if (debug) LOGGER.info("Timeout");
                responseReceived = false;
                continue;
              }
            }
          }
          if (reachedServer && responseReceived) {
            ClassesDefinitions.ResponseCode responseCode = response.getCode();
            if (responseCode.equals(ClassesDefinitions.ResponseCode.OK)) {
              ClassesDefinitions.ClassState state = response.getClassState();
              System.out.println(Stringify.format(state));
            }
            else if (responseCode.equals(ClassesDefinitions.ResponseCode.INACTIVE_SERVER) || responseCode.equals(ClassesDefinitions.ResponseCode.WRITING_NOT_SUPPORTED)) {
              System.out.println(Stringify.format(responseCode));
            }
          }
        }
        else if(ACTIVATE_CMD.equals(cmd)){
          if(result.length == 2)
            qualifiers.add(result[1]);
          else
            qualifiers.add("P");
          AdminFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          ActivateResponse response = frontend.activate(ActivateRequest.getDefaultInstance());
          ClassesDefinitions.ResponseCode responseCode = response.getCode();
          System.out.println(Stringify.format(responseCode));
          frontend.close();
          if (debug) LOGGER.info("Activated");
        }
        else if(DEACTIVATE_CMD.equals(cmd)){
          if(result.length == 2)
            qualifiers.add(result[1]);
          else
            qualifiers.add("P");
          AdminFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          DeactivateResponse response = frontend.deactivate(DeactivateRequest.getDefaultInstance());
          ClassesDefinitions.ResponseCode responseCode = response.getCode();
          System.out.println(Stringify.format(responseCode));
          frontend.close();
          if (debug) LOGGER.info("Deactivated");
        }
        else if(GOSSIP_CMD.equals(cmd)){
          if(result.length == 2)
            qualifiers.add(result[1]);
          else
            qualifiers.add("P");
          AdminFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          GossipResponse response = frontend.gossip(GossipRequest.getDefaultInstance());
          ClassesDefinitions.ResponseCode responseCode = response.getCode();
          System.out.println(Stringify.format(responseCode));
          frontend.close();
          if (debug) LOGGER.info("Gossiped");
        }
        else if(ACTIVATE_GOSSIP_CMD.equals(cmd)){
          if(result.length == 2)
            qualifiers.add(result[1]);
          else
            qualifiers.add("P");
          AdminFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          ActivateGossipResponse response = frontend.activateGossip(ActivateGossipRequest.getDefaultInstance());
          ClassesDefinitions.ResponseCode responseCode = response.getCode();
          System.out.println(Stringify.format(responseCode));
          frontend.close();
          if (debug) LOGGER.info("Activated gossip");
        }
        else if(DEACTIVATE_GOSSIP_CMD.equals(cmd)){
          if(result.length == 2)
            qualifiers.add(result[1]);
          else
            qualifiers.add("P");
          AdminFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          DeactivateGossipResponse response = frontend.deactivateGossip(DeactivateGossipRequest.getDefaultInstance());
          ClassesDefinitions.ResponseCode responseCode = response.getCode();
          System.out.println(Stringify.format(responseCode));
          frontend.close();
          if (debug) LOGGER.info("Deactivated gossip");
        }
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " +
              e.getStatus().getDescription());
    }
  }
}

