import { omit } from 'lodash';

// TODO: adapt the rest of the frontend so this isn't necessary
export function contactMapper(contact) {
  const jsondata = JSON.parse(contact.jsondata);
  const { status: holderStatus, connectionstatus, contactid, ...rest } = {
    ...omit(contact, ['jsondata', 'holderid']),
    ...jsondata,
    ...parseName(jsondata)
  };
  const status = holderStatus !== undefined ? holderStatus : connectionstatus;

  return Object.assign({}, rest, { key: contactid, status, contactid });
}

// TODO: adapt the rest of the frontend so this isn't necessary
export function genericCredentialToStudentCredential(credential) {
  const credentialData = JSON.parse(credential.credentialdata);
  const subjectdata = JSON.parse(credential.subjectdata);
  return {
    ...omit(credential, ['credentialdata', 'subjectdata']),
    ...credentialData,
    credentialid: credential.id,
    studentname: subjectdata.fullname,
    studentid: subjectdata.studentid
  };
}

export const parseName = ({ fullname = '', firstName = '', lastName = '', midNames = '' }) =>
  fullname ? deconstructFullName(fullname) : constructFullName(firstName, midNames, lastName);

export const deconstructFullName = fullnameInput => {
  const wordSeparator = '@';
  const inputAsString = fullnameInput.toString();

  const [joinedFirstName, ...restNames] = inputAsString.split(' ');
  const joinedMidNames = restNames.slice(0, restNames.length - 1).join(' ');
  const joinedLastName = restNames.slice(-1).join(' ');

  const firstName = joinedFirstName.split(wordSeparator).join(' ');
  const midNames = joinedMidNames.split(wordSeparator).join(' ');
  const lastName = joinedLastName.split(wordSeparator).join(' ');
  const fullname = inputAsString.split(wordSeparator).join(' ');

  return { firstName, midNames, lastName, fullname };
};

export const constructFullName = (firstName, midNames, lastName) => ({
  firstName,
  midNames,
  lastName,
  fullname: [firstName, midNames, lastName].filter(Boolean).join(' ')
});
