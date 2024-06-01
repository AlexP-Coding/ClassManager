# ClassManager - Turmas

Class management system for the course of Distributed Systems @ist. 

This project was implemented using gRPC and gossip for communication protocols.

## Authors

**Group A06**

### Code Identification

### Team Members

| Name           | User                              |
|----------------|-----------------------------------|
| Jiaqi Yu       | <https://github.com/jiaqiyusun>   |
| André Matos    | <https://github.com/andrem000>    |
| Alexandra Pato | <https://github.com/AlexP-Coding> |

## Getting Started

The overall system is made up of several modules. The main server is the _ClassServer_. The clients are the _Student_,
the _Professor_ and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement (PT)](ProjectStatement.md) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you can too, just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install compile
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.


## To run processes
Turn on debug mode by adding ```-debug ``` at the END of the list of args

Debug mode is always on when using the default ```mvn exec:java ```

NamingServer should always be set on localhost port 5000


### NamingServer
```s
mvn exec:java -Dexec.args="localhost 5000"
```

### ClassServer
```s
mvn exec:java -Dexec.args="host port serverType"
```
If no serverType is specified, default is ```P```

### Student
```s
mvn exec:java -Dexec.args="alunoXXXX StudentName"
```

### Professor
```s
mvn exec:java -Dexec.args=""
```

### Admin
```s
mvn exec:java -Dexec.args=""
```