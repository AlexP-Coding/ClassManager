package pt.ulisboa.tecnico.classes.namingserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.RegisterResponse;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase{
  NamingServices namingServices = new NamingServices();
  boolean debug;
  private static final Logger LOGGER = Logger.getLogger(NamingServerServiceImpl.class.getName());

  public NamingServerServiceImpl(boolean debug) {
    this.debug = debug;
  }

  @Override
  public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver){
    String serviceName = request.getServiceName();
    String host = request.getHost();
    int port = request.getPort();
    List<String> qualifiers = request.getQualifiersList();
    ServerEntry serverEntry = new ServerEntry(host, port, qualifiers);

    synchronized (namingServices.getServiceEntries()) {
      if (namingServices.getServiceEntries().containsKey(serviceName)) {
        namingServices.updateServiceEntry(serviceName, serverEntry);
        if (debug) LOGGER.info("Server entry list currently at: " + namingServices.getService(serviceName).getServerEntryList());
      }
    }
    responseObserver.onNext(RegisterResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver){
    String serviceName = request.getServiceName();
    List<String> wantedQualifiers = request.getQualifiersList();
    List<ServerEntry> serverEntryList;
    ArrayList<ClassesDefinitions.ServerEntry> protoServerEntryList = new ArrayList<>();

    if (!namingServices.getServiceEntries().containsKey(serviceName))
      serverEntryList = new ArrayList<>();
    else {
      synchronized (namingServices.getService(serviceName)){
        if (wantedQualifiers.size() == 0)
          serverEntryList = namingServices.getService(serviceName).getServerEntryList();
        else {
          serverEntryList =
            namingServices.getService(serviceName).getServerEntryList().stream()
              .filter(serverEntry -> serverEntry.getQualifiers().equals(wantedQualifiers))
              .collect(Collectors.toList());
        }
        if (debug) LOGGER.info("Sending list: " + serverEntryList);
      }
    }

    for (int i = 0; i < serverEntryList.size(); i++) {
      ServerEntry domainServerEntry = serverEntryList.get(i);
      protoServerEntryList.add(domainServerEntry.toProto());
    }
    LookupResponse response = LookupResponse.newBuilder().addAllServerEntries(protoServerEntryList).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver){
    String serviceName = request.getServiceName();
    String host = request.getHost();
    int port = request.getPort();

    if (namingServices.getServiceEntries().containsKey(serviceName)) {
      synchronized (namingServices.getService(serviceName)) {
        namingServices
                .getService(serviceName)
                .getServerEntryList()
                .removeIf(e -> (e.getHost().equals(host) && e.getPort() == port));
        if (debug) LOGGER.info("Server entry list currently at: " + namingServices.getService(serviceName).getServerEntryList());
      }
    }
    DeleteResponse response = DeleteResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
