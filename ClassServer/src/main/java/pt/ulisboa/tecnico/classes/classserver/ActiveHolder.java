package pt.ulisboa.tecnico.classes.classserver;

public class ActiveHolder {
  private boolean isActive;

  public ActiveHolder() {
     setActive(true);
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public boolean isActive() {
    return isActive;
  }
}
