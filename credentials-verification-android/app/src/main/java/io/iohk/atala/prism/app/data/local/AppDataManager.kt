package io.iohk.atala.prism.app.data.local

import androidx.lifecycle.LiveData
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.japi.ECKeyPair
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.DbHelper
import io.iohk.atala.prism.app.data.local.db.model.*
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.PreferencesHelper
import io.iohk.atala.prism.app.data.local.remote.ApiHelper
import io.iohk.atala.prism.app.viewmodel.dtos.ConnectionDataDto
import io.iohk.atala.prism.protos.*
import javax.inject.Inject

class AppDataManager @Inject constructor(dbHelper: DbHelper, private var apiHelper: ApiHelper, private var prefHelper: PreferencesHelper) : DataManager {

    private var mDbHelper: DbHelper = dbHelper


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

    override suspend fun getAllContacts(): List<Contact> {
        return mDbHelper.getAllContacts()
    }

    override suspend fun saveContact(contact: Contact): Long {
        return mDbHelper.saveContact(contact)
    }

    override suspend fun removeAllLocalData() {
        mDbHelper.removeAllLocalData()
    }

    override suspend fun getContactByConnectionId(connectionId: String): Contact? {
        return mDbHelper.getContactByConnectionId(connectionId)
    }

    override suspend fun insertIssuedCredentialsToAContact(contactId: Long, issuedCredentials: List<Credential>) {
        mDbHelper.insertIssuedCredentialsToAContact(contactId, issuedCredentials)
    }

    override suspend fun updateContact(contact: Contact) {
        mDbHelper.updateContact(contact)
    }

    override suspend fun contactById(contactId: Int): Contact? {
        return mDbHelper.contactById(contactId)
    }

    override suspend fun getAllCredentials(): List<Credential> {
        return mDbHelper.getAllCredentials()
    }

    override fun allCredentials(): LiveData<List<Credential>> {
        return mDbHelper.allCredentials()
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential? {
        return mDbHelper.getCredentialByCredentialId(credentialId)
    }

    override suspend fun deleteCredential(credential: Credential) {
        mDbHelper.deleteCredential(credential)
    }

    override suspend fun getCredentialsByConnectionId(connectionId: String): List<Credential> {
        return mDbHelper.getCredentialsByConnectionId(connectionId)
    }

    override suspend fun deleteContact(contact: Contact) {
        return mDbHelper.deleteContact(contact)
    }

    override fun allContacts(): LiveData<List<Contact>> {
        return mDbHelper.allContacts()
    }

    override suspend fun insertShareCredentialActivityHistories(credential: Credential, contacts: List<Contact>) {
        mDbHelper.insertShareCredentialActivityHistories(credential, contacts)
    }

    override suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) {
        mDbHelper.insertRequestedCredentialActivities(contact, credentials)
    }

    override suspend fun getCredentialsActivityHistoriesByConnection(connectionId: String): List<ActivityHistoryWithCredential> {
        return mDbHelper.getCredentialsActivityHistoriesByConnection(connectionId)
    }

    override suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact> {
        return mDbHelper.getContactsActivityHistoriesByCredentialId(credentialId)
    }

    override fun totalOfContacts(): LiveData<Int> {
        return mDbHelper.totalOfContacts()
    }

    override fun allIssuedCredentialsNotifications(): LiveData<List<ActivityHistoryWithCredential>> {
        return mDbHelper.allIssuedCredentialsNotifications()
    }

    override suspend fun clearCredentialNotifications(credentialId: String) {
        mDbHelper.clearCredentialNotifications(credentialId)
    }

    override fun activityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>> {
        return mDbHelper.activityHistories()
    }
}