package io.iohk.atala.prism.app.data.local

import androidx.lifecycle.LiveData
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.japi.ECKeyPair
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.DbHelper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.PreferencesHelper
import io.iohk.atala.prism.app.data.local.remote.ApiHelper
import io.iohk.atala.prism.app.viewmodel.dtos.ConnectionDataDto
import io.iohk.atala.prism.protos.*
import javax.inject.Inject

class AppDataManager @Inject constructor(dbHelper: DbHelper, private var apiHelper: ApiHelper, private var prefHelper: PreferencesHelper) : DataManager {

    private var mDbHelper: DbHelper = dbHelper

    override suspend fun saveContact(contact: Contact): Long {
        return mDbHelper.saveContact(contact)
    }

    override suspend fun getAllContacts(): List<Contact> {
        return mDbHelper.getAllContacts()
    }

    override fun allContacts(): LiveData<List<Contact>> {
        return mDbHelper.allContacts()
    }

    override suspend fun contactById(id: Int): Contact? {
        return mDbHelper.contactById(id)
    }

    override suspend fun saveAllCredentials(credentialsList: List<Credential>) {
        mDbHelper.saveAllCredentials(credentialsList)
    }

    override suspend fun addConnection(ecKeyPair: ECKeyPair, token: String, nonce: String): AddConnectionFromTokenResponse {
        return apiHelper.addConnection(ecKeyPair, token, nonce)
    }

    override suspend fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse {
        return apiHelper.getAllMessages(ecKeyPair, lastMessageId)
    }

    override suspend fun sendMultipleMessage(ecKeyPair: ECKeyPair, connectionId: String, messages: List<ByteString>) {
        return apiHelper.sendMultipleMessage(ecKeyPair, connectionId, messages)
    }

    override suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse {
        return apiHelper.getConnectionTokenInfo(token)
    }

    override suspend fun sendMessageToMultipleConnections(connectionDataList: List<ConnectionDataDto>, credential: ByteString) {
        apiHelper.sendMessageToMultipleConnections(connectionDataList, credential)
    }

    override suspend fun getConnection(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse {
        return apiHelper.getConnection(ecKeyPair)
    }

    override fun getCurrentIndex(): Int {
        return prefHelper.getCurrentIndex()
    }

    override fun getMnemonicList(): List<String> {
        return prefHelper.getMnemonicList()
    }

    override fun increaseIndex() {
        prefHelper.increaseIndex()
    }

    override fun getKeyPairFromPath(keyDerivationPath: String): ECKeyPair {
        return prefHelper.getKeyPairFromPath(keyDerivationPath)
    }

    override fun saveMnemonics(phrasesList: MutableList<String>) {
        prefHelper.saveMnemonics(phrasesList)
    }

    override fun saveIndex(lastIndex: Int) {
        prefHelper.saveIndex(lastIndex)
    }

    override suspend fun updateContact(contact: Contact) {
        mDbHelper.updateContact(contact)
    }

    override suspend fun getAllCredentials(): List<Credential> {
        return mDbHelper.getAllCredentials()
    }

    override fun allCredentials(): LiveData<List<Credential>> {
        return mDbHelper.allCredentials()
    }

    override suspend fun getContactByConnectionId(connectionId: String): Contact? {
        return mDbHelper.getContactByConnectionId(connectionId)
    }

    override suspend fun removeAllLocalData() {
        mDbHelper.removeAllLocalData()
    }

    override suspend fun getAllNewCredentials(): List<Credential> {
        return mDbHelper.getAllNewCredentials()
    }

    override suspend fun updateCredential(credential: Credential) {
        mDbHelper.updateCredential(credential)
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential {
        return mDbHelper.getCredentialByCredentialId(credentialId)
    }

    override suspend fun deleteCredential(credential: Credential) {
        mDbHelper.deleteCredential(credential)
    }

    override suspend fun deleteContact(contact: Contact) {
        mDbHelper.deleteContact(contact)
    }

    override suspend fun deleteCredentialByContactId(connectionId: String) {
        mDbHelper.deleteCredentialByContactId(connectionId)
    }

    override suspend fun getCredentialsByConnectionId(connectionId: String): List<Credential> {
        return mDbHelper.getCredentialsByConnectionId(connectionId)
    }
}