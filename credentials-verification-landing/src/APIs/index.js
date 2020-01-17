import { issueCredential } from './connector/connector';
import {
  getCredentials,
  createCredential,
  registerUser,
  createAndIssueCredential
} from './credentials/credentialsManager';
import {
  createStudent,
  generateConnectionToken,
  getStudentById
} from './credentials/studentsManager';

export const api = {
  generateConnectionToken,
  getCredentials,
  issueCredential,
  createCredential,
  registerUser,
  createStudent,
  createAndIssueCredential,
  getStudentById
};
