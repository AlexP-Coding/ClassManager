REPORT - Fault tolerance, coordination, consistency
================ 

*(Note: this solution allows for multiple servers being added and removed at will)*


ClassServer
------------
A **classState** now saves:
- The time at which enrollments were `closed/opened`
- The time at which each student was `enrolled/discarded`

A **classServer** propagates its classState every 40 seconds, to every other server but itself

This way, all servers will have the same information at the end of 40s (if they suffer no changes meanwhile or a server does not fail)

If a server fails to receive the classState, it will have once again an opportunity in 40 seconds.

The order through which the servers get the message (same as given by the NamingServer or reverse) is randomized, as to not overload a server when multiple are present

When another server receives its state, it:
```
- Considers the most recent open/closed enrollments state the real one
- Considers the most recent capacity the real one
- Accepts all students enrolled after enrollments were closed (it was not their fault the server was outdated)
- If a student is discarded in one classState and enrolled in another, only the most recent addition (to any map) remains
```

Client
------------------- 
To place a request, a client:
```
- Randomly chooses a server from the list received by the NamingServer (as to not overload a server when multiple are available) (see "Utilities/src/ServerEntry - chooseServerEntry")
- Sends its request to the server
- If the request was not successful (server does not support request, server took too long to respond, ...), the client tries again, at least once, randomly, to choose another server to send it to
```