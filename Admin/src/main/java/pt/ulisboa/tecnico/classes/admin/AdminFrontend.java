package pt.ulisboa.tecnico.classes.admin;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;

public class AdminFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final AdminServiceGrpc.AdminServiceBlockingStub stub;

    public AdminFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public DumpResponse dump(DumpRequest request){ return stub.dump(request); }
    public ActivateResponse activate(ActivateRequest request){return stub.activate(request);}
    public DeactivateResponse deactivate(DeactivateRequest request){return  stub.deactivate(request);}
    public GossipResponse gossip(GossipRequest request){return  stub.gossip(request);}
    public ActivateGossipResponse activateGossip(ActivateGossipRequest request){return  stub.activateGossip(request);}
    public DeactivateGossipResponse deactivateGossip(DeactivateGossipRequest request){return  stub.deactivateGossip(request);}

    @Override
    public final void close() { channel.shutdown(); }

}