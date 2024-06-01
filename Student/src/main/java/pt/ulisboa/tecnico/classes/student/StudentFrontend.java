package pt.ulisboa.tecnico.classes.student;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.student.StudentServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.ListClassResponse;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollRequest;
import pt.ulisboa.tecnico.classes.contract.student.StudentClassServer.EnrollResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;

public class StudentFrontend implements AutoCloseable {
    private final ManagedChannel channel;
    private final StudentServiceGrpc.StudentServiceBlockingStub stub;

    public StudentFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        stub = StudentServiceGrpc.newBlockingStub(channel);
    }

    public ListClassResponse listClass(ListClassRequest request){
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).listClass(request);
    }

    public EnrollResponse enroll(EnrollRequest request) {
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).enroll(request);
    }

    @Override
    public final void close() { channel.shutdown(); }

}