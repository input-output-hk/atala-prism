const prism = require('prism-kotlin-sdk/packages/credentials');

const { ECJS } = prism.io.iohk.atala.prism.kotlin.crypto.exposed;
const { DIDJSCompanion } = prism.io.iohk.atala.prism.kotlin.identity.exposed;
const {
    CredentialContentJSCompanion, JsonBasedCredentialJS, CredentialBatchesJS, KeyDataJS,
    TimestampInfoJS, BatchDataJS, CredentialVerificationJS,
} = prism.io.iohk.atala.prism.kotlin.credentials.exposed;

function basicUsage() {
    const masterKeyPair = ECJS.generateKeyPair();
    const did = DIDJSCompanion.createUnpublishedDID(masterKeyPair.publicKey);
    const credentialContentJson = {
        issuerDid: did.getValue(),
        issuanceKeyId: 'Issuance-0',
        credentialSubject: {
            name: 'José López Portillo',
            certificate: 'Certificate of PRISM SDK tutorial completion',
        },
    };
    const credentialContent = CredentialContentJSCompanion.fromString(
        JSON.stringify(credentialContentJson),
    );
    const credential = JsonBasedCredentialJS.create(credentialContent, null);
    const signedCredential = credential.sign(masterKeyPair.privateKey);
    const batchResult = CredentialBatchesJS.batch([signedCredential]);
    const merkleRoot = batchResult.root;
    const merkleProofs = batchResult.proofs;

    const now = Math.floor(Date.now() / 1000);
    const didBlockInfo = TimestampInfoJS.create((now - 60).toString(), 1, 1);
    const batchBlockInfo = TimestampInfoJS.create((now - 20).toString(), 2, 2);

    const keyData = new KeyDataJS(masterKeyPair.publicKey, didBlockInfo, null);
    const batchData = new BatchDataJS(batchBlockInfo, null);

    CredentialVerificationJS.verifyMerkle(
        keyData,
        batchData,
        null,
        merkleRoot,
        merkleProofs[0],
        signedCredential,
    );
    console.log('Credential verified successfully');
}

basicUsage();
