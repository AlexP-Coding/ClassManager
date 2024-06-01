package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.*;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.naming.ClassServerNamingServer.*;
import pt.ulisboa.tecnico.classes.contract.naming.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.classes.NamingFrontend;

public class NamingServerFrontend extends NamingFrontend {

    public NamingServerFrontend(String host, int port) {
        super(host, port);
    }

    public RegisterResponse register(RegisterRequest request){
        return super.stub.register(request);
    }

    public DeleteResponse delete(DeleteRequest request){
        return super.stub.delete(request);
    }


}
