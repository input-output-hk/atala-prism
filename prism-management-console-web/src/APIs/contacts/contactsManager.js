import { ContactsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import {
  CONNECTED,
  CONTACT_PAGE_SIZE,
  CONTACT_SORTING_KEYS,
  MAX_CONTACT_PAGE_SIZE,
  PENDING_CONNECTION,
  REQUEST_AUTH_TIMEOUT_MS,
  SORTING_DIRECTIONS
} from '../../helpers/constants';
import {
  CreateContactRequest,
  CreateContactsRequest,
  GetContactsRequest,
  GetContactRequest,
  ConnectorRequestMetadata,
  UpdateContactRequest
} from '../../protos/console_api_pb';
import { GenerateConnectionTokenRequest } from '../../protos/connector_api_pb';
import { getAditionalTimeout } from '../../helpers/genericHelpers';
import { getProtoDate } from '../../helpers/formatters';
import { contactMapper } from '../helpers/contactHelpers';

const { FilterBy, SortBy } = GetContactsRequest;

const fieldKeys = {
  CREATED_AT: 1,
  NAME: 2,
  EXTERNAL_ID: 3
};

const sortByDirection = {
  ASCENDING: 1,
  DESCENDING: 2
};

const statusKeys = {
  [PENDING_CONNECTION]: 2,
  [CONNECTED]: 3
};

async function createContact(groupName, jsonData, externalId) {
  Logger.info(`Creating contact with externalId = ${externalId} for group ${groupName}`, jsonData);
  const req = new CreateContactRequest();
  if (groupName) req.setGroupName(groupName);
  req.setJsonData(JSON.stringify(jsonData));
  req.setExternalId(externalId);

  const { metadata, sessionError } = await this.auth.getMetadata(req);
  if (sessionError) return {};

  const res = await this.client.createContact(req, metadata);
  const { contact } = res.toObject();
  Logger.info('Created contact:', contact);
  return contact;
}

async function createContacts(groups, contacts) {
  const req = new CreateContactsRequest();

  req.setGroupsList(groups.map(g => g.id));
  req.setContactsList(
    contacts.map(c => {
      const contactObj = new CreateContactsRequest.Contact();
      contactObj.setExternalId(c.externalId);
      contactObj.setName(c.contactName);
      contactObj.setJsonData(JSON.stringify(c.jsonData));
      return contactObj;
    })
  );

  const connectionReq = new GenerateConnectionTokenRequest();
  connectionReq.setCount(contacts.length);

  const {
    metadata: { did, didKeyId, didSignature, requestNonce }
  } = await this.auth.getMetadata(connectionReq);

  const connectionTokenMetadataObj = new ConnectorRequestMetadata();
  connectionTokenMetadataObj.setDid(did);
  connectionTokenMetadataObj.setDidKeyId(didKeyId);
  connectionTokenMetadataObj.setDidSignature(didSignature);
  connectionTokenMetadataObj.setRequestNonce(requestNonce);
  req.setGenerateConnectionTokensRequestMetadata(connectionTokenMetadataObj);

  const { metadata, sessionError } = await this.auth.getMetadata(req);
  if (sessionError) return {};

  const res = await this.client.createContacts(req, metadata);

  const contactsCreated = res.getContactsCreated();
  Logger.info(`Created ${contactsCreated} contacts`);

  return contactsCreated;
}

async function getContacts({
  pageSize = CONTACT_PAGE_SIZE,
  scrollId,
  filter = {},
  sort = { field: CONTACT_SORTING_KEYS.name, direction: SORTING_DIRECTIONS.ascending }
}) {
  const { searchText, connectionStatus, createdAt, groupName } = filter;
  const { field, direction } = sort;

  const groupMessage = groupName ? ` from ${groupName}` : '';
  Logger.info(`Getting up to ${pageSize} contacts${groupMessage}`);

  const req = new GetContactsRequest();
  req.setLimit(pageSize);
  if (scrollId) req.setScrollId(scrollId);

  const filterBy = new FilterBy();
  filterBy.setGroupName(groupName);
  filterBy.setNameOrExternalId(searchText);
  filterBy.setConnectionStatus(statusKeys[connectionStatus]);

  if (createdAt) {
    const createdAtDate = getProtoDate(createdAt);
    filterBy.setCreatedAt(createdAtDate);
  }

  req.setFilterBy(filterBy);

  const sortBy = new SortBy();
  sortBy.setField(fieldKeys[field]);
  sortBy.setDirection(sortByDirection[direction]);
  req.setSortBy(sortBy);

  const timeout = REQUEST_AUTH_TIMEOUT_MS + getAditionalTimeout(pageSize);

  const { metadata, sessionError } = await this.auth.getMetadata(req, timeout);
  if (sessionError) return { contactsList: [] };

  const res = await this.client.getContacts(req, metadata);

  const { dataList, scrollId: newScrollId } = res.toObject();

  const contactsList = dataList.map(({ contact, ...rest }) => ({ ...contact, ...rest }));
  const mappedContacts = contactsList.map(contactMapper);

  Logger.info('Got contacts:', mappedContacts);

  return { contactsList: mappedContacts, newScrollId };
}

async function getContact(contactId) {
  Logger.info('Getting contact:', contactId);
  const req = new GetContactRequest();
  req.setContactId(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return { contactsList: [] };

  const res = await this.client.getContact(req, metadata);
  const { contact } = res.toObject();
  const mappedContact = contactMapper(contact);

  Logger.info('Got contact:', mappedContact);

  return mappedContact;
}

async function fetchMoreContactsRecursively(scrollId, groupName, acc, onFinish) {
  const { contactsList, newScrollId } = await this.getContacts({
    filter: { groupName },
    scrollId,
    pageSize: MAX_CONTACT_PAGE_SIZE
  });
  const mappedContacts = contactsList.map(contactMapper);
  const partialContactsArray = acc.concat(mappedContacts);
  if (contactsList.length < MAX_CONTACT_PAGE_SIZE) return onFinish(partialContactsArray);
  return this.fetchMoreContactsRecursively(newScrollId, groupName, partialContactsArray, onFinish);
}

function getAllContacts(groupName) {
  return new Promise(resolve => {
    this.fetchMoreContactsRecursively(null, groupName, [], resolve);
  });
}

async function updateContact(contactId, { externalId, name, jsonData }) {
  const req = new UpdateContactRequest();
  req.setContactId(contactId);
  if (externalId) req.setNewExternalId(externalId);
  if (name) req.setNewName(name);
  if (jsonData) req.setNewJsonData(jsonData);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return;

  return this.client.updateContact(req, metadata);
}

function ContactsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ContactsServicePromiseClient(this.config.grpcClient);
}

ContactsManager.prototype.createContact = createContact;
ContactsManager.prototype.createContacts = createContacts;
ContactsManager.prototype.getContacts = getContacts;
ContactsManager.prototype.getAllContacts = getAllContacts;
ContactsManager.prototype.fetchMoreContactsRecursively = fetchMoreContactsRecursively;
ContactsManager.prototype.getContact = getContact;
ContactsManager.prototype.updateContact = updateContact;

export default ContactsManager;
