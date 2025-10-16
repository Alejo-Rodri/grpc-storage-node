package co.edu.upb.grpc_storage_node.cli;

import com.example.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Arrays;

/**
 * CLI sencillo para invocar operaciones del nodo por gRPC sin modificar archivos existentes.
 *
 * Comandos:
 *   - delete <path> [--host <h>] [--port <p>]
 *   - move <source> <destination> [--host <h>] [--port <p>]
 *   - rename <source> <destination> [--host <h>] [--port <p>]  (alias de move)
 *   - mkdir <path> [--host <h>] [--port <p>]
 *
 * Defaults: host=localhost, port=9090
 */
public class NodeCli {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            printUsageAndExit(0);
            return;
        }

        // Valores por defecto
        String host = "localhost";
        int port = 9090;

        // Extraer host/port opcionales al final (o en cualquier posición)
        String[] filtered = new String[args.length];
        int fi = 0;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--host".equalsIgnoreCase(a) && (i + 1) < args.length) {
                host = args[++i];
            } else if ("--port".equalsIgnoreCase(a) && (i + 1) < args.length) {
                try { port = Integer.parseInt(args[++i]); } catch (NumberFormatException nfe) { /* ignore, keep default */ }
            } else {
                filtered[fi++] = a;
            }
        }
        filtered = Arrays.copyOf(filtered, fi);

        if (filtered.length == 0) {
            printUsageAndExit(0);
            return;
        }

        String cmd = filtered[0].toLowerCase();
        try {
            switch (cmd) {
                case "delete":
                    if (filtered.length < 2) {
                        System.err.println("Falta argumento: path");
                        printUsageAndExit(2);
                    }
                    execDelete(host, port, filtered[1]);
                    break;
                case "move":
                case "rename":
                    if (filtered.length < 3) {
                        System.err.println("Faltan argumentos: source destination");
                        printUsageAndExit(2);
                    }
                    execMove(host, port, filtered[1], filtered[2]);
                    break;
                case "mkdir":
                    if (filtered.length < 2) {
                        System.err.println("Falta argumento: path");
                        printUsageAndExit(2);
                    }
                    execMkdir(host, port, filtered[1]);
                    break;
                case "help":
                case "-h":
                case "--help":
                    printUsageAndExit(0);
                    break;
                default:
                    System.err.println("Comando desconocido: " + cmd);
                    printUsageAndExit(2);
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void execDelete(String host, int port, String path) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        try {
            FileNodeServiceGrpc.FileNodeServiceBlockingStub stub = FileNodeServiceGrpc.newBlockingStub(channel);
            DeleteFileRequest req = DeleteFileRequest.newBuilder().setPath(path).build();
            DeleteFileResponse resp = stub.deleteFile(req);
            printFileOpResult("delete", resp.getSuccess(), resp.getCode(), resp.getMessage(), null);
            System.exit(resp.getSuccess() ? 0 : 3);
        } finally {
            channel.shutdownNow();
        }
    }

    private static void execMove(String host, int port, String source, String dest) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        try {
            FileNodeServiceGrpc.FileNodeServiceBlockingStub stub = FileNodeServiceGrpc.newBlockingStub(channel);
            MoveOrRenameRequest req = MoveOrRenameRequest.newBuilder()
                    .setSourcePath(source)
                    .setDestinationPath(dest)
                    .build();
            MoveOrRenameResponse resp = stub.moveOrRename(req);
            printFileOpResult("move/rename", resp.getSuccess(), resp.getCode(), resp.getMessage(), resp.getResultingPath());
            System.exit(resp.getSuccess() ? 0 : 3);
        } finally {
            channel.shutdownNow();
        }
    }

    private static void execMkdir(String host, int port, String path) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        try {
            DirectoryServiceGrpc.DirectoryServiceBlockingStub stub = DirectoryServiceGrpc.newBlockingStub(channel);
            CreateDirectoryRequest req = CreateDirectoryRequest.newBuilder().setPath(path).build();
            CreateDirectoryResponse resp = stub.createDirectory(req);
            System.out.println("Directorio creado:");
            System.out.println("  path: " + resp.getPath());
            System.out.println("  owner: " + resp.getOwner());
            System.out.println("  createdAt: " + resp.getCreatedAt());
            System.exit(0);
        } finally {
            channel.shutdownNow();
        }
    }

    private static void printFileOpResult(String op, boolean success, ResponseCode code, String message, String resultingPath) {
        System.out.println("Operación: " + op);
        System.out.println("  success: " + success);
        System.out.println("  code: " + code);
        if (resultingPath != null && !resultingPath.isBlank()) {
            System.out.println("  resultingPath: " + resultingPath);
        }
        if (message != null && !message.isBlank()) {
            System.out.println("  message: " + message);
        }
    }

    private static void printUsageAndExit(int code) {
        String usage = String.join(System.lineSeparator(),
                "Uso:",
                "  java -cp <jar|clases> co.edu.upb.grpc_storage_node.cli.NodeCli <comando> [args] [--host <h>] [--port <p>]",
                "",
                "Comandos:",
                "  delete <path>                 Elimina un archivo/directorio (dir debe estar vacío)",
                "  move <source> <destination>   Mueve o renombra un archivo/directorio",
                "  rename <source> <destination> Alias de move",
                "  mkdir <path>                  Crea un directorio (y padres si no existen)",
                "",
                "Opciones:",
                "  --host <h>   Host del nodo (default: localhost)",
                "  --port <p>   Puerto gRPC del nodo (default: 9090)",
                "",
                "Ejemplos:",
                "  mvn -q -f grpc-storage-node/pom.xml exec:java -Dexec.mainClass=co.edu.upb.grpc_storage_node.cli.NodeCli -Dexec.args=\"mkdir C:/data/test\"",
                "  mvn -q -f grpc-storage-node/pom.xml exec:java -Dexec.mainClass=co.edu.upb.grpc_storage_node.cli.NodeCli -Dexec.args=\"move C:/data/a.txt C:/data/b.txt\"",
                "  mvn -q -f grpc-storage-node/pom.xml exec:java -Dexec.mainClass=co.edu.upb.grpc_storage_node.cli.NodeCli -Dexec.args=\"delete C:/data/b.txt --host localhost --port 9090\""
        );
        System.out.println(usage);
        System.exit(code);
    }
}
