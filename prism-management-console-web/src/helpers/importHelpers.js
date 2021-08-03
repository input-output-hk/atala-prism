import _ from 'lodash';

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
