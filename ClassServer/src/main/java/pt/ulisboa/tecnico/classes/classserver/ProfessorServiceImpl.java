package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;

import java.time.LocalDateTime;
import java.util.logging.Logger;

import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;
import  pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;

public class ProfessorServiceImpl extends ProfessorServiceGrpc.ProfessorServiceImplBase {
    private ActiveHolder activeHolder;
    private DomainClassState classState;
    private static final Logger LOGGER = Logger.getLogger(ProfessorServiceImpl.class.getName());
    private boolean debug;
    private String serverType;

    public ProfessorServiceImpl(ActiveHolder activeHolder, DomainClassState classState, boolean debug ,String serverType ) {
        setActiveHolder(activeHolder);
        setDomainClassState(classState);
        setDebug(debug);
        setServerType(serverType);
        if (debug) {LOGGER.info("Created ProfessorServiceImpl.");}
    }

    public void setActiveHolder(ActiveHolder activeHolder) { this.activeHolder = activeHolder;}
    public void setDomainClassState(DomainClassState classState) {
        this.classState = classState;
    }
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setServerType(String serverType) {this.serverType = serverType;}

    @Override
    public void openEnrollments(OpenEnrollmentsRequest request, StreamObserver<OpenEnrollmentsResponse> responseObserver) {
        String now = LocalDateTime.now().toString();
        ResponseCode code;
        int capacity = request.getCapacity();

        if(!activeHolder.isActive()) {
            code = ResponseCode.INACTIVE_SERVER;
        }else if(!serverType.equals("P")){
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } else{
            if (classState.getOpenEnrollments()) {
                code = ResponseCode.ENROLLMENTS_ALREADY_OPENED;
            } else if (capacity <= classState.getEnrolled().size())
                code = ResponseCode.FULL_CLASS;
            else {
                code = ResponseCode.OK;
                synchronized (this.classState) {
                    classState.setOpenEnrollments(true, now);
                    classState.setCapacity(capacity);
                }
            }
        }
        responseObserver.onNext(OpenEnrollmentsResponse.newBuilder().setCode(code).build());
        responseObserver.onCompleted();
        if(debug){LOGGER.info("openEnrollments: capacity="+ classState.getCapacity() + ". Code:" + code);}

    }

    @Override
    public void closeEnrollments(CloseEnrollmentsRequest request, StreamObserver<CloseEnrollmentsResponse> responseObserver){
        String now = LocalDateTime.now().toString();
        ResponseCode code;
        if(!activeHolder.isActive()) {
            code = ResponseCode.INACTIVE_SERVER;
        }else if(!serverType.equals("P")){
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        } else{
            if (!classState.getOpenEnrollments()) {
                code = ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
            } else {
                code = ResponseCode.OK;
                synchronized (this.classState) {
                    classState.setOpenEnrollments(false, now);
                }
            }
        }
        responseObserver.onNext(CloseEnrollmentsResponse.newBuilder().setCode(code).build());
        responseObserver.onCompleted();
        if(debug){LOGGER.info("closeEnrollments: capacity="+ classState.getCapacity() + ". Code=" + code);}
    }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        ResponseCode code;
        ClassState protoClassState = null;
        if (!activeHolder.isActive()) {
          code = ResponseCode.INACTIVE_SERVER;
        }
        else {
            code = ResponseCode.OK;
        }
        protoClassState = classState.toProto();

        ListClassResponse response = ListClassResponse.newBuilder().setCode(code).setClassState(protoClassState).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("listClass: " + code + " sent successfully");
    }

    public void cancelEnrollment(CancelEnrollmentRequest request, StreamObserver<CancelEnrollmentResponse> responseObserver){
        String now = LocalDateTime.now().toString();
        CancelEnrollmentResponse response;
        ResponseCode code;
        int nrEnrolled = -1;
        int nrDiscarded = -1;
        if(!activeHolder.isActive()) {
            code = ResponseCode.INACTIVE_SERVER;
        }
        else if(!serverType.equals("P")){
            code = ResponseCode.WRITING_NOT_SUPPORTED;
        }
        else{
            synchronized (this.classState) {
                if (!classState.isEnrolled(request.getStudentId())) {
                    code = ResponseCode.NON_EXISTING_STUDENT;
                }
                else {
                    DomainStudent studentDiscarded = classState.getEnrolled().get(request.getStudentId());
                    classState.getEnrolled().remove(request.getStudentId());
                    if (!classState.isDiscarded(request.getStudentId())) {
                        studentDiscarded.setDateAdded(now);
                        classState.getDiscarded().put(request.getStudentId(), studentDiscarded);
                    }
                    code = ResponseCode.OK;
                }
                nrEnrolled = classState.getEnrolled().size();
                nrDiscarded = classState.getDiscarded().size();
            }
        }
        response = CancelEnrollmentResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if(debug){LOGGER.info("cancelEnrollment:" + request.getStudentId() + ", code:" + code +". #Enrolled: " + nrEnrolled + ". #Discarded: " + nrDiscarded);}
    }
}
