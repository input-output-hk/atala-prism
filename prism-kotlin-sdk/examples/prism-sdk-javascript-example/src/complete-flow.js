const prism = require('prism-kotlin-sdk/packages/extras');

const { EC, SHA256DigestCompanion, MerkleInclusionProofCompanion } = prism.io.iohk.atala.prism.kotlin.crypto;
const { DIDCompanion } = prism.io.iohk.atala.prism.kotlin.identity;
const {
    GrpcEnvoyOptions, ConnectorServicePromise, NodeServicePromise, RegisterDIDRequest,
    GenerateConnectionTokenRequest, GetConnectionTokenInfoRequest, AddConnectionFromTokenRequest,
    CredentialBatchData, IssueCredentialBatchRequest, AtalaMessage, PlainTextCredential,
    GetConnectionByTokenRequest, SendMessageRequest, GetMessagesPaginatedRequest,
    GetDidDocumentRequest, GetBatchStateRequest, GetCredentialRevocationTimeRequest,
    RevokeCredentialsRequest,
    noUnknownFields,
} = prism.io.iohk.atala.prism.kotlin.protos;
const {
    CredentialBatches, BatchData, CredentialVerification, CredentialBatchIdCompanion,
} = prism.io.iohk.atala.prism.kotlin.credentials;
const { CredentialContentCompanion } = prism.io.iohk.atala.prism.kotlin.credentials.content;
const { JsonBasedCredential, JsonBasedCredentialCompanion } = prism.io.iohk.atala.prism.kotlin.credentials.json;
const {
    ProtoUtils, RequestUtils, findPublicKey, toTimestampInfoModel, toList, toArray
} = prism.io.iohk.atala.prism.kotlin.extras;
const { pbandk } = prism;

function multilinePrint(text) {
    console.log(text.split('\n').map((s) => s.trim()).join('\n'));
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}


