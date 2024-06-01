package pt.ulisboa.tecnico.classes.classserver;

import java.time.LocalDateTime;

public class DomainTimedEnrollments {
  private boolean openEnrollments;
  private String dateOpenedClosed;

  public DomainTimedEnrollments() {
    setOpenEnrollments(false);
    setDateOpenedClosed(LocalDateTime.now().minusMonths(1).toString());
  }

  public void setOpenEnrollments(boolean openEnrollments) {
    this.openEnrollments = openEnrollments;
  }

  public void setDateOpenedClosed(String dateOpenedClosed) {
    this.dateOpenedClosed = dateOpenedClosed;
  }

  public boolean isOpenEnrollments() {
    return openEnrollments;
  }

  public String getDateOpenedClosed() {
    return dateOpenedClosed;
  }
}
