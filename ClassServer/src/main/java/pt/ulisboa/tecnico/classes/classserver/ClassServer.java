package pt.ulisboa.tecnico.classes.classserver;

import java.util.Timer;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import java.util.ArrayList;
import java.util.TimerTask;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.Stringify;
import pt.ulisboa.tecnico.classes.ServerEntry;
import java.util.List;
import java.util.logging.Logger;
import java.lang.Math;

public class ClassServer {
  private boolean debug;
  private ActiveHolder activeHolder = new ActiveHolder();
  private GossipHolder gossipHolder = new GossipHolder();
  private DomainClassState classState;
  private ArrayList<String> qualifiers = new ArrayList<>();
  private static final Logger LOGGER = Logger.getLogger(ClassServer.class.getName());
  int classPort;
  String classHost;


  public ClassServer() {
    setActive(true);
    setGossip(true);
    setDomainClassState(new DomainClassState());
  }

  public void setActive(boolean isActive) { this.activeHolder.setActive(isActive); }
  public void setGossip(boolean isGossiping){this.gossipHolder.setGossip(isGossiping);}
  public boolean isActive() { return this.activeHolder.isActive(); }
  public boolean isGossiping() { return this.gossipHolder.isGossiping(); }
  public void setDomainClassState(DomainClassState classState) {
    this.classState = classState;
  }

  public void propagate(NamingServerFrontend namingFrontend, boolean debug, String host, int port) {
    List<String> wanted_qualifiers = new ArrayList<>();
    LookupRequest lookupRequest = LookupRequest.newBuilder()
                              .setServiceName("CLASS")
                              .addAllQualifiers(wanted_qualifiers)
                              .build();
    List<ClassesDefinitions.ServerEntry> protoAvailableServers = namingFrontend.lookup(lookupRequest).getServerEntriesList();
    if (protoAvailableServers.size() <= 1) {
      return;
    }
    List<ServerEntry> domainAvailableServers = ServerEntry.protoServersToDomain(protoAvailableServers);
    ServerEntry.removeOwnServerEntry(host, port, domainAvailableServers);
    int nrAvailableServers = domainAvailableServers.size();
    boolean reverseOrder = Math.random() > 0.5; // Randomly chooses to send message to servers in reverse order, as to not overwhelm them
    for (int i = 0; i < nrAvailableServers; i++) {
      ServerEntry inUseServer;
      if (reverseOrder)
        inUseServer = domainAvailableServers.get(nrAvailableServers-1-i);
      else
        inUseServer = domainAvailableServers.get(i);
      classHost = inUseServer.getHost();
      classPort = inUseServer.getPort();
      if (debug) { System.out.println("Propagating to {Host:" + classHost + ". Port:" + classPort + "}"); }

      ClassServerFrontend classFrontend = new ClassServerFrontend(classHost, classPort);
      ClassState protoClassState = classState.toProto();
      PropagateStateRequest request = PropagateStateRequest.newBuilder().setClassState(protoClassState).build();
      PropagateStateResponse response = classFrontend.propagateState(request);
      classFrontend.close();
      ResponseCode responseCode = response.getCode();
      System.out.println(Stringify.format(responseCode));
    }
  }

  public void run(String address, int port, NamingServerFrontend namingFrontend, ClassServer classes, boolean debug) {
    // Startup of server and its services
    final BindableService adminImpl = new AdminServiceImpl(this, this.classState);
    final BindableService serverImpl = new ClassServerServiceImpl(this.activeHolder, this.debug, this.classState);
    final BindableService studentImpl = new StudentServiceImpl(this.activeHolder, this.classState, this.debug);
    final BindableService professorImpl = new ProfessorServiceImpl(this.activeHolder, this.classState, this.debug, this.qualifiers.get(0));

    // Create a new server to listen on port.
    Server server = ServerBuilder.forPort(port).addService(serverImpl).addService(studentImpl).addService(professorImpl).addService(adminImpl).build();
    try{
      // Server threads are running in the background.
      server.start();
      Timer timer = new Timer();
      new java.util.Timer().schedule(new TimerTask(){
        @Override
        public void run() {
          if (classes.isGossiping() && classes.isActive())
            propagate(namingFrontend, debug, address, port);
        }
      },1000*40,1000*40);
    }
    catch (java.io.IOException e){
      e.printStackTrace();
    }
    catch (StatusRuntimeException e){
      e.printStackTrace();
    }
    System.out.println("Server started");

    try{
      // Do not exit the main thread. Wait until server is terminated.
      server.awaitTermination();
    }
    catch (java.lang.InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    System.out.println(ClassServer.class.getSimpleName());
    ClassServer classServer = new ClassServer();
    NamingServerFrontend namingFrontend = new NamingServerFrontend("localhost", 5000);
    final String host;
    final int port;
    final String type;

    host = args[0];
    port = Integer.parseInt(args[1]);
    if (args.length >= 3 && !args[2].equals("-debug"))
      type = args[2];
    else type = "P";

    if (args[args.length-1].equals("-debug")) // if final argument is debug flag
      classServer.debug = true;
    else
      classServer.debug = false;

    System.out.println("" +
            "host:" + host +
            ". port:" + port +
            ". type:" + type +
            ". debug:"+ classServer.debug);
    classServer.qualifiers.add(type);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("Shutting server down.");
        DeleteRequest request = DeleteRequest.newBuilder().setServiceName("CLASS").setHost(host).setPort(port).build();
        DeleteResponse response = namingFrontend.delete(request);
        namingFrontend.close();
        System.out.println("Server terminated!");
      }
    });

    // Register server in Naming Server
    RegisterRequest request = RegisterRequest.newBuilder().setServiceName("CLASS").setHost(host).setPort(port).addAllQualifiers(classServer.qualifiers).build();
    RegisterResponse response = namingFrontend.register(request);

    classServer.run(host, port, namingFrontend, classServer, classServer.debug);
  }
}