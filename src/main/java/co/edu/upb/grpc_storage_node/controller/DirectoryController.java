package co.edu.upb.grpc_storage_node.controller;

import co.edu.upb.grpc_storage_node.services.IDirectoryService;
import com.example.grpc.CreateDirectoryRequest;
import com.example.grpc.CreateDirectoryResponse;
import com.example.grpc.DirectoryServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;

@GrpcService
@AllArgsConstructor
public class DirectoryController extends DirectoryServiceGrpc.DirectoryServiceImplBase {
    private final IDirectoryService iDirectoryService;

    @Override
    public void createDirectory(
            CreateDirectoryRequest request,
            StreamObserver<CreateDirectoryResponse> responseObserver
    ) {
        CreateDirectoryResponse reply = null;
        try {
            reply = iDirectoryService.createDirectory(request.getPath());
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error creating directory: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
