import { CredentialsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';
import Logger from '../../helpers/Logger';
import { setDateInfoFromJSON } from '../helpers';

const { Date } = require('../../protos/common_models_pb');
const { GetCredentialsRequest, CreateCredentialRequest } = require('../../protos/cmanager_api_pb');
const {
  AlphaCredential,
  IssuerData,
  SubjectData,
  PersonalId,
  AtalaMessage,
  IssuerSentCredential,
  Signer
} = require('../../protos/credential_pb');

async function getCredentials(limit, lastSeenCredentialId = null) {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);

  const getCredentialsRequest = new GetCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  getCredentialsRequest.setLastseencredentialid(lastSeenCredentialId);

  const result = await this.client.getCredentials(getCredentialsRequest, this.auth.getMetadata());
  const { credentialsList } = result.toObject();

  return { credentials: credentialsList, count: credentialsList.length };
}

function createAndPopulateCreationRequest(
  studentId,
  title,
  enrollmentDate,
  graduationDate,
  groupName
) {
  const createCredentialRequest = new CreateCredentialRequest();

  createCredentialRequest.setStudentid(studentId);
  createCredentialRequest.setTitle(title);
  createCredentialRequest.setEnrollmentdate(enrollmentDate);
  createCredentialRequest.setGraduationdate(graduationDate);
  createCredentialRequest.setGroupname(groupName);

  return createCredentialRequest;
}

async function createCredential({ title, enrollmentDate, graduationDate, groupName, students }) {
  Logger.info(
    'Creating credentials for the all the subjects as the issuer: ',
    this.config.issuerId
  );

  const enrollmentDateObject = new Date();
  const graduationDateObject = new Date();

  setDateInfoFromJSON(enrollmentDateObject, enrollmentDate);
  setDateInfoFromJSON(graduationDateObject, graduationDate);

  const credentialStudentsPromises = students.map(student => {
    const createCredentialRequest = createAndPopulateCreationRequest(
      student.id,
      title,
      enrollmentDateObject,
      graduationDateObject,
      groupName
    );

    return this.client.createCredential(createCredentialRequest, this.auth.getMetadata());
  });

  return Promise.all(credentialStudentsPromises);
}

const IssuerTypes = {
  UNIVERSITY: 0,
  SCHOOL: 1
};

function translateIssuerType(type) {
  return IssuerTypes[type];
}

function populateIssuer({ type = 'UNIVERSITY', legalName = 'Free University Tbilisi', name, did }) {
  const issuerData = new IssuerData();

  issuerData.setIssuertype(translateIssuerType(type));
  issuerData.setIssuerlegalname(legalName);
  issuerData.setAcademicauthority(name);
  issuerData.setDid(did);

  return issuerData;
}

const DocType = {
  NationalIdCard: 0,
  Passporrt: 1
};

function translateDocType(type) {
  return DocType[type];
}

function populateDate({ year = 1999, month = 5, day = 5 }) {
  const date = new Date();

  date.setYear(year);
  date.setMonth(month);
  date.setDay(day);

  return date;
}

function populatePersonalId({ id = '12345678', type = 'NationalIdCard' }) {
  const personalId = new PersonalId();

  personalId.setId(id);
  personalId.setDocumenttype(translateDocType(type));

  return personalId;
}

function populateSubject({ names, surnames, birthDate = {}, idInfo = {} }) {
  const subjectData = new SubjectData();

  const dateOfBirth = populateDate(birthDate);
  subjectData.setDateofbirth(dateOfBirth);

  const personalId = populatePersonalId(idInfo);
  subjectData.setIddocument(personalId);

  subjectData.setNamesList(names);
  subjectData.setSurnamesList(surnames);

  return subjectData;
}

function populateSigner({ names, surnames, role, did, title }) {
  const signer = Signer();

  signer.setNames(names);
  signer.setSurnames(surnames);
  signer.setRole(role);
  signer.setDid(did);
  signer.setTittle(title);

  return signer;
}

function populateSigners(signerArray = []) {
  return signerArray.map(populateSigner);
}

function setAdditionalInfo(
  credential,
  {
    grantingDecision = 'Placeholder Title',
    degree,
    speciality = '',
    issueNumber,
    registrationNumber = '',
    decisionNumber = '',
    description = ''
  }
) {
  credential.setGrantingdecision(grantingDecision);
  credential.setDegreeawarded(degree);
  credential.setAdditionalspeciality(speciality);
  credential.setIssuenumber(issueNumber);
  credential.setRegistrationnumber(registrationNumber);
  credential.setDecisionnumber(decisionNumber);
  // TODO Clarify
  credential.setYearcompletedbystudent('');
  credential.setDescription(description);
}

function setDates(
  credential,
  { issuedOn = {}, expiresOn = {}, admissionDate, graduationDate, attainmentDate = {} }
) {
  credential.setIssuedon(populateDate(issuedOn));
  credential.setExpireson(populateDate(expiresOn));
  credential.setAdmissiondate(populateDate(admissionDate));
  credential.setGraduationdate(populateDate(graduationDate));
  credential.setAttainmentdate(populateDate(attainmentDate));
}

function populateCredential({ issuerInfo, subjectInfo, signersInfo, additionalInfo }) {
  const credential = new AlphaCredential();

  const issuerData = populateIssuer(issuerInfo);
  credential.setIssuertype(issuerData);

  const subjectData = populateSubject(subjectInfo);
  credential.setSubjectdata(subjectData);

  const signers = populateSigners(signersInfo);
  credential.setSigningauthoritiesList(signers);

  setAdditionalInfo(credential, additionalInfo);
  setDates(credential, additionalInfo);

  return credential;
}

export function getNamesAndSurnames(fullName) {
  const wordsSeparator = '@';
  const [joinedNames, joinedSurnames = ''] = fullName.split(' ');

  const names = joinedNames.split(wordsSeparator);
  const surnames = joinedSurnames.split(wordsSeparator);

  return { names, surnames };
}

function parseAndPopulate(credentialData, studentData, did) {
  const { enrollmentdate, graduationdate, id, issuername, title } = credentialData;

  const issuerInfo = {
    name: issuername,
    did
  };

  const { fullname } = studentData;

  const subjectInfo = {
    ...getNamesAndSurnames(fullname)
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
  issuerCredential.setAlphacredential(credential);

  atalaMessage.setIssuersentcredential(issuerCredential);

  return atalaMessage.serializeBinary();
}

function CredentialsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialsServicePromiseClient(config.grpcClient, null, null);
}

CredentialsManager.prototype.getCredentials = getCredentials;
CredentialsManager.prototype.createCredential = createCredential;
CredentialsManager.prototype.getCredentialBinary = getCredentialBinary;

export default CredentialsManager;
