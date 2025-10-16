package co.edu.upb.grpc_storage_node.controller;

import com.example.grpc.FileServiceGrpc;
import com.example.grpc.UploadFileRequest;
import com.example.grpc.UploadFileResponse;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@GrpcService
public class FileController extends FileServiceGrpc.FileServiceImplBase {
    private static final String STORAGE_PATH = "\\Users\\arodr\\Documents";

    public void uploadFile(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        try {
            Path path = Paths.get(STORAGE_PATH);
            Files.write(path, request.getFileData().toByteArray());
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Archivo guardado en: " + path)
                    .build());
        } catch (IOException e) {
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
        }
        responseObserver.onCompleted();
    }
}
