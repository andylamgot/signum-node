package brs.grpc

import brs.grpc.proto.ProtoBuilder
import com.google.protobuf.Message
import io.grpc.stub.StreamObserver

interface StreamResponseGrpcApiHandler<R : Message, S : Message> : GrpcApiHandler<R, S> {
    override suspend fun handleRequest(request: R): S {
        throw UnsupportedOperationException("Cannot return single value from stream response")
    }

    suspend fun handleStreamRequest(request: R, responseObserver: StreamObserver<S>)

    override suspend fun handleRequest(request: R, responseObserver: StreamObserver<S>) {
        try {
            handleStreamRequest(request, responseObserver)
        } catch (e: Exception) {
            responseObserver.onError(ProtoBuilder.buildError(e))
        }
    }
}
