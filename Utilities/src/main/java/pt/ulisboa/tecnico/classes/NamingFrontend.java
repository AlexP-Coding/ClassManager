package pt.ulisboa.tecnico.classes;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupRequest;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.LookupResponse;

public class NamingFrontend implements AutoCloseable {
  protected final ManagedChannel channel;
  protected final NamingServerServiceGrpc.NamingServerServiceBlockingStub stub;

  public NamingFrontend(String host, int port) {
    // Channel is the abstraction to connect to a service endpoint.
    // Let us use plaintext communication because we do not have certificates.
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

    // Create a blocking stub.
    stub = NamingServerServiceGrpc.newBlockingStub(channel);
  }

 public LookupResponse lookup(LookupRequest request){ return stub.lookup(request);}

  @Override
  public final void close() { channel.shutdown(); }

}