package pt.ulisboa.tecnico.classes.student;

import java.util.Scanner;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.ServerEntry;
import pt.ulisboa.tecnico.classes.NamingFrontend;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import io.grpc.Status;
import java.util.logging.Logger;

public class Student {
  private static final String EXIT_CMD = "exit";
  private static final String ENROLL_CMD = "enroll";
  private static final String LIST_CMD = "list";
  private static final String CLASS_SERVICE = "CLASS";
  private static final String QUALIFIER_PRIMARY_SERVER = "P";
  private static final String QUALIFIER_SECONDARY_SERVER = "S";

  public static ListClassResponse tryList(Logger logger, boolean debug, List<ClassesDefinitions.ServerEntry> protoAvailableServers, Map<ServerEntry, Integer> usedServers) {
    ServerEntry inUseServer = ServerEntry.chooseServerEntry(protoAvailableServers, usedServers);
    String classHost = inUseServer.getHost();
    int classPort = inUseServer.getPort();
    if (debug) logger.info("Host:" + classHost + ". Port:" + classPort);

    StudentFrontend classFrontend = new StudentFrontend(classHost, classPort);
    ListClassResponse response = classFrontend.listClass(ListClassRequest.getDefaultInstance());
    classFrontend.close();
    return response;
  }

  public static EnrollResponse tryEnroll(Logger logger, boolean debug, List<ClassesDefinitions.ServerEntry> protoAvailableServers, Map<ServerEntry, Integer> usedServers, String studentId, String studentName) {
    ServerEntry inUseServer = ServerEntry.chooseServerEntry(protoAvailableServers, usedServers);
    String classHost = inUseServer.getHost();
    int classPort = inUseServer.getPort();
    if (debug) logger.info("Host:" + classHost + "Port:" + classPort);

    StudentFrontend classFrontend = new StudentFrontend(classHost, classPort);
    ClassesDefinitions.Student student = ClassesDefinitions.Student.newBuilder().setStudentId(studentId).setStudentName(studentName).build();
    EnrollRequest request = EnrollRequest.newBuilder().setStudent(student).build();
    EnrollResponse response = classFrontend.enroll(request);
    classFrontend.close();
    return response;
  }

  public static void main(String[] args) {
    final String namingHost = "localhost";
    final int namingPort = 5000;
    final boolean debug;
    final Logger LOGGER = Logger.getLogger(Student.class.getName());

    List<String> qualifiers = new ArrayList<>();
    Map<ServerEntry, Integer> usedServers = new HashMap<ServerEntry, Integer>(); // map server entry to nr of times used

    String studentId = args[0];
    String studentName = args[1];

    int len;
    if (args[args.length-1].equals("-debug")){
      debug = true;
      len = args.length-1;
    }
    else {
      debug = false;
      len = args.length;
    }
    for (int i=2; i<len; i++) {
      studentName = studentName.concat(" ");
      studentName = studentName.concat(args[i]);
    }

    try (NamingFrontend namingFrontend = new NamingFrontend(namingHost, namingPort); Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.printf("%n> ");
        String line = scanner.nextLine();

        qualifiers.clear();

        // exit
        if (EXIT_CMD.equals(line)) {
          scanner.close();
          namingFrontend.close();
          break;
        }

        // enroll
        else if (ENROLL_CMD.equals(line)) {
          boolean reachedServer = false;
          boolean responseReceived = false;
          boolean requestAccepted = false;
          EnrollResponse response = null;
          for (int i = 0; (i < 2) && (requestAccepted == false); i++) {
            reachedServer = false;
            responseReceived = false;
            LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
            List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
            if (protoAvailableServers.size() == 0) {
              if (debug) LOGGER.info("No servers available");
              reachedServer = false;
              continue;
            }
            reachedServer = true;
            try {
              response = tryEnroll(LOGGER, debug, protoAvailableServers, usedServers, studentId, studentName);
              if (response.getCode().equals(ResponseCode.INACTIVE_SERVER)) {
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
            ResponseCode responseCode = response.getCode();
            System.out.println(Stringify.format(responseCode));
          }
        }

        // list
        else if (LIST_CMD.equals(line)) {
          boolean reachedServer = false;
          boolean responseReceived = false;
          boolean requestAccepted = false;
          ListClassResponse response = null;
          for (int i = 0; (i < 2) && (requestAccepted == false); i++) {
            reachedServer = false;
            responseReceived = false;
            LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
            List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
            if (protoAvailableServers.size() == 0) {
              if (debug) LOGGER.info("No servers available");
              reachedServer = false;
              continue;
            }
            reachedServer = true;
            try {
              response = tryList(LOGGER, debug, protoAvailableServers, usedServers);
              if (response.getCode().equals(ResponseCode.INACTIVE_SERVER)) {
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
            ResponseCode responseCode = response.getCode();
            if (responseCode.equals(ResponseCode.OK)) {
              ClassState state = response.getClassState();
              System.out.println(Stringify.format(state));
            }
            else if (responseCode.equals(ResponseCode.INACTIVE_SERVER) || responseCode.equals(ResponseCode.WRITING_NOT_SUPPORTED)) {
              System.out.println(Stringify.format(responseCode));
            }
          }
        }
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " +
              e.getStatus().getDescription());
    }
  }
}
