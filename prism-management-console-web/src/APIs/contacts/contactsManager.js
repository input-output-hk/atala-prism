import { ContactsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { CONTACT_PAGE_SIZE, REQUEST_AUTH_TIMEOUT_MS } from '../../helpers/constants';
import {
  CreateContactRequest,
  GetContactsRequest,
  GetContactRequest,
  GenerateConnectionTokenForContactRequest
} from '../../protos/console_api_pb';
import { getAditionalTimeout } from '../../helpers/genericHelpers';

async function generateConnectionToken(contactId) {
  Logger.info('Generating connection token for:', contactId);
  const req = new GenerateConnectionTokenForContactRequest();
  req.setContactId(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return '';

  const res = await this.client.generateConnectionTokenForContact(req, metadata);
  const token = res.getToken();
  Logger.info('Created connection token:', token);

  return token;
}

async function createContact(groupName, jsonData, externalid) {
  Logger.info(`Creating contact with externalId = ${externalid} for group ${groupName}`, jsonData);
  const req = new CreateContactRequest();
  if (groupName) req.setGroupName(groupName);
  req.setJsonData(JSON.stringify(jsonData));
  req.setExternalId(externalid);

  const { metadata, sessionError } = await this.auth.getMetadata(req);
  if (sessionError) return {};

  const res = await this.client.createContact(req, metadata);
  const { contact } = res.toObject();
  Logger.info('Created contact:', contact);
  return contact;
}

async function getContacts(lastSeenContactId, limit = CONTACT_PAGE_SIZE, groupName) {
  Logger.info(`Getting up to ${limit} contacts from ${lastSeenContactId} for group ${groupName}`);
  const req = new GetContactsRequest();
  req.setLimit(limit);
  req.setLastSeenContactId(lastSeenContactId);
  if (groupName) req.setGroupName(groupName);

  const timeout = REQUEST_AUTH_TIMEOUT_MS + getAditionalTimeout(limit);

  const { metadata, sessionError } = await this.auth.getMetadata(req, timeout);
  if (sessionError) return [];

  const res = await this.client.getContacts(req, metadata);
  const { contactsList } = res.toObject();
  Logger.info('Got contacts:', contactsList);

  return contactsList;
}

async function getContact(contactId) {
  Logger.info('Getting contact:', contactId);
  const req = new GetContactRequest();
  req.setContactId(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(req, REQUEST_AUTH_TIMEOUT_MS);
  if (sessionError) return {};

  const res = await this.client.getContact(req, metadata);
  const { contact } = res.toObject();
  Logger.info('Got contact:', contact);

  return contact;
}

function ContactsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ContactsServicePromiseClient(this.config.grpcClient);
}

ContactsManager.prototype.generateConnectionToken = generateConnectionToken;
ContactsManager.prototype.createContact = createContact;
ContactsManager.prototype.getContacts = getContacts;
ContactsManager.prototype.getContact = getContact;

export default ContactsManager;
