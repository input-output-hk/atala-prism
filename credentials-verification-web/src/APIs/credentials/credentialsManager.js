import { CredentialsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { dayMonthYearBackendFormatter } from '../../helpers/formatters';
import credentialTypes from './credentialTypes';
import { FAILED, SUCCESS } from '../../helpers/constants';

const {
  GetGenericCredentialsRequest,
  CreateGenericCredentialRequest,
  GetContactCredentialsRequest
} = require('../../protos/cmanager_api_pb');
const {
  Credential,
  AtalaMessage,
  IssuerSentCredential
} = require('../../protos/credential_models_pb');

async function getCredentials(limit, lastSeenCredentialId = null) {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);

  const getCredentialsRequest = new GetGenericCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  getCredentialsRequest.setLastseencredentialid(lastSeenCredentialId);

  const metadata = await this.auth.getMetadata(getCredentialsRequest);

  const result = await this.client.getGenericCredentials(getCredentialsRequest, metadata);
  const credentialsList = result.getCredentialsList().map(cred => {
    const credentialData = JSON.parse(cred.getCredentialdata());
    const subjectId = cred.getContactid();
    const groupname = cred.getGroupname();
    const id = cred.getCredentialid();
    const subjectData = JSON.parse(cred.getContactdata());
    return Object.assign({ id, subjectId, groupname, subjectData }, credentialData);
  });

  return credentialsList;
}

function createAndPopulateCreationRequest(studentId, credentialData, groupName) {
  const createCredentialRequest = new CreateGenericCredentialRequest();

  createCredentialRequest.setSubjectid(studentId);
  createCredentialRequest.setCredentialdata(JSON.stringify(credentialData));
  createCredentialRequest.setGroupname(groupName);

  return createCredentialRequest;
}

async function createCredential({ title, enrollmentdate, graduationdate, groupName, students }) {
  Logger.info(
    'Creating credentials for the all the subjects as the issuer: ',
    this.config.issuerId
  );

  const credentialStudentsPromises = students.map(student => {
    const credentialData = { title, enrollmentdate, graduationdate };
    const createCredentialRequest = createAndPopulateCreationRequest(
      student.id,
      credentialData,
      groupName
    );

    return this.auth
      .getMetadata(createCredentialRequest)
      .then(metadata => this.client.createGenericCredential(createCredentialRequest, metadata));
  });

  return Promise.all(credentialStudentsPromises);
}

async function createBatchOfCredentials(credentialsData) {
  Logger.info(`Creating ${credentialsData?.length} credential(s):`);

  const credentialStudentsPromises = credentialsData.map(
    ({ externalid, contactid, ...json }, index) => {
      const createCredentialRequest = new CreateGenericCredentialRequest();

      createCredentialRequest.setContactid(contactid);
      createCredentialRequest.setExternalid(externalid);
      createCredentialRequest.setCredentialdata(JSON.stringify(json));

      return this.auth
        .getMetadata(createCredentialRequest)
        .then(metadata => {
          Logger.info(`${index}) externalid: ${externalid}, issuer: ${metadata.did}'`);
          return this.client.createGenericCredential(createCredentialRequest, metadata);
        })
        .then(response => ({ externalid, status: SUCCESS, response }))
        .catch(error => {
          Logger.error(error);
          return { externalid, status: FAILED, error };
        });
    }
  );

  return Promise.all(credentialStudentsPromises);
}

function populateCredential({ issuerInfo, subjectInfo, additionalInfo }) {
  const credentialType = ['VerifiableCredential', 'AirsideDegreeCredential'];
  const credential = new Credential();
  const credentialDocument = {
    type: credentialType,
    issuer: issuerInfo,
    issuanceDate: dayMonthYearBackendFormatter(additionalInfo.graduationDate),
    credentialSubject: {
      name: subjectInfo.fullName,
      degreeAwarded: 'Bachelor of Science',
      degreeResult: 'First class honors',
      graduationYear: additionalInfo.graduationDate.year
    }
  };

  credential.setTypeid(credentialType.join('/'));
  credential.setCredentialdocument(JSON.stringify(credentialDocument));

  return credential;
}

function parseAndPopulate(credentialData, studentData, did) {
  const { enrollmentdate, graduationdate, id, issuername, title } = credentialData;

  const issuerInfo = {
    name: issuername,
    did
  };

  const { fullName } = studentData;

  const subjectInfo = {
    fullName
  };

  const additionalInfo = {
    degree: title,
    issueNumber: id,
    admissionDate: enrollmentdate,
    graduationDate: graduationdate
  };

  return populateCredential({
    issuerInfo,
    subjectInfo,
    additionalInfo
  });
}

function getCredentialBinary(connectionData, studentData, did) {
  const atalaMessage = new AtalaMessage();
  const issuerCredential = new IssuerSentCredential();

  const credential = parseAndPopulate(connectionData, studentData, did);
  issuerCredential.setCredential(credential);

  atalaMessage.setIssuersentcredential(issuerCredential);

  return atalaMessage.serializeBinary();
}

function getCredentialTypes() {
  return credentialTypes;
}

async function getContactCredentials(contactId) {
  Logger.info('Getting credentials for contact:', contactId);
  const req = new GetContactCredentialsRequest();
  req.setContactid(contactId);

  const metadata = await this.auth.getMetadata(req);

  const res = await this.client.getContactCredentials(req, metadata);
  const credentialsList = res.getGenericcredentialsList();
  Logger.info('Got credentials:', credentialsList);

  return credentialsList;
}

function CredentialsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsServicePromiseClient(config.grpcClient, null, null);
}

CredentialsManager.prototype.getCredentials = getCredentials;
CredentialsManager.prototype.createCredential = createCredential;
CredentialsManager.prototype.createBatchOfCredentials = createBatchOfCredentials;
CredentialsManager.prototype.getCredentialBinary = getCredentialBinary;
CredentialsManager.prototype.getCredentialTypes = getCredentialTypes;
CredentialsManager.prototype.getContactCredentials = getContactCredentials;

export default CredentialsManager;
