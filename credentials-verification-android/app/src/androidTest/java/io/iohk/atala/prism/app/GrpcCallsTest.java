package io.iohk.atala.prism.app;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.iohk.atala.prism.crypto.japi.ECKeyPair;
import io.iohk.atala.prism.app.utils.CryptoUtils;
import io.iohk.atala.prism.app.utils.GrpcUtils;
import io.iohk.atala.prism.protos.AddConnectionFromTokenRequest;
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse;
import io.iohk.atala.prism.protos.ConnectorServiceGrpc;
import io.iohk.atala.prism.protos.EncodedPublicKey;
import io.iohk.atala.prism.protos.GetConnectionsPaginatedRequest;
import io.iohk.atala.prism.protos.GetMessagesPaginatedRequest;
import io.iohk.atala.prism.protos.GetMessagesPaginatedResponse;
import io.iohk.cvp.BuildConfig;


@RunWith(BlockJUnit4ClassRunner.class)
public class GrpcCallsTest {

    private ManagedChannel origChannel;
    final private List<String> phrases = Arrays.asList("already", "ankle", "announce", "annual", "another",
            "answer", "antenna", "antique", "anxiety", "any", "apart", "apology");
    private int index = -1;

    @Before
    public void init() {
        origChannel = ManagedChannelBuilder
                .forAddress(BuildConfig.API_BASE_URL, BuildConfig.API_PORT)
                .usePlaintext()
                .build();
    }

    @Test
    public void addConnectionTest() throws Exception {
        ECKeyPair ecKeyPair = CryptoUtils.Companion.getKeyPairFromPath(CryptoUtils.Companion.getNextPathFromIndex(index), phrases);

        EncodedPublicKey publicKeyEncoded = GrpcUtils.Companion.getPublicKeyEncoded(ecKeyPair);

        String token = "IoKAZ_a8Q87eBH-KSm3Bsg==";
        String nonce = "";

        AddConnectionFromTokenRequest request = AddConnectionFromTokenRequest.newBuilder()
                .setToken(token)
                .setPaymentNonce(nonce)
                .setHolderEncodedPublicKey(publicKeyEncoded)
                .build();

        AddConnectionFromTokenResponse response = getChannel(null).addConnectionFromToken(request);
        getMessages(index);
    }

    private void getMessages(Integer index) throws Exception {
        GetMessagesPaginatedRequest request = GetMessagesPaginatedRequest.newBuilder()
                .setLimit(200).build();

        ECKeyPair ecKeyPair = CryptoUtils.Companion.getKeyPairFromPath("m/" + ++index + "'/0'/0'", phrases);
        Metadata metadata = CryptoUtils.Companion.getMetadata(ecKeyPair, request.toByteArray());
        GetMessagesPaginatedResponse getMessagesPaginatedResponse = getChannel(metadata).getMessagesPaginated(request);
        getMessagesPaginatedResponse.getMessagesList().forEach(receivedMessage -> System.out.println(receivedMessage.getConnectionId()));
    }

    @Test
    public void getConnection() throws Exception {
        GetConnectionsPaginatedRequest request = GetConnectionsPaginatedRequest.newBuilder()
                .setLimit(200).build();

        ECKeyPair ecKeyPair = CryptoUtils.Companion.getKeyPairFromPath("m/" + ++index + "'/0'/0'", phrases);
        Metadata metadata = CryptoUtils.Companion.getMetadata(ecKeyPair, request.toByteArray());
        getChannel(metadata).getConnectionsPaginated(request);
    }

    @Test
    public void getMessagesFromServerTest() throws Exception {
        getMessages(index);
    }

    private ConnectorServiceGrpc.ConnectorServiceBlockingStub getChannel(Metadata metadata) {
        ConnectorServiceGrpc.ConnectorServiceBlockingStub stub = ConnectorServiceGrpc.newBlockingStub(origChannel);
        if (metadata != null)
            stub = MetadataUtils.attachHeaders(stub, metadata);
        return stub;
    }
}
