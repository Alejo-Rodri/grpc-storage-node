param(
  [Parameter(Mandatory = $true)][string]$Path,
  [string]$GrpcHost = "localhost",
  [int]$GrpcPort = 9090
)

$pom = Join-Path $PSScriptRoot "..\pom.xml"
$argsValue = "mkdir `"$Path`" --host $GrpcHost --port $GrpcPort"
& mvn -q -f $pom exec:java "-Dexec.mainClass=co.edu.upb.grpc_storage_node.cli.NodeCli" "-Dexec.args=$argsValue"
