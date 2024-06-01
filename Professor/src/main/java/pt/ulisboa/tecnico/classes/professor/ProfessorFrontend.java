package pt.ulisboa.tecnico.classes.professor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorClassServer.*;
import pt.ulisboa.tecnico.classes.contract.professor.ProfessorServiceGrpc;

import java.util.concurrent.TimeUnit;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ProfessorFrontend implements AutoCloseable{
    private final ManagedChannel channel;
    private final ProfessorServiceGrpc.ProfessorServiceBlockingStub stub;

    public ProfessorFrontend(String host, int port) {
        // Channel is the abstraction to connect to a service endpoint.
        // Let us use plaintext communication because we do not have certificates.
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create a blocking stub.
        stub = ProfessorServiceGrpc.newBlockingStub(channel);
    }

    public OpenEnrollmentsResponse openEnrollments(OpenEnrollmentsRequest request){
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).openEnrollments(request);
    }

    public CloseEnrollmentsResponse closeEnrollments(CloseEnrollmentsRequest request){
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).closeEnrollments(request);
    }

    public ListClassResponse listClass(ListClassRequest request){
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).listClass(request);
    }

    public CancelEnrollmentResponse cancelEnrollment(CancelEnrollmentRequest request){
        return stub.withDeadlineAfter(7, TimeUnit.SECONDS).cancelEnrollment(request);
    }


    @Override
    public final void close() { channel.shutdown(); }

}
