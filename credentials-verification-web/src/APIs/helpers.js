import { omit } from 'lodash';

// TODO: adapt the rest of the frontend so this isn't necessary
export function subjectToStudent(subject) {
  const jsondata = JSON.parse(subject.jsondata);
  return {
    ...omit(subject, ['jsondata']),
    ...jsondata
  };
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