async function completeFlow() {
    multilinePrint(`
        Welcome to the complete flow example, which covers:
        - Interacting with the PRISM node to claim/verify DIDs, as well as to issuer/verify credentials.
        - Interacting with the PRISM connector to connect with other entities and share credentials with those.
        - An issuer who claims its DID, using it to issue a credential to a holder.
        - A holder who claims its unpublished DID, who connects to the issuer to receive a credential.
        - A holder who claims a second unpublished DID to connect to the verifier.
        - A verifier who claims its DID, using it to connect to the holder to receive a credential.
        - The verifier receiving a credential from the connector, gathering the necessary data about it from the node
        and then, run the credential verification.
    `);
    console.log('Creating the clients for the connector/node, '
        + 'which are expected to be running on provided environment');
    const environment = 'localhost';
    const connector = new ConnectorServicePromise(new GrpcEnvoyOptions('http', environment, 10000));
    const node = new NodeServicePromise(new GrpcEnvoyOptions('http', environment, 10000));

    // Issuer claims an identity
    console.log('Issuer: Generates and registers a DID');
    const issuerMasterKeyPair = EC.generateKeyPair();
    const issuerCreateDIDOperation = ProtoUtils.createDidAtalaOperation(issuerMasterKeyPair);
    const issuerCreatedDIDSignedOperation = ProtoUtils.signedAtalaOperation(
        issuerMasterKeyPair,
        issuerCreateDIDOperation,
    );

    // Issuer registers its identity to the node
    // Usually the DID would be registered with the node, but, the connector can handle that as well
    const issuerRegisterDIDResponse = await connector.RegisterDID(
        new RegisterDIDRequest(
            new RegisterDIDRequest.Role().ISSUER,
            'Issuer',
            new pbandk.ByteArr(new Int8Array(0)),
            new RegisterDIDRequest.RegisterWith.CreateDidOperation(issuerCreatedDIDSignedOperation),
            noUnknownFields,
        ),
    );
    const issuerDID = DIDCompanion.fromString(issuerRegisterDIDResponse.did);

    // the DID takes some minutes to get confirmed by Cardano, in the mean time, the unpublished DID
    // can be used to authenticate requests to the backend
    const issuerUnpublishedDID = DIDCompanion.createUnpublishedDID(issuerMasterKeyPair.publicKey);

    multilinePrint(`
        Issuer DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - DID: ${issuerRegisterDIDResponse.did}
        - Operation identifier: ${issuerRegisterDIDResponse.operationId}
    `);

    // Issuer generates a token to connect with the credential holder
    const issuerGenerateConnectionTokenRequest = new GenerateConnectionTokenRequest(
        1,
        noUnknownFields,
    );
    const issuerGenerateConnectionTokenResponse = await connector.GenerateConnectionTokenAuth(
        issuerGenerateConnectionTokenRequest,
        RequestUtils.generateRequestMetadata(
            issuerUnpublishedDID.value,
            issuerMasterKeyPair.privateKey,
            issuerGenerateConnectionTokenRequest,
        ),
    );
    const issuerConnectionToken = toArray(issuerGenerateConnectionTokenResponse.tokens)[0];
    console.log('Issuer: Token for connecting with Holder generated = $issuerConnectionToken');

    // Holder generates its identity to connect with issuer
    const holderMasterKeyPair = EC.generateKeyPair();
    const holderUnpublishedDID = DIDCompanion.createUnpublishedDID(holderMasterKeyPair.publicKey);
    console.log(`Holder: First DID generated to connect with Issuer = ${holderUnpublishedDID.value}`);

    // Holder generates its identity to connect with verifier
    // in PRISM, you are supposed to use different identities for every connection
    const holderMasterKeyPair2 = EC.generateKeyPair();
    const holderUnpublishedDID2 = DIDCompanion.createUnpublishedDID(
        holderMasterKeyPair2.publicKey,
    );
    console.log(`Holder: Second DID generated to connect with Verifier = ${holderUnpublishedDID2.value}`);

    // Holder verifies the connection token details to make sure its connecting to the right entity
    const issuerConnectionTokenDetails = await connector.GetConnectionTokenInfo(
        new GetConnectionTokenInfoRequest(issuerConnectionToken, noUnknownFields),
    );
    multilinePrint(`
        Holder: Check Issuer's connection token details:
        - Issuer name = ${issuerConnectionTokenDetails.creatorName}
        - Issuer DID  = ${issuerConnectionTokenDetails.creatorDID}
    `);

    // Holder accepts the connection token to connect to Issuer
    const holderAcceptsIssuerConnectionRequest = new AddConnectionFromTokenRequest(
        issuerConnectionToken,
        null,
        noUnknownFields,
    );
    const holderAcceptsIssuerConnectionResponse = await connector.AddConnectionFromTokenAuth(
        holderAcceptsIssuerConnectionRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID.value,
            holderMasterKeyPair.privateKey,
            holderAcceptsIssuerConnectionRequest,
        ),
    );
    const holderIssuerConnection = holderAcceptsIssuerConnectionResponse.connection;
    console.log(`Holder (DID 1): Connected to Issuer, connectionId = ${holderIssuerConnection.connectionId}`);

    // Issuer generates a credential to Holder
    const holderCredentialContentJson = {
        issuerDid: issuerDID.value,
        issuanceKeyId: 'master0',
        credentialSubject: {
            name: 'José López Portillo',
            certificate: 'Certificate of PRISM SDK tutorial completion',
        },
    };
    const holderCredentialContent = CredentialContentCompanion.fromString(
        JSON.stringify(holderCredentialContentJson),
    );
    const holderUnsignedCredential = new JsonBasedCredential(holderCredentialContent, null);
    const holderSignedCredential = holderUnsignedCredential.sign(issuerMasterKeyPair.privateKey);

    // Include the credential in a batch
    const holderBatchResult = CredentialBatches.batch(toList([holderSignedCredential]));
    const holderCredentialMerkleRoot = holderBatchResult.root;
    const holderCredentialMerkleProofs = toArray(holderBatchResult.proofs);
    const merkleRootHash = holderCredentialMerkleRoot.hash;
    const credentialBatchData = new CredentialBatchData(
        // This requires the suffix only, as the node stores only suffixes
        issuerDID.suffix.toString(),
        new pbandk.ByteArr(merkleRootHash.value),
        noUnknownFields,
    );
    const issueCredentialOperation = ProtoUtils.issueCredentialBatchOperation(credentialBatchData);

    // Issuer publishes the credential to Cardano
    const signedIssueCredentialOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issueCredentialOperation);
    const issueCredentialBatchRequest = new IssueCredentialBatchRequest(
        signedIssueCredentialOperation,
        noUnknownFields,
    );
    const issuedCredentialResponse = await node.IssueCredentialBatch(issueCredentialBatchRequest);
    multilinePrint(`
        Issuer: Credential issued to Holder, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - IssuerDID = $issuerDID
        - Operation identifier = ${issuedCredentialResponse.operationId}
        - Credential content = $holderUnsignedCredential
        - Signed credential = ${holderSignedCredential.canonicalForm}
        - Inclusion proof (encoded) = ${holderCredentialMerkleProofs[0].encode()}
        - Batch id = ${issuedCredentialResponse.batchId}
    `);

    // Issuer sends the credential to Holder through the connector
    const credentialFromIssuerMessage = new AtalaMessage(
        '',
        new AtalaMessage.Message.PlainCredential(
            new PlainTextCredential(
                holderSignedCredential.canonicalForm,
                holderCredentialMerkleProofs[0].encode(),
                noUnknownFields,
            ),
        ),
        noUnknownFields,
    );

    // Issuer needs the connection id to send a message to Holder, which can be retrieved
    // from the token generated before.
    const issuerGetConnectionRequest = new GetConnectionByTokenRequest(
        issuerConnectionToken,
        noUnknownFields,
    );
    const issuerGetConnectionResponse = await connector.GetConnectionByTokenAuth(
        issuerGetConnectionRequest,
        RequestUtils.generateRequestMetadata(
            issuerUnpublishedDID.value,
            issuerMasterKeyPair.privateKey,
            issuerGetConnectionRequest,
        ),
    );
    const issuerHolderConnectionId = issuerGetConnectionResponse.connection.connectionId;

    // the connector allows any kind of message, this is just a way to send a credential but you can define your own
    const issuerSendMessageRequest = new SendMessageRequest(
        issuerHolderConnectionId,
        new pbandk.ByteArr(pbandk.encodeToByteArray(credentialFromIssuerMessage)),
        '',
        noUnknownFields,
    );
    await connector.SendMessageAuth(
        issuerSendMessageRequest,
        RequestUtils.generateRequestMetadata(
            issuerUnpublishedDID.value,
            issuerMasterKeyPair.privateKey,
            issuerSendMessageRequest,
        ),
    );
    console.log('Issuer: Credential sent to Holder');

    // Holder receives the credential from Issuer
    const holderGetMessagesRequest = new GetMessagesPaginatedRequest('', 1, noUnknownFields);
    const holderGetMessagesResponse = await connector.GetMessagesPaginatedAuth(
        holderGetMessagesRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID.value,
            holderMasterKeyPair.privateKey,
            holderGetMessagesRequest,
        ),
    );
    const holderReceivedMessage = toArray(holderGetMessagesResponse.messages)[0];

    const holderReceivedCredential = pbandk.decodeFromByteArray(
        new AtalaMessage('', null, noUnknownFields).descriptor.messageCompanion,
        holderReceivedMessage.message.array,
    ).plainCredential;
    multilinePrint(`
        Holder: Message received
        - Canonical credential = ${holderReceivedCredential.encodedCredential}
        - Inclusion proof = ${holderReceivedCredential.encodedMerkleProof}
    `);

    // Verifier claims an identity, similar to the previous example done with Issuer
    console.log('Verifier: Generates and registers a DID');
    const verifierMasterKeyPair = EC.generateKeyPair();
    const verifierCreateDIDOperation = ProtoUtils.createDidAtalaOperation(verifierMasterKeyPair);
    const verifierCreateDIDSignedOperation = ProtoUtils.signedAtalaOperation(verifierMasterKeyPair, verifierCreateDIDOperation);

    const verifierRegisterDIDResponse = await connector.RegisterDID(
        new RegisterDIDRequest(
            new RegisterDIDRequest.Role().VERIFIER,
            'Verifier',
            new pbandk.ByteArr(new Int8Array(0)),
            new RegisterDIDRequest.RegisterWith.CreateDidOperation(verifierCreateDIDSignedOperation),
            noUnknownFields,
        ),
    );
    const verifierDID = DIDCompanion.fromString(verifierRegisterDIDResponse.did);
    const verifierUnpublishedDID = DIDCompanion.createUnpublishedDID(verifierMasterKeyPair.publicKey);
    multilinePrint(`
        Verifier DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - DID: $verifierDID
        - Operation identifier: ${verifierRegisterDIDResponse.operationId}
    `);

    // Verifier generates a token to connect with the credential holder
    const verifierGenerateConnectionTokenRequest = new GenerateConnectionTokenRequest(1, noUnknownFields);
    const verifierGenerateConnectionTokenResponse = await connector.GenerateConnectionTokenAuth(
        verifierGenerateConnectionTokenRequest,
        RequestUtils.generateRequestMetadata(
            verifierUnpublishedDID.value,
            verifierMasterKeyPair.privateKey,
            verifierGenerateConnectionTokenRequest,
        ),
    );
    const verifierConnectionToken = toArray(verifierGenerateConnectionTokenResponse.tokens)[0];
    console.log(`Verifier: Token for connecting with Holder generated = ${verifierConnectionToken}`);

    // Holder accepts the connection token to connect to Verifier
    const holderAcceptsVerifierConnectionRequest = new AddConnectionFromTokenRequest(
        verifierConnectionToken,
        null,
        noUnknownFields,
    );
    const holderAcceptsVerifierConnectionResponse = await connector.AddConnectionFromTokenAuth(
            holderAcceptsVerifierConnectionRequest,
            RequestUtils.generateRequestMetadata(
                holderUnpublishedDID2.value,
                holderMasterKeyPair2.privateKey,
                holderAcceptsVerifierConnectionRequest,
            ),
        );
    const holderVerifierConnection = holderAcceptsVerifierConnectionResponse.connection;
    console.log(`Holder (DID 2): Connected to Verifier, connectionId = ${holderVerifierConnection.connectionId}`);

    // Holder shares a credential with Verifier
    const credentialFromHolderMessage = new AtalaMessage(
        '',
        new AtalaMessage.Message.PlainCredential(
            new PlainTextCredential(
                holderReceivedCredential.encodedCredential,
                holderReceivedCredential.encodedMerkleProof,
                noUnknownFields,
            ),
        ),
        noUnknownFields,
    );

    const holderSendMessageRequest = new SendMessageRequest(
        holderVerifierConnection.connectionId,
        new pbandk.ByteArr(pbandk.encodeToByteArray(credentialFromHolderMessage)),
        '',
        noUnknownFields,
    );

    const response = await connector.SendMessageAuth(
        holderSendMessageRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID2.value,
            holderMasterKeyPair2.privateKey,
            holderSendMessageRequest,
        ),
    );
    console.log('Holder (DID 2): Credential sent to Verifier');

    // Verifier receives the credential shared from Holder
    const verifierGetMessagesRequest = new GetMessagesPaginatedRequest('', 1, noUnknownFields);
    const verifierGetMessagesResponse = await connector.GetMessagesPaginatedAuth(
        verifierGetMessagesRequest,
        RequestUtils.generateRequestMetadata(
            verifierUnpublishedDID.value,
            verifierMasterKeyPair.privateKey,
            verifierGetMessagesRequest,
        ),
    );
    const verifierReceivedMessage = toArray(verifierGetMessagesResponse.messages)[0];
    const verifierReceivedCredential = pbandk.decodeFromByteArray(
        new AtalaMessage('', null, noUnknownFields).descriptor.messageCompanion,
        verifierReceivedMessage.message.array,
    ).plainCredential;
    multilinePrint(`
        Verifier: Message received
        - Canonical credential = ${verifierReceivedCredential.encodedCredential}
        - Inclusion proof = ${verifierReceivedCredential.encodedMerkleProof}
    `);

    // decode the received credential
    const verifierReceivedJsonCredential = JsonBasedCredentialCompanion.fromString(
        verifierReceivedCredential.encodedCredential,
    );
    const verifierReceivedCredentialIssuerDID = verifierReceivedJsonCredential.content.getString('issuerDid');
    const verifierReceivedCredentialIssuanceKeyId = verifierReceivedJsonCredential.content.getString('issuanceKeyId');
    multilinePrint(`
        Verifier: Received credential decoded
        - Credential: ${verifierReceivedJsonCredential.content}
        - Issuer DID: $verifierReceivedCredentialIssuerDID
        - Issuer issuance key id: $verifierReceivedCredentialIssuanceKeyId
    `);

    // Verifier queries the node for the credential data
    console.log('Verifier: Resolving issuer/credential details from the node');
    const verifierGetDidResponse = await node.GetDidDocument(
        new GetDidDocumentRequest(verifierReceivedCredentialIssuerDID, noUnknownFields)
    );
    const verifierReceivedCredentialIssuerDIDDocument = verifierGetDidResponse.document
    const verifierReceivedCredentialIssuerKey = findPublicKey(
        verifierReceivedCredentialIssuerDIDDocument,
        verifierReceivedCredentialIssuanceKeyId,
    );
    const verifierReceivedCredentialMerkleProof = MerkleInclusionProofCompanion.decode(verifierReceivedCredential.encodedMerkleProof);

    const verifierReceivedCredentialBatchId = CredentialBatches.computeCredentialBatchId(
        DIDCompanion.fromString(verifierReceivedCredentialIssuerDID),
        verifierReceivedCredentialMerkleProof.derivedRoot(),
    );

    const verifierReceivedCredentialBatchState = await node.GetBatchState(
        new GetBatchStateRequest(
            SHA256DigestCompanion.fromHex(verifierReceivedCredentialBatchId.id).hexValue(),
            noUnknownFields,
        ),
    );
    const publicationTimestamp = verifierReceivedCredentialBatchState.publicationLedgerData.timestampInfo
    const revocationTimestamp = verifierReceivedCredentialBatchState.revocationLedgerData?.timestampInfo
    const verifierReceivedCredentialBatchData = new BatchData(
        publicationTimestamp != null ? toTimestampInfoModel(publicationTimestamp) : null,
        revocationTimestamp != null ? toTimestampInfoModel(revocationTimestamp) : null,
    );
    const verifierGetCredentialRevocationTimeResponse = await node.GetCredentialRevocationTime(
        new GetCredentialRevocationTimeRequest(
            verifierReceivedCredentialBatchId.id,
            new pbandk.ByteArr(verifierReceivedJsonCredential.hash().value),
            noUnknownFields,
        ),
    );
    const verifierRevocationTimestampInfo = verifierGetCredentialRevocationTimeResponse?.revocationLedgerData?.timestampInfo
    const verifierReceivedCredentialRevocationTime = verifierRevocationTimestampInfo != null ?
        toTimestampInfoModel(verifierRevocationTimestampInfo) : null;

    // Verifier checks the credential validity (which succeeds)
    console.log('Verifier: Verifying received credential')
    CredentialVerification.verifyMerkle(
        verifierReceivedCredentialIssuerKey,
        verifierReceivedCredentialBatchData,
        verifierReceivedCredentialRevocationTime,
        verifierReceivedCredentialMerkleProof.derivedRoot(), // TODO: We may want to receive this instead of computing it
        verifierReceivedCredentialMerkleProof,
        verifierReceivedJsonCredential
    )

    // Issuer revokes the credential
    const issuerRevokeCredentialOperation = ProtoUtils.revokeCredentialsOperation(
        SHA256DigestCompanion.compute(pbandk.encodeToByteArray(issueCredentialOperation)),
        CredentialBatchIdCompanion.fromString(issuedCredentialResponse.batchId),
        toList([holderSignedCredential]),
    )
    const issuerRevokeCredentialSignedOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issuerRevokeCredentialOperation)
    const issuerCredentialRevocationResponse = await node.RevokeCredentials(
        new RevokeCredentialsRequest(
            issuerRevokeCredentialSignedOperation,
            noUnknownFields,
        ),
    )
    multilinePrint(`
        Issuer: Credential revoked, the transaction can take up to 10 minutes to be confirmed by the Cardano network
        - Operation identifier: ${issuerCredentialRevocationResponse.operationId}
    `)

    // Verifier resolves the credential revocation time from the node
    console.log("Verifier: Checking the credential validity again, expect an error explaining that the credential is revoked")
    await sleep(2000) // give some time to the backend to apply the operation
    const verifierGetCredentialRevocationTimeResponse2 = await node.GetCredentialRevocationTime(
            new GetCredentialRevocationTimeRequest(
                verifierReceivedCredentialBatchId.id,
                new pbandk.ByteArr(verifierReceivedJsonCredential.hash().value),
                noUnknownFields,
            ),
        )
    const verifierRevocationTimestampInfo2 = verifierGetCredentialRevocationTimeResponse2?.revocationLedgerData?.timestampInfo
    const verifierReceivedCredentialRevocationTime2 = verifierRevocationTimestampInfo2 != null ?
        toTimestampInfoModel(verifierRevocationTimestampInfo2) : null;

    // Verifier checks the credential validity (which fails)
    try {
        CredentialVerification.verifyMerkle(
            verifierReceivedCredentialIssuerKey,
            verifierReceivedCredentialBatchData,
            verifierReceivedCredentialRevocationTime2,
            verifierReceivedCredentialMerkleProof.derivedRoot(),
            verifierReceivedCredentialMerkleProof,
            verifierReceivedJsonCredential
        );
        console.error("Credential remained valid after revocation");
    } catch (e) {
        console.log(e);
    }
}

completeFlow();
