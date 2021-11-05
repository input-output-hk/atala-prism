import { omit } from 'lodash';

// TODO: adapt the rest of the frontend so this isn't necessary
export function contactMapper(contact) {
  const jsondata = JSON.parse(contact.jsonData || '{}');
  const { connectionStatus, contactId, ...rest } = {
    ...omit(contact, ['jsondata', 'holderid']),
    ...jsondata,
    ...parseName(contact)
  };
  return { ...rest, key: contactId, connectionStatus, contactId };
}

export const parseName = ({ name = '', firstName = '', lastName = '', midNames = '' }) =>
  name ? deconstructFullName(name) : constructFullName(firstName, midNames, lastName);

export const deconstructFullName = fullnameInput => {
  const wordSeparator = '@';
  const inputAsString = fullnameInput.toString();

  const [joinedFirstName, ...restNames] = inputAsString.split(' ');
  const joinedMidNames = restNames.slice(0, restNames.length - 1).join(' ');
  // eslint-disable-next-line no-magic-numbers
  const joinedLastName = restNames.slice(-1).join(' ');

  const firstName = joinedFirstName.split(wordSeparator).join(' ');
  const midNames = joinedMidNames.split(wordSeparator).join(' ');
  const lastName = joinedLastName.split(wordSeparator).join(' ');
  const contactName = inputAsString.split(wordSeparator).join(' ');

  return { firstName, midNames, lastName, contactName };
};

export const constructFullName = (firstName, midNames, lastName) => ({
  firstName,
  midNames,
  lastName,
  contactName: [firstName, midNames, lastName].filter(Boolean).join(' ')
});
