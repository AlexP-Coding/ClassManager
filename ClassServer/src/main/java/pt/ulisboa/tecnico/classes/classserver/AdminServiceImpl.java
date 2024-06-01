package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.*;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import java.util.logging.Logger;

public class AdminServiceImpl extends AdminServiceImplBase {
    private DomainClassState classState;
    private ClassServer classServer;
    private boolean debug;
    private static final Logger LOGGER = Logger.getLogger(AdminServiceImpl.class.getName());
    private NamingServerFrontend frontend = new NamingServerFrontend("localhost", 5000);

    public AdminServiceImpl(ClassServer classServer, DomainClassState classState) {
        setDomainClassState(classState);
        setDebug(debug);
        setClassServer(classServer);
        if (debug) {LOGGER.info("Created AdminServiceImpl.");}
    }

    public void setDomainClassState(DomainClassState classState){this.classState = classState;}
    public void setClassServer(ClassServer classServer) { this.classServer = classServer;}
    public void setDebug(boolean debug) { this.debug = debug; }

    @Override
    public void dump(DumpRequest request, StreamObserver<DumpResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        ClassState protoClassState = classState.toProto();
        DumpResponse response = DumpResponse.newBuilder().setCode(code).setClassState(protoClassState).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#dumped: " + code);
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        synchronized (classServer) {
            classServer.setActive(true);
        }
        ActivateResponse response = ActivateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#Activated: " + code);
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        synchronized (classServer) {
            classServer.setActive(false);
        }
        DeactivateResponse response = DeactivateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#Deactivated: " + code);
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        synchronized (classServer) {
            classServer.propagate(frontend, false, classServer.classHost, classServer.classPort);
        }
        GossipResponse response = GossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#Gossiped: " + code);
    }

    @Override
    public void activateGossip(ActivateGossipRequest request, StreamObserver<ActivateGossipResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        synchronized (classServer) {
            classServer.setGossip(true);
        }
        ActivateGossipResponse response = ActivateGossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#Gossip activated: " + code);
    }

    @Override
    public void deactivateGossip(DeactivateGossipRequest request, StreamObserver<DeactivateGossipResponse> responseObserver){
        ResponseCode code = ResponseCode.OK;
        synchronized (classServer) {
            classServer.setGossip(false);
        }
        DeactivateGossipResponse response = DeactivateGossipResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("#Gossip deactivated: " + code);
    }

}
