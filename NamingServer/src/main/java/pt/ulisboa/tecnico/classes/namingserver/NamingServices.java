package pt.ulisboa.tecnico.classes.namingserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class NamingServices {
    private final ConcurrentHashMap<String, ServiceEntry> serviceEntries = new ConcurrentHashMap<>();

    public NamingServices(){
        addServiceEntry("CLASS");
    }

    public ConcurrentHashMap<String, ServiceEntry> getServiceEntries() {
        return serviceEntries;
    }

    public void addServiceEntry(String serviceName){
        serviceEntries.put(serviceName, new ServiceEntry(serviceName));
    }

    public void updateServiceEntry(String serviceName, ServerEntry serverEntry){
        serviceEntries.get(serviceName).updateEntryList(serverEntry);
    }

    public ServiceEntry getService(String serviceName) {
        return serviceEntries.get(serviceName);
    }

    public List<ServerEntry> filterQualifiers(String serviceName, List<String> qualifiers) {
        ServiceEntry serviceEntry = getService(serviceName);
        List<ServerEntry> filteredServerEntries;
        if (serviceEntry != null) // if service exists, filter through qualifiers
            filteredServerEntries = serviceEntry.filterQualifiers(qualifiers);
        else
            filteredServerEntries = new ArrayList<>();
        return filteredServerEntries;
    }
}
