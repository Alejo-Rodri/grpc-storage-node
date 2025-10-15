package co.edu.upb.grpc_storage_node.services;

import com.example.grpc.CreateDirectoryResponse;
import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class DirectoryService implements IDirectoryService {

    @Override
    public CreateDirectoryResponse createDirectory(String path) throws IOException {
        String owner = System.getProperty("user.name");

        Path dirPath = Paths.get(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        LocalDateTime now = LocalDateTime.now();
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        Timestamp createdAt = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        return CreateDirectoryResponse.newBuilder()
                .setPath(dirPath.toString())
                .setOwner(owner)
                .setCreatedAt(createdAt)
                .build();
    }
}
