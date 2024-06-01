package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;

public class ClassServerFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final ClassServerServiceGrpc.ClassServerServiceBlockingStub stub;

    public ClassServerFrontend(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        stub = ClassServerServiceGrpc.newBlockingStub(channel);
    }

    public PropagateStateResponse propagateState(PropagateStateRequest request) { return stub.propagateState(request);}

    @Override
    public final void close() { channel.shutdown(); }
}
