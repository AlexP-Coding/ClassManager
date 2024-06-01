package pt.ulisboa.tecnico.classes.professor;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.ServerEntry;
import pt.ulisboa.tecnico.classes.NamingFrontend;

import java.util.*;

import io.grpc.Status;

public class Professor {
  private static final String EXIT_CMD = "exit";
  private static final String OPEN_ENROLLMENTS_CMD = "openEnrollments";
  private static final String CLOSE_ENROLLMENTS_CMD = "closeEnrollments";
  private static final String LIST_CLASS_CMD = "list";
  private static final String CANCEL_ENROLLMENTS_CMD = "cancelEnrollment";

  private static final String CLASS_SERVICE = "CLASS";
  private static final String QUALIFIER_PRIMARY_SERVER = "P";



  public static ProfessorClassServer.OpenEnrollmentsResponse getOpenEnrollmentsResponse(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers, int capacity, boolean debug){
    ProfessorFrontend frontend = lookupNamingServer(namingFrontend, qualifiers,usedServers);
    ProfessorClassServer.OpenEnrollmentsResponse response = frontend.openEnrollments(ProfessorClassServer.OpenEnrollmentsRequest.newBuilder().setCapacity(capacity).build());
    if(debug){System.out.printf(Stringify.format(response.getCode()));}
    return response;
  }

  public static ProfessorClassServer.CloseEnrollmentsResponse getCloseEnrollmentsResponse(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers, boolean debug){
    ProfessorFrontend frontend = lookupNamingServer(namingFrontend, qualifiers,usedServers);
    ProfessorClassServer.CloseEnrollmentsResponse response = frontend.closeEnrollments(ProfessorClassServer.CloseEnrollmentsRequest.newBuilder().build());
    if(debug){System.out.printf(Stringify.format(response.getCode()));}
    return response;
  }

  public static ProfessorClassServer.ListClassResponse getListClassResponse(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers, boolean debug){
    ProfessorFrontend frontend = lookupNamingServer(namingFrontend, qualifiers,usedServers);
    ProfessorClassServer.ListClassResponse response = frontend.listClass(ProfessorClassServer.ListClassRequest.getDefaultInstance());
    if(debug){System.out.printf(Stringify.format(response.getCode()));}
    return response;
  }

  public static ProfessorClassServer.CancelEnrollmentResponse getCancelEnrollmentResponse(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers, String studentId, boolean debug){
    ProfessorFrontend frontend = lookupNamingServer(namingFrontend, qualifiers,usedServers);
    ProfessorClassServer.CancelEnrollmentResponse response = frontend.cancelEnrollment(ProfessorClassServer.CancelEnrollmentRequest.newBuilder().setStudentId(studentId).build());
    if(debug){System.out.printf(Stringify.format(response.getCode()));}
    return response;
  }

  public static List<ClassesDefinitions.ServerEntry> tryAgainLookup(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers){
    ProfessorFrontend frontend = null;
    LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
    List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
    return protoAvailableServers;
  }

  public static ProfessorFrontend createProfessorFrontend(List<ClassesDefinitions.ServerEntry> availableServers, Map<ServerEntry, Integer> usedServers){
    ProfessorFrontend frontend =null;
    ServerEntry inUseServer = ServerEntry.chooseServerEntry(availableServers, usedServers);
    String classHost = inUseServer.getHost();
    int classPort = inUseServer.getPort();
    frontend = new ProfessorFrontend(classHost, classPort);
    return frontend;
  }

  public static ProfessorFrontend lookupNamingServer(NamingFrontend namingFrontend, List<String> qualifiers, Map<ServerEntry, Integer> usedServers){
    ProfessorFrontend frontend = null;
    LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(CLASS_SERVICE).addAllQualifiers(qualifiers).build();
    List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
    List<ClassesDefinitions.ServerEntry> tryAgainProtoAvailableServers;
    if (protoAvailableServers.size() == 0) {
      tryAgainProtoAvailableServers = tryAgainLookup(namingFrontend,qualifiers,usedServers);
      if(tryAgainProtoAvailableServers.size()!=0){
        frontend = createProfessorFrontend(tryAgainProtoAvailableServers, usedServers);
      }
    }else{
      frontend=createProfessorFrontend(protoAvailableServers,usedServers);
    }
    return frontend;
  }

  public static void main(String[] args) {
    System.out.println(Professor.class.getSimpleName());
    final String namingHost = "localhost";
    final int namingPort = 5000;
    boolean debug;
    List<String> qualifiers = new ArrayList<>();
    Map<ServerEntry, Integer> usedServers = new HashMap<ServerEntry, Integer>(); // map server entry to nr of times used
    if ( args.length == 1 && args[0].equals("-debug")) {
      debug = true;
    }
    else {
      debug = false;
    }


    try(NamingFrontend namingFrontend = new NamingFrontend(namingHost, namingPort); Scanner scanner = new Scanner(System.in)) {
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
        }
        else if (OPEN_ENROLLMENTS_CMD.equals((cmd))) {
          qualifiers.add(QUALIFIER_PRIMARY_SERVER);
          ProfessorFrontend frontend = lookupNamingServer(namingFrontend, qualifiers,usedServers);
          if(frontend!=null) {
            try {
              ProfessorClassServer.OpenEnrollmentsResponse response = getOpenEnrollmentsResponse(namingFrontend,qualifiers,usedServers, Integer.parseInt(result[1]),debug);
              if(response.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                response = getOpenEnrollmentsResponse(namingFrontend,qualifiers,usedServers, Integer.parseInt(result[1]),debug);
              }
            } catch (StatusRuntimeException e) {
              if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
                try {
                  ProfessorClassServer.OpenEnrollmentsResponse response = getOpenEnrollmentsResponse(namingFrontend,qualifiers,usedServers, Integer.parseInt(result[1]),debug);
                }catch (StatusRuntimeException e1){
                  if(debug){System.out.printf("Caught exception with description:" + e.getStatus().getDescription());}
                }
              }
            } catch (ArrayIndexOutOfBoundsException e) {
              if(debug){System.out.printf("Have not got a capacity!");}
            }
            frontend.close();
          }
        }
        else if (CLOSE_ENROLLMENTS_CMD.equals((cmd))) {
          qualifiers.add(QUALIFIER_PRIMARY_SERVER);
          ProfessorFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          if(frontend!=null) {
            try {
              ProfessorClassServer.CloseEnrollmentsResponse response = getCloseEnrollmentsResponse(namingFrontend,qualifiers,usedServers,debug);
              if(response.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                response = getCloseEnrollmentsResponse(namingFrontend,qualifiers,usedServers,debug);
              }
              if(debug){System.out.printf(Stringify.format(response.getCode()));}
            } catch (StatusRuntimeException e) {
              if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
                try {
                  ProfessorClassServer.CloseEnrollmentsResponse response = getCloseEnrollmentsResponse(namingFrontend,qualifiers,usedServers,debug);
                }catch (StatusRuntimeException e1){
                  if(debug){System.out.printf("Caught exception with description:" + e.getStatus().getDescription());}
                }
              }
            }
            frontend.close();
          }
        }
        else if (LIST_CLASS_CMD.equals(cmd)) {
          ProfessorFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          if(frontend!=null) {
            try {
              ProfessorClassServer.ListClassResponse response = getListClassResponse(namingFrontend,qualifiers,usedServers,debug);
              if(response.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                response = getListClassResponse(namingFrontend,qualifiers,usedServers,debug);
                if (response.getCode().equals(ResponseCode.OK)) {
                  ClassState state = response.getClassState();
                  if(debug){System.out.printf(Stringify.format(state));}
                }else{
                  if(debug){System.out.printf(Stringify.format(response.getCode()));}
                }
              } else if (response.getCode().equals(ResponseCode.OK)) {
                ClassState state = response.getClassState();
                if(debug){System.out.printf(Stringify.format(state));}
              }else{
                if(debug){System.out.printf(Stringify.format(response.getCode()));}
              }
            } catch (StatusRuntimeException e) {
              if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
                if(debug){System.out.printf("The cause was a timeout exception. Try Again.");}
                try {
                  ProfessorClassServer.ListClassResponse response = getListClassResponse(namingFrontend,qualifiers,usedServers,debug);
                  if(debug){System.out.printf(Stringify.format(response.getCode()));}
                }catch (StatusRuntimeException e1){
                  if(debug){System.out.printf("Caught exception with description:" + e.getStatus().getDescription());}
                }
              }
            }
            frontend.close();
          }
        }
        else if (CANCEL_ENROLLMENTS_CMD.equals(cmd)) {
          qualifiers.add(QUALIFIER_PRIMARY_SERVER);
          ProfessorFrontend frontend = lookupNamingServer(namingFrontend,qualifiers,usedServers);
          if(frontend!=null) {
            try {
              ProfessorClassServer.CancelEnrollmentResponse response = getCancelEnrollmentResponse(namingFrontend,qualifiers,usedServers,result[1],debug);
              if(response.getCode().equals(ResponseCode.INACTIVE_SERVER)){
                response = getCancelEnrollmentResponse(namingFrontend,qualifiers,usedServers,result[1],debug);
              }
            } catch (StatusRuntimeException e) {
              if(Status.DEADLINE_EXCEEDED.getCode() == e.getStatus().getCode()){
                try{
                  ProfessorClassServer.CancelEnrollmentResponse response = getCancelEnrollmentResponse(namingFrontend,qualifiers,usedServers,result[1],debug);
                }catch (StatusRuntimeException e1){
                  if(debug){System.out.printf("Caught exception with description:" + e.getStatus().getDescription());}
                }
              }
            } catch (ArrayIndexOutOfBoundsException e) {
              if(debug){System.out.printf("Have not got a id student!");}
            }
            frontend.close();
          }

        }

      }
    } catch (StatusRuntimeException e) {
      if(debug){System.out.printf("Caught exception with description:" + e.getStatus().getDescription());}

    }
  }
}
