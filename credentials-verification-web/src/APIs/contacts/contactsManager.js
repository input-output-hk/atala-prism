import { ConsoleServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { HOLDER_PAGE_SIZE } from '../../helpers/constants';
import {
  CreateContactRequest,
  GetContactsRequest,
  GetContactRequest,
  GenerateConnectionTokenForContactRequest
} from '../../protos/console_api_pb';

async function generateConnectionToken(contactId) {
  Logger.info('Generating connection token for:', contactId);
  const req = new GenerateConnectionTokenForContactRequest();
  req.setContactid(contactId);

  const metadata = await this.auth.getMetadata(req);

  const res = await this.client.generateConnectionTokenForContact(req, metadata);
  const token = res.getToken();
  Logger.info('Created connection token:', token);

  return token;
}

async function createContact(groupName, jsonData, externalId) {
  Logger.info(`Creating contact ${jsonData} as ${externalId} for group ${groupName}`);
  const req = new CreateContactRequest();
  req.setGroupname(groupName);
  req.setJsondata(JSON.stringify(jsonData));
  req.setExternalid(externalId);

  const metadata = await this.auth.getMetadata(req);

  const res = await this.client.createContact(req, metadata);
  const { contact } = res.toObject();
  Logger.info('Created contact:', contact);
}

async function getContacts(lastSeenContactId, limit = HOLDER_PAGE_SIZE, groupName) {
  Logger.info(`Getting up to ${limit} contacts from ${lastSeenContactId} for group ${groupName}`);
  const req = new GetContactsRequest();
  req.setLimit(limit);
  req.setLastseencontactid(lastSeenContactId);
  if (groupName) req.setGroupname(groupName);

  const metadata = await this.auth.getMetadata(req);

  const res = await this.client.getContacts(req, metadata);
  const { contactsList } = res.toObject();
  Logger.info('Got contacts:', contactsList);

  return contactsList;
}

async function getContact(contactId) {
  Logger.info('Getting contact:', contactId);
  const req = new GetContactRequest();
  req.setContactid(contactId);

  const metadata = await this.auth.getMetadata(req);

  const res = await this.client.getContact(req, metadata);
  const { contact } = res.toObject();
  Logger.info('Got contact:', contact);

  return contact;
}

function ContactsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConsoleServicePromiseClient(this.config.grpcClient);
}

ContactsManager.prototype.generateConnectionToken = generateConnectionToken;
ContactsManager.prototype.createContact = createContact;
ContactsManager.prototype.getContacts = getContacts;
ContactsManager.prototype.getContact = getContact;

export default ContactsManager;
