package pt.ulisboa.tecnico.classes.classserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;

public class DomainStudent {
  private String studentId;
  private String studentName;
  private String dateAdded;

  public DomainStudent(String studentId, String studentName, String dateAdded) {
    setStudentId(studentId);
    setStudentName(studentName);
    setDateAdded(dateAdded);
  }

  public void setStudentId(String studentId) { this.studentId = studentId; }
  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }
  public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

  public String getStudentId() { return studentId; }
  public String getStudentName() { return studentName; }
  public String getDateAdded() { return dateAdded; }

  public Student toProto() {
    Student protoStudent = Student.newBuilder()
            .setStudentId(this.studentId)
            .setStudentName(this.studentName)
            .setDateAdded(this.dateAdded)
            .build();
    return protoStudent;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DomainStudent))
      return false;

    DomainStudent other = (DomainStudent) o;

    return this.studentId.equals(other.getStudentId())
            && this.studentName.equals(other.getStudentName());
  }

  @Override
  public int hashCode() {
    return this.studentName.hashCode() * this.studentId.hashCode() ;
  }

  @Override
  public String toString() {
    return "Id:" + studentId + ", Name:" + studentName + ", DateAdded:" + dateAdded;
  }

}