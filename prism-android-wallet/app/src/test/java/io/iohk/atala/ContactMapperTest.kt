package io.iohk.atala

import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.protos.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.charset.Charset

class ContactMapperTest {

    private lateinit var verifierInfo: VerifierInfo
    private lateinit var holderInfo: HolderInfo
    private lateinit var issuerInfo: IssuerInfo
    private lateinit var expectedConnectionID: String
    private lateinit var expectedConnectionToken: String

    @Before
    fun init() {
        expectedConnectionID = "e7a4309e-cce4-4d4f-abac-b12be6fccbd0"
        expectedConnectionToken = "KeBg_oKlmsLqDBEkxfMWYQ=="
        verifierInfo = VerifierInfo.newBuilder()
                .setDID("d54850e563994ecf48bff7e9fd51b9c4279af467d9b17fe01336623b178838d9")
                .setName("Verifier Name")
                .setLogo(ByteString.copyFrom("verifierLogoImageData", Charset.defaultCharset()))
                .build()
        holderInfo = HolderInfo.newBuilder()
                .setDID("a7d85a93-f45c-4b7b-9267-09abb64ce2c0")
                .setName("Holder Name")
                .build()
        issuerInfo = IssuerInfo.newBuilder()
                .setDID("did:test:091d41cc-e8fc-4c44-9bd3-c938dcf76dff")
                .setName("Issuer Name")
                .setLogo(ByteString.copyFrom("issuerLogoImageData", Charset.defaultCharset()))
                .build()
    }

    @Test
    fun testMappingAVerifierConnection() {
        val connection = ConnectionInfo.newBuilder()
                .setConnectionId(expectedConnectionID)
                .setToken(expectedConnectionToken)
                .setParticipantInfo(ParticipantInfo.newBuilder().setVerifier(verifierInfo))
                .build()

        val contact = ContactMapper.mapToContact(connection, "KEY_DERIVATION_PATH")

        Assert.assertEquals(contact.connectionId, expectedConnectionID)
        Assert.assertEquals(contact.token, expectedConnectionToken)
        Assert.assertEquals(contact.keyDerivationPath, "KEY_DERIVATION_PATH")
        Assert.assertEquals(contact.did, verifierInfo.did)
        Assert.assertEquals(contact.name, verifierInfo.name)
        Assert.assertEquals(String(contact.logo), String(verifierInfo.logo.toByteArray()))
    }

    @Test
    fun testMappingAIssuerConnection() {
        val connection = ConnectionInfo.newBuilder()
                .setConnectionId(expectedConnectionID)
                .setToken(expectedConnectionToken)
                .setParticipantInfo(ParticipantInfo.newBuilder().setIssuer(issuerInfo))
                .build()

        val contact = ContactMapper.mapToContact(connection, "KEY_DERIVATION_PATH")

        Assert.assertEquals(contact.connectionId, expectedConnectionID)
        Assert.assertEquals(contact.token, expectedConnectionToken)
        Assert.assertEquals(contact.keyDerivationPath, "KEY_DERIVATION_PATH")
        Assert.assertEquals(contact.did, issuerInfo.did)
        Assert.assertEquals(contact.name, issuerInfo.name)
        Assert.assertEquals(String(contact.logo), String(issuerInfo.logo.toByteArray()))
    }

    @Test
    fun testMappingAHolderConnection() {
        val connection = ConnectionInfo.newBuilder()
                .setConnectionId(expectedConnectionID)
                .setToken(expectedConnectionToken)
                .setParticipantInfo(ParticipantInfo.newBuilder().setHolder(holderInfo))
                .build()

        val contact = ContactMapper.mapToContact(connection, "KEY_DERIVATION_PATH")

        Assert.assertEquals(contact.connectionId, expectedConnectionID)
        Assert.assertEquals(contact.token, expectedConnectionToken)
        Assert.assertEquals(contact.keyDerivationPath, "KEY_DERIVATION_PATH")
        Assert.assertEquals(contact.did, holderInfo.did)
        Assert.assertEquals(contact.name, holderInfo.name)
        Assert.assertTrue(contact.logo == null)
    }
}