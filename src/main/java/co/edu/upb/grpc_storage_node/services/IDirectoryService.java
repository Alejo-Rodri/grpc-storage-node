package co.edu.upb.grpc_storage_node.services;

import com.example.grpc.CreateDirectoryResponse;

import java.io.IOException;

public interface IDirectoryService {
    CreateDirectoryResponse createDirectory(String path) throws IOException;
}
