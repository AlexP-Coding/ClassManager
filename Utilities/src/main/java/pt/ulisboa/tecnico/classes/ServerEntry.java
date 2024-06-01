package pt.ulisboa.tecnico.classes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Math;

import io.grpc.Server;
import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions;


public class ServerEntry {
  String host;
  int port;
  List<String> qualifiers;

  public ServerEntry() {}

  public ServerEntry(String host, int port, List<String> qualifiers){
    setHost(host);
    setPort(port);
    setQualifiers(qualifiers);
  }

  public static void protoServerEntryToDomain(ClassesDefinitions.ServerEntry protoServerEntry, ServerEntry serverEntry) {
    serverEntry.setHost(protoServerEntry.getHost());
    serverEntry.setPort(protoServerEntry.getPort());
    serverEntry.setQualifiers(protoServerEntry.getQualifiersList());
  }

  public static void removeOwnServerEntry(String igrnHost, int ignrPort, List<ServerEntry> domainAvailableServers) {
    for (int i = 0; i < domainAvailableServers.size(); i++) {
      ServerEntry domainServerEntry = domainAvailableServers.get(i);
      String serverHost = domainServerEntry.getHost();
      int serverPort = domainServerEntry.getPort();
      if (serverHost.equals(igrnHost) && serverPort == ignrPort) {
        domainAvailableServers.remove(i);
        break;
      }
    }
  }

  public static List<ServerEntry> protoServersToDomain(List<ClassesDefinitions.ServerEntry> protoServerEntries) {
    List<ServerEntry> domainServers = new ArrayList<>();
    for (int i = 0; i < protoServerEntries.size(); i++) {
      ServerEntry domainServerEntry = new ServerEntry();
      ServerEntry.protoServerEntryToDomain(protoServerEntries.get(i), domainServerEntry);
      domainServers.add(domainServerEntry);
    }
    return domainServers;
  }

  public static ServerEntry chooseServerEntry(List<ClassesDefinitions.ServerEntry> protoAvailableServers, Map<ServerEntry, Integer> usedServers) {
    List<ServerEntry> domainAvailableServers = protoServersToDomain(protoAvailableServers);
    return chooseDomainServerEntry(domainAvailableServers, usedServers);
  }

  private static ServerEntry chooseDomainServerEntry(List<ServerEntry> domainAvailableServers, Map<ServerEntry, Integer> usedServers) {
    List<ServerEntry> leastUsedServers = new ArrayList<>();

    int minTimesUsed = 1;
    if (usedServers.size() != 0) {
      List<Integer> timesUsedList = new ArrayList<Integer>(usedServers.values());
      minTimesUsed = timesUsedList.get(0);
    }

    int nrAvailableServers = domainAvailableServers.size();

    for (int i=0; i < nrAvailableServers; i++) {
      ServerEntry availableServer = domainAvailableServers.get(i);
      if (usedServers.containsKey(availableServer)) {
        int nrTimesUsed = usedServers.get(availableServer);
        if (nrTimesUsed == minTimesUsed) {
          leastUsedServers.add(availableServer);
        }
        else if (nrTimesUsed < minTimesUsed) {
          leastUsedServers.clear();
          minTimesUsed = nrTimesUsed;
          leastUsedServers.add(availableServer);
        }
      }
      else {
        if (minTimesUsed > 0) {
          minTimesUsed = 0;
          leastUsedServers.clear();
        }
        leastUsedServers.add(availableServer);
      }
    }

    int leastUsedIndex;
    if (leastUsedServers.size() == 1)
      leastUsedIndex = 0;
    else
      leastUsedIndex = (int) Math.round(Math.random()*(leastUsedServers.size()-1));

    ServerEntry inUseServer = leastUsedServers.get(leastUsedIndex);
    return inUseServer;
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
  public boolean equals(Object o) {
    if (!(o instanceof ServerEntry))
      return false;

    ServerEntry other = (ServerEntry) o;
    boolean sameQualifiers = true;
    if (this.qualifiers.size() != other.getQualifiers().size())
      sameQualifiers = false;
    else {
      for (int i=0; i < this.qualifiers.size(); i++) {
        if (!(this.qualifiers.get(i).equals(other.getQualifiers().get(i)))) {
          sameQualifiers = false;
          break;
        }
      }
    }
    return this.host.equals(other.getHost())
            && this.port == other.getPort()
            && sameQualifiers == true;
  }

  @Override
  public int hashCode() {
    int multi = 31;
    int result = 17;
    result = multi * result + this.host.length();
    result = multi * result + this.port;
    int nrQualifiers = this.qualifiers.size();
    for (int i=0; i< nrQualifiers; i++)
      result = multi * result + this.qualifiers.get(i).length();
    return result;
  }
}
