package pt.ulisboa.tecnico.classes.classserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateRequest;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerClassServer.PropagateStateResponse;
import pt.ulisboa.tecnico.classes.contract.classserver.ClassServerServiceGrpc;
import java.util.Collections;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;

import static java.lang.Math.max;


public class ClassServerServiceImpl extends ClassServerServiceGrpc.ClassServerServiceImplBase{
    private boolean debug;
    private ActiveHolder activeHolder;
    private GossipHolder gossipHolder;
    private DomainClassState classState;
    private static final Logger LOGGER = Logger.getLogger(ClassServerServiceImpl.class.getName());

    public ClassServerServiceImpl(ActiveHolder activeHolder, boolean debug, DomainClassState classState) {
        setActiveHolder(activeHolder);
        setDebug(debug);
        setDomainClassState(classState);
        if (debug) {LOGGER.info("Created ClassServerServiceImpl.");}
    }

    public void setActiveHolder(ActiveHolder activeHolder) {this.activeHolder = activeHolder; }
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setDomainClassState(DomainClassState classState) {
        this.classState = classState;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        ResponseCode code;
        if (!activeHolder.isActive())
            code = ResponseCode.INACTIVE_SERVER;
        else {
            code = ResponseCode.OK;
            ClassState protoState = request.getClassState();
            int newCapacity = protoState.getCapacity();
            TimedEnrollments newTimedEnrollments = protoState.getOpenEnrollments();
            boolean newOpenEnrollments = newTimedEnrollments.getAreOpen();
            String newDateAdded = newTimedEnrollments.getDateOpenedClosed();
            List<ClassesDefinitions.Student> protoNewEnrolled = protoState.getEnrolledList();
            List<ClassesDefinitions.Student> protoNewDiscarded = protoState.getDiscardedList();
            int trueCapacity;
            boolean trueEnrollments;
            String trueDate;
            synchronized (classState) {
                if (newTimedEnrollments.getDateOpenedClosed().compareTo(classState.getDateOpenedClosed())>0){
                    trueEnrollments =  newOpenEnrollments;
                    trueDate = newDateAdded;
                    trueCapacity = newCapacity;
                }
                else {
                    trueEnrollments = classState.getOpenEnrollments();
                    trueDate = classState.getDateOpenedClosed();
                    trueCapacity = classState.getCapacity();
                }

                classState.setCapacity(trueCapacity);
                classState.setOpenEnrollments(trueEnrollments, trueDate);
                newEnrolled(classState.getEnrolled(),protoNewEnrolled,max(newCapacity,classState.getCapacity()));
                newDiscarded(classState.getDiscarded(), protoNewDiscarded);
                synchronizeStudents();



            }
        }
        PropagateStateResponse response = PropagateStateResponse.newBuilder().setCode(code).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (debug) LOGGER.info("PropagateStateResponse " + code + " sent!");
    }

    private void synchronizeStudents() {
        for(Map.Entry<String, DomainStudent> entry: classState.getEnrolled().entrySet()){
            if(classState.getDiscarded().containsKey(entry.getKey())){
                if(classState.getDiscarded().get(entry.getKey()).getDateAdded().compareTo(entry.getValue().getDateAdded())>0){
                    classState.getEnrolled().remove(entry.getKey());
                }
            }
        }

        for(Map.Entry<String, DomainStudent> entry: classState.getDiscarded().entrySet()){
            if(classState.getEnrolled().containsKey(entry.getKey())){
                if(classState.getEnrolled().get(entry.getKey()).getDateAdded().compareTo(entry.getValue().getDateAdded())>0){
                    classState.getDiscarded().remove(entry.getKey());
                }
            }
        }


    }

    public void newDiscarded(ConcurrentHashMap<String, DomainStudent> oldStudents, List<Student> protoStudents){
        List<DomainStudent> discarded = new ArrayList<>();
        for(Map.Entry<String, DomainStudent> entry: oldStudents.entrySet()){
            discarded.add(entry.getValue());
        }
        for(int i= 0; i<protoStudents.size(); i++){
            DomainStudent student = new DomainStudent(protoStudents.get(i).getStudentId(), protoStudents.get(i).getStudentName(),protoStudents.get(i).getDateAdded());
            discarded.add(student);
        }
        List<DomainStudent> discarded2 = discarded.stream().sorted(Comparator.comparing(e->e.getDateAdded())).distinct().collect(Collectors.toList());
       // Collections.reverse(discarded2);
        classState.getDiscarded().clear();
        for(int i =0; i<discarded2.size(); i++){
            classState.getDiscarded().put(discarded2.get(i).getStudentId(),discarded2.get(i));
        }
    }

    public void newEnrolled(ConcurrentHashMap<String, DomainStudent> oldStudents, List<Student> protoStudents , int newCapacity){
        List<DomainStudent> enrolled = new ArrayList<>();
        for(Map.Entry<String, DomainStudent> entry: oldStudents.entrySet()){
            enrolled.add(entry.getValue());
        }
        for(int i= 0; i<protoStudents.size(); i++){
            DomainStudent student = new DomainStudent(protoStudents.get(i).getStudentId(), protoStudents.get(i).getStudentName(),protoStudents.get(i).getDateAdded());
            enrolled.add(student);
        }
        List<DomainStudent> enrolled2 = enrolled.stream().sorted(Comparator.comparing(e->e.getDateAdded())).distinct().collect(Collectors.toList());
       // Collections.reverse(enrolled2);

        if(newCapacity<enrolled2.size()){
            classState.getEnrolled().clear();
            for(int j=0; j<newCapacity; j++){
                classState.getEnrolled().put(enrolled2.get(j).getStudentId(),enrolled2.get(j));
            }
            for(int i = newCapacity; i<enrolled2.size(); i++){
                classState.getDiscarded().put(enrolled2.get(i).getStudentId(),enrolled2.get(i));
            }
        }else{
            for(int i =0; i< enrolled.size(); i++){
                classState.getEnrolled().put(enrolled.get(i).getStudentId(),enrolled.get(i));
            }
        }
    }
}
