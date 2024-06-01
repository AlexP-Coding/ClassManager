package pt.ulisboa.tecnico.classes.classserver;

public class GossipHolder {
    private boolean isGossiping;

    public GossipHolder() {
        setGossip(true);
    }

    public void setGossip(boolean gossiping) {
        isGossiping = gossiping;
    }
    public boolean isGossiping() {return isGossiping;}
}
