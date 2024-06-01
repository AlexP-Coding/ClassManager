package pt.ulisboa.tecnico.classes.classserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ClassState;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.TimedEnrollments;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import java.time.LocalDateTime;

public class DomainClassState {
    private int capacity = 0;
    private DomainTimedEnrollments openEnrollments = new DomainTimedEnrollments();
    private ConcurrentHashMap<String, DomainStudent> enrolled = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DomainStudent> discarded = new ConcurrentHashMap<>();

    public DomainClassState() {}

    public int getCapacity() { return capacity; }
    public boolean getOpenEnrollments() { return openEnrollments.isOpenEnrollments(); }
    public String getDateOpenedClosed() { return openEnrollments.getDateOpenedClosed();}
    public ConcurrentHashMap<String, DomainStudent> getEnrolled() { return enrolled; }
    public ConcurrentHashMap<String, DomainStudent> getDiscarded() { return discarded;}

    public synchronized void setCapacity(int capacity) { this.capacity = capacity; }
    public synchronized void setOpenEnrollments(boolean openEnrollments, String dateAdded) {
        this.openEnrollments.setOpenEnrollments(openEnrollments);
        this.openEnrollments.setDateOpenedClosed(dateAdded);
    }
    public synchronized void setEnrolled(ConcurrentHashMap<String, DomainStudent> enrolled) {this.enrolled = enrolled; }
    public synchronized void setDiscarded(ConcurrentHashMap<String, DomainStudent> discarded) {this.discarded = discarded; }

    public ResponseCode enroll(String studentId, String studentName) {
        String now = LocalDateTime.now().toString();
        if (!this.getOpenEnrollments()) {
            return ResponseCode.ENROLLMENTS_ALREADY_CLOSED;
        }
        else if (this.isEnrolled(studentId)) {
            this.getEnrolled().get(studentId).setDateAdded(LocalDateTime.now().toString());
            return ResponseCode.STUDENT_ALREADY_ENROLLED;
        }
        else if (this.getEnrolled().size() >= this.getCapacity()) {
            return ResponseCode.FULL_CLASS;
        }
        else {
            DomainStudent student;
            if (isDiscarded(studentId)) {
                student = this.getDiscarded().get(studentId);
                this.getDiscarded().remove(studentId);
                student.setDateAdded(now);
            }
            else
                student = new DomainStudent(studentId, studentName, now);

            this.getEnrolled().put(student.getStudentId(), student);

            return ResponseCode.OK;
        }
    }


    public boolean isValidStudentId(String studentId) {
        if (studentId == null || studentId.length() != 9) return false;

        String alunoId = studentId.substring(0,5);
        if (!alunoId.equals("aluno")) return false;

        String nrId = studentId.substring(5);
        try { Integer.parseInt(nrId); } catch (NumberFormatException e) { return false;}

        return true;
    }

    public boolean isValidStudentName(String studentName) {
        return studentName != null && studentName.length() >= 3 && studentName.length() <= 30;
    }

    public boolean isEnrolled(String studentId) { return enrolled.containsKey(studentId); }
    public boolean isDiscarded(String studentId) { return discarded.containsKey(studentId); }

    public ArrayList<Student> getProtoEnrolled() {
        ArrayList<Student> protoEnrolled = new ArrayList<>();
        for (DomainStudent domainStudent : enrolled.values()) {
            protoEnrolled.add(domainStudent.toProto());
        }
        return protoEnrolled;
    }

    public ArrayList<Student> getProtoDiscarded() {
        ArrayList<Student> protoDiscarded = new ArrayList<>();
        for (DomainStudent domainStudent : discarded.values()) {
            protoDiscarded.add(domainStudent.toProto());
        }
        return protoDiscarded;
    }

    public static ConcurrentHashMap<String, DomainStudent> protoStudentsToDomainStudents (List<Student> protoStudents) {
        ConcurrentHashMap<String, DomainStudent> domainStudents = new ConcurrentHashMap<>();
        int nrStudents = protoStudents.size();
        for (int i=0; i < nrStudents; i++) {
            Student protoStudent = protoStudents.get(i);
            DomainStudent student = new DomainStudent(protoStudent.getStudentId(), protoStudent.getStudentName(), protoStudent.getDateAdded());
            domainStudents.put(student.getStudentId(), student);
        }
        return domainStudents;
    }

    public ClassState toProto() {
        ClassState protoClassState;
        synchronized (this) {
            TimedEnrollments timedEnrollments = TimedEnrollments.newBuilder()
                    .setAreOpen(this.getOpenEnrollments())
                    .setDateOpenedClosed(this.getDateOpenedClosed())
                    .build();
            protoClassState = ClassState.newBuilder()
                    .setCapacity(this.capacity)
                    .setOpenEnrollments(timedEnrollments)
                    .addAllEnrolled(this.getProtoEnrolled())
                    .addAllDiscarded(this.getProtoDiscarded())
                    .build();
        }
        return protoClassState;
    }
}
