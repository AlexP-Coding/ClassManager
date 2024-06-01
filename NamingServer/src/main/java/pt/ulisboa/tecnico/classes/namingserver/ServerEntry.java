package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;
import java.util.ArrayList;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;

public class ServerEntry {
    String host;
    int port;
    List<String> qualifiers;

    public ServerEntry(String host, int port, List<String> qualifiers){
        setHost(host);
        setPort(port);
        setQualifiers(qualifiers);
    }

    public ClassesDefinitions.ServerEntry toProto() {
        ClassesDefinitions.ServerEntry serverEntry = ClassesDefinitions.ServerEntry.newBuilder()
                                                    .setPort(this.port)
                                                    .setHost(this.host)
                                                    .addAllQualifiers(this.qualifiers)
                                                    .build();
        return serverEntry;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setQualifiers(List<String> qualifiers) {
        this.qualifiers = qualifiers;
    }

    @Override
    public String toString() {
        return "{Host:" + host + ". Port:" + port + ". Qualifiers:" + qualifiers + "}";
    }
}
