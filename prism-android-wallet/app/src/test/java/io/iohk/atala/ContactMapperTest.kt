package io.iohk.atala

import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.protos.ConnectionInfo
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class ContactMapperTest {

    @Test
    fun testMappingAVerifierConnection() {
        val connection = ConnectionInfo.newBuilder()
            .setConnectionId("e7a4309e-cce4-4d4f-abac-b12be6fccbd0")
            .setToken("KeBg_oKlmsLqDBEkxfMWYQ==")
            .setParticipantDid("d54850e563994ecf48bff7e9fd51b9c4279af467d9b17fe01336623b178838d9")
            .setParticipantName("Contact Name")
            .setParticipantLogo(ByteString.copyFrom("verifierLogoImageData", Charset.defaultCharset()))
            .build()

        val contact = ContactMapper.mapToContact(connection, "KEY_DERIVATION_PATH")

        Assert.assertEquals(contact.connectionId, "e7a4309e-cce4-4d4f-abac-b12be6fccbd0")
        Assert.assertEquals(contact.token, "KeBg_oKlmsLqDBEkxfMWYQ==")
        Assert.assertEquals(contact.keyDerivationPath, "KEY_DERIVATION_PATH")
        Assert.assertEquals(contact.did, "d54850e563994ecf48bff7e9fd51b9c4279af467d9b17fe01336623b178838d9")
        Assert.assertEquals(contact.name, "Contact Name")
        Assert.assertEquals(String(contact.logo), "verifierLogoImageData")
    }
}
