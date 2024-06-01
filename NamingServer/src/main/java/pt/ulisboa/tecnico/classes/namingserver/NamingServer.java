package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NamingServer {

  public NamingServer() {}


  public static void main(String[] args) {
    NamingServer namingServer = new NamingServer();
    System.out.println(NamingServer.class.getSimpleName());
    System.out.printf("Received %d Argument(s)%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("args[%d] = %s%n", i, args[i]);
    }
    String host = args[0];
    int port = Integer.parseInt(args[1]);
    boolean debug;

    if (args.length >=3 && args[2].equals("-debug"))
      debug = true;
    else
      debug = false;

    final BindableService namingServerImpl = new NamingServerServiceImpl(debug);

    Server server = ServerBuilder.forPort(port).addService(namingServerImpl).build();
    try{
      // Server threads are running in the background.
      System.out.println("Server started");
      server.start();
    }
    catch (java.io.IOException e){
      e.printStackTrace();
    }

    try{
      // Do not exit the main thread. Wait until server is terminated.
      server.awaitTermination();
    }
    catch (java.lang.InterruptedException e) {
      e.printStackTrace();
    }
  }
}
