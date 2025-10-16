package co.edu.upb.grpc_storage_node.controller;

import com.example.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.nio.file.*;

@GrpcService
@Slf4j
public class FileNodeController extends FileNodeServiceGrpc.FileNodeServiceImplBase {

    @Value("${node.storage.root:}")
    private String storageRootProp;

    private Path getRoot() {
        if (storageRootProp == null || storageRootProp.isBlank()) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        return Paths.get(storageRootProp).toAbsolutePath().normalize();
    }

    private boolean isInsideRoot(Path p, Path root) {
        Path np = p.toAbsolutePath().normalize();
        return np.startsWith(root);
    }

    private Path resolveUnderRoot(String maybeRelative, Path root) {
        Path p = Paths.get(maybeRelative);
        if (!p.isAbsolute()) {
            p = root.resolve(p);
        }
        return p.toAbsolutePath().normalize();
    }

    @Override
    public void moveOrRename(MoveOrRenameRequest request, StreamObserver<MoveOrRenameResponse> responseObserver) {
        Path root = getRoot();
        try {
            Path source = resolveUnderRoot(request.getSourcePath(), root);
            Path dest = resolveUnderRoot(request.getDestinationPath(), root);

            if (!isInsideRoot(source, root) || !isInsideRoot(dest, root)) {
                respondMove(responseObserver, false, ResponseCode.OUT_OF_ROOT, "Ruta fuera del root del nodo", "");
                return;
            }

            if (!Files.exists(source)) {
                respondMove(responseObserver, false, ResponseCode.NOT_FOUND, "Fuente no existe", "");
                return;
            }
            if (Files.exists(dest)) {
                respondMove(responseObserver, false, ResponseCode.DEST_EXISTS, "Destino ya existe", dest.toString());
                return;
            }

            // Crear dir padre si no existe
            Path parent = dest.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
            respondMove(responseObserver, true, ResponseCode.OK, "Operación exitosa", dest.toString());
        } catch (AccessDeniedException ade) {
            respondMove(responseObserver, false, ResponseCode.PERMISSION_DENIED, ade.getMessage(), "");
        } catch (AtomicMoveNotSupportedException amnse) {
            // fallback sin ATOMIC_MOVE
            try {
                Path root2 = getRoot();
                Path source = resolveUnderRoot(request.getSourcePath(), root2);
                Path dest = resolveUnderRoot(request.getDestinationPath(), root2);
                Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                respondMove(responseObserver, true, ResponseCode.OK, "Operación exitosa (no atómica)", dest.toString());
            } catch (IOException e2) {
                respondMove(responseObserver, false, ResponseCode.IO_ERROR, e2.getMessage(), "");
            }
        } catch (InvalidPathException ipe) {
            respondMove(responseObserver, false, ResponseCode.INVALID_PATH, ipe.getMessage(), "");
        } catch (IOException ioe) {
            respondMove(responseObserver, false, ResponseCode.IO_ERROR, ioe.getMessage(), "");
        } catch (Exception ex) {
            log.error("Error moveOrRename", ex);
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    private void respondMove(StreamObserver<MoveOrRenameResponse> responseObserver, boolean success, ResponseCode code, String message, String resultingPath) {
        MoveOrRenameResponse resp = MoveOrRenameResponse.newBuilder()
                .setSuccess(success)
                .setCode(code)
                .setMessage(message == null ? "" : message)
                .setResultingPath(resultingPath == null ? "" : resultingPath)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        Path root = getRoot();
        try {
            Path target = resolveUnderRoot(request.getPath(), root);
            if (!isInsideRoot(target, root)) {
                respondDelete(responseObserver, false, ResponseCode.OUT_OF_ROOT, "Ruta fuera del root del nodo");
                return;
            }
            if (!Files.exists(target)) {
                respondDelete(responseObserver, false, ResponseCode.NOT_FOUND, "Recurso no existe");
                return;
            }
            if (Files.isDirectory(target)) {
                // Solo borrar directorio vacío de forma segura
                try (var dirStream = Files.newDirectoryStream(target)) {
                    if (dirStream.iterator().hasNext()) {
                        respondDelete(responseObserver, false, ResponseCode.IO_ERROR, "Directorio no vacío");
                        return;
                    }
                }
            }
            Files.delete(target);
            respondDelete(responseObserver, true, ResponseCode.OK, "Eliminado");
        } catch (AccessDeniedException ade) {
            respondDelete(responseObserver, false, ResponseCode.PERMISSION_DENIED, ade.getMessage());
        } catch (InvalidPathException ipe) {
            respondDelete(responseObserver, false, ResponseCode.INVALID_PATH, ipe.getMessage());
        } catch (NoSuchFileException nsfe) {
            respondDelete(responseObserver, false, ResponseCode.NOT_FOUND, nsfe.getMessage());
        } catch (IOException ioe) {
            respondDelete(responseObserver, false, ResponseCode.IO_ERROR, ioe.getMessage());
        } catch (Exception ex) {
            log.error("Error deleteFile", ex);
            responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    private void respondDelete(StreamObserver<DeleteFileResponse> responseObserver, boolean success, ResponseCode code, String message) {
        DeleteFileResponse resp = DeleteFileResponse.newBuilder()
                .setSuccess(success)
                .setCode(code)
                .setMessage(message == null ? "" : message)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}
