const prism = require('prism-kotlin-sdk/packages/extras');

const { EC } = prism.io.iohk.atala.prism.kotlin.crypto;
const { DIDCompanion } = prism.io.iohk.atala.prism.kotlin.identity;
const {
    CredentialBatches, KeyData, TimestampInfo, BatchData, CredentialVerification,
} = prism.io.iohk.atala.prism.kotlin.credentials;
const { CredentialContentCompanion } = prism.io.iohk.atala.prism.kotlin.credentials.content;
const { JsonBasedCredential } = prism.io.iohk.atala.prism.kotlin.credentials.json;
const { toList, toArray, toLong } = prism.io.iohk.atala.prism.kotlin.extras;

function basicUsage() {
    const masterKeyPair = EC.generateKeyPair();
    const did = DIDCompanion.createUnpublishedDID(masterKeyPair.publicKey);
    const credentialContentJson = {
        issuerDid: did.value,
        issuanceKeyId: 'Issuance-0',
        credentialSubject: {
            name: 'José López Portillo',
            certificate: 'Certificate of PRISM SDK tutorial completion',
        },
    };
    const credentialContent = CredentialContentCompanion.fromString(
        JSON.stringify(credentialContentJson),
    );
    const credential = new JsonBasedCredential(credentialContent, null);
    const signedCredential = credential.sign(masterKeyPair.privateKey);
    const batchResult = CredentialBatches.batch(toList([signedCredential]));
    const merkleRoot = batchResult.root;
    const merkleProofs = toArray(batchResult.proofs);

    const now = Math.floor(Date.now() / 1000);
    const didBlockInfo = new TimestampInfo(toLong(now - 60), 1, 1);
    const batchBlockInfo = new TimestampInfo(toLong(now - 20), 2, 2);

    const keyData = new KeyData(masterKeyPair.publicKey, didBlockInfo, null);
    const batchData = new BatchData(batchBlockInfo, null);

    CredentialVerification.verifyMerkle(
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
