package co.edu.upb.grpc_storage_node.controller;

import com.example.grpc.HelloReply;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class HelloController extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        var message = "Hello, " + request.getName();
        var reply = HelloReply.newBuilder().setMessage(message).build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
