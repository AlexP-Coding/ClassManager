package pt.ulisboa.tecnico.classes.classserver;

import static io.grpc.Status.INVALID_ARGUMENT;
import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.*;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase{
    private ActiveHolder activeHolder;
    private DomainClassState classState;
    private boolean debug;
    private static final Logger LOGGER = Logger.getLogger(StudentServiceImpl.class.getName());

    public StudentServiceImpl(ActiveHolder activeHolder, DomainClassState classState, boolean debug) {
        setActiveHolder(activeHolder);
        setDomainClassState(classState);
        setDebug(debug);
        if (debug) {LOGGER.info("Created StudentServiceImpl.");}
    }

    public void setActiveHolder(ActiveHolder activeHolder) { this.activeHolder = activeHolder;}

    public void setDomainClassState(DomainClassState classState) {
        this.classState = classState;
    }
    public void setDebug(boolean debug) { this.debug = debug; }

    @Override
    public void listClass(ListClassRequest request, StreamObserver<ListClassResponse> responseObserver) {
        ResponseCode code;
        ClassState protoClassState = null;
        if (!activeHolder.isActive())
            code = ResponseCode.INACTIVE_SERVER;
        else {
            code = ResponseCode.OK;
        }
        protoClassState = classState.toProto();

        ListClassResponse response = ListClassResponse.newBuilder().setCode(code).setClassState(protoClassState).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("listClass: " + code + " sent successfully");
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        Student protoStudent = request.getStudent();
        String studentId = protoStudent.getStudentId();
        String studentName = protoStudent.getStudentName();
        int nrEnrolled = -1;
        int nrDiscarded = -1;

        if (!classState.isValidStudentId(studentId))
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input " + studentId + "is not a valid student id!").asRuntimeException());
        else if (!classState.isValidStudentName(studentName))
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Input " + studentName + "is not a valid student name!").asRuntimeException());
        else {
            ResponseCode code;
            if (!activeHolder.isActive()) {
                code = ResponseCode.INACTIVE_SERVER;
            }
            else
                synchronized (classState) {
                    code = classState.enroll(studentId, studentName);
                    nrEnrolled = classState.getEnrolled().size();
                    nrDiscarded = classState.getDiscarded().size();
                }

            EnrollResponse response = EnrollResponse.newBuilder().setCode(code).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            if (debug) LOGGER.info("enroll: Code=" + code + ". "  + studentId + ": " + studentName
                    + ". #Enrolled: " + nrEnrolled + ". #Discarded: " + nrDiscarded);
        }
    }

}