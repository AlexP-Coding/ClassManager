package pt.ulisboa.tecnico.classes.namingserver;

import java.util.List;
import java.util.ArrayList;

public class ServiceEntry {
    String serviceName;
    List<ServerEntry> serverEntryList = new ArrayList<>();

    public ServiceEntry(String serviceName){
        setServiceName(serviceName);
    }

    public void setServiceName(String serviceName){
        this.serviceName = serviceName;
    }

    public String getServiceName(){
        return this.serviceName;
    }

    public List<ServerEntry> getServerEntryList(){
        return this.serverEntryList;
    }

    public void updateEntryList(ServerEntry serverEntry){
        this.serverEntryList.add(serverEntry);
    }

    public List<ServerEntry> filterQualifiers(List<String> qualifiers) {
        List<ServerEntry> filteredServerEntryList = new ArrayList<>();
        int nrServerEntries = serverEntryList.size();
        int nrQualifiers = qualifiers.size();

        for (int i = 0; i < nrServerEntries; i++) {
            boolean hasQualifiers = true;
            for (int j = 0; j < nrQualifiers; j++) {
                if (!serverEntryList.get(i).getQualifiers().contains(qualifiers.get(j))) {
                    hasQualifiers = false;
                    break;
                }
            }
            if (hasQualifiers)
                filteredServerEntryList.add(serverEntryList.get(i));
        }

        return filteredServerEntryList;
    }
}
