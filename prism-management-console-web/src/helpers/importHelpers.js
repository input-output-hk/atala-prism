import _ from 'lodash';
import { COMMON_CONTACT_HEADERS } from './constants';
import { dateFormat } from './formatters';

const blankContact = {
  externalId: '',
  contactName: ''
};

export const createBlankContact = key => ({
  ...blankContact,
  key
});

export const createBlankCredential = (key, credentialType) => ({
  ...blankContact,
  ...credentialType?.fields.map(f => ({ [f.key]: '' })),
  key
});

export const addNewContact = contacts => {
  const { key = 0 } = _.last(contacts) || {};

  const newContact = createBlankContact(key + 1);
  return contacts.concat(newContact);
};

export const deleteContact = (key, contacts) => {
  const filteredContacts = contacts.filter(({ key: contactKey }) => key !== contactKey);
  const last = _.last(contacts) || {};

  return filteredContacts.length ? filteredContacts : [createBlankContact(last.key + 1)];
};

export const addNewCredential = credentialsData => {
  const { key = 0 } = _.last(credentialsData) || {};

  const newCredential = createBlankCredential(key + 1);
  return credentialsData.concat(newCredential);
};

export const deleteCredential = (key, credentialsData) => {
  const filteredCredentials = credentialsData.filter(
    ({ key: credentialKey }) => key !== credentialKey
  );
  const last = _.last(credentialsData) || {};

  return filteredCredentials.length ? filteredCredentials : [createBlankCredential(last.key + 1)];
};

export const processCredentials = (credentials, credentialType) => {
  const fieldsToInclude = COMMON_CONTACT_HEADERS.concat(credentialType.fields.map(f => f.key));

  const dateFields = credentialType.fields
    .filter(({ type }) => type === 'date')
    .map(({ key }) => key);

  const trimmedCredentials = credentials.map(r =>
    _.pickBy(r, (_value, key) => fieldsToInclude.includes(key))
  );

  const parsedCredentials = trimmedCredentials.map(c =>
    dateFields.reduce((acc, df) => Object.assign(acc, { [df]: dateFormat(c[df]) }), c)
  );

  return parsedCredentials;
};
