import { omit } from 'lodash';

// TODO: adapt the rest of the frontend so this isn't necessary
export function contactMapper(contact) {
  const jsondata = JSON.parse(contact.jsonData);
  const { status: holderStatus, connectionstatus, contactid, ...rest } = {
    ...omit(contact, ['jsondata', 'holderid']),
    ...jsondata,
    ...parseName(jsondata)
  };
  const status = holderStatus !== undefined ? holderStatus : connectionstatus;
  return Object.assign({}, rest, { key: contactid, status, contactid });
}

export const parseName = ({ contactName = '', firstName = '', lastName = '', midNames = '' }) =>
  contactName ? deconstructFullName(contactName) : constructFullName(firstName, midNames, lastName);

export const deconstructFullName = fullnameInput => {
  const wordSeparator = '@';
  const inputAsString = fullnameInput.toString();

  const [joinedFirstName, ...restNames] = inputAsString.split(' ');
  const joinedMidNames = restNames.slice(0, restNames.length - 1).join(' ');
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
