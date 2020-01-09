/* eslint import/no-unresolved: 0 */ // --> OFF
import { CredentialsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';
import {
  GetCredentialsRequest,
  CreateCredentialRequest,
  RegisterRequest,
  Date
} from '../../protos/connector/credentialsManager_pb';
import {
  Credential,
  IssuerData,
  SubjectData,
  PersonalId,
  SentCredential,
  IssuerSentCredential,
  Signer
} from '../../protos/credentials/credential_pb';
import Logger from '../../helpers/Logger';
import { setDateInfoFromJSON } from '../helpers';
import { issueCredential } from '../connector/connector';
import { getStudentById } from './studentsManager';
import { HARDCODED_LIMIT, LANDING_TITLE, LANDING_GROUP } from '../../helpers/constants';

const { REACT_APP_GRPC_CLIENT, REACT_APP_ISSUER_ID } = window._env_;
const issuerId = REACT_APP_ISSUER_ID;
const credentialsService = new CredentialsServicePromiseClient(REACT_APP_GRPC_CLIENT, null, null);

export const getCredentials = async (limit = 100, lastSeenCredentialId = null) => {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);
  const getCredentialsRequest = new GetCredentialsRequest();
  getCredentialsRequest.setLimit(HARDCODED_LIMIT);
  const result = await credentialsService.getCredentials(getCredentialsRequest, {
    userId: issuerId
  });
  const { credentialsList } = result.toObject();

  return { credentials: credentialsList, count: credentialsList.length };
};

const createAndPopulateCreationRequest = (
  studentId,
  title,
  enrollmentDate,
  graduationDate,
  groupName
) => {
  const createCredentialRequest = new CreateCredentialRequest();

  createCredentialRequest.setStudentid(studentId);
  createCredentialRequest.setTitle(title);
  createCredentialRequest.setEnrollmentdate(enrollmentDate);
  createCredentialRequest.setGraduationdate(graduationDate);
  createCredentialRequest.setGroupname(groupName);

  return createCredentialRequest;
};

export const createAndIssueCredential = async credentialData => {
  const { studentId } = credentialData;
  const { connectionid } = await getStudentById(studentId);

  if (!connectionid) throw new Error('User not connected');

  const dataToIssue = Object.assign({}, credentialData, {
    title: LANDING_TITLE,
    groupName: LANDING_GROUP
  });
  createCredential(dataToIssue)
    .then(() => issueCredential(dataToIssue))
    .catch(error => Logger.error(error));
};

export const createCredential = async credentialData => {
  const { enrollmentDate, graduationDate, groupName, studentId } = credentialData;

  const enrollmentDateObject = new Date();
  const graduationDateObject = new Date();

  setDateInfoFromJSON(enrollmentDateObject, enrollmentDate);
  setDateInfoFromJSON(graduationDateObject, graduationDate);

  const createCredentialRequest = createAndPopulateCreationRequest(
    studentId,
    LANDING_TITLE,
    enrollmentDateObject,
    graduationDateObject,
    groupName
  );

  return credentialsService.createCredential(createCredentialRequest, {
    userId: issuerId
  });
};

const IssuerTypes = {
  UNIVERSITY: 0,
  SCHOOL: 1
};

const translateIssuerType = type => IssuerTypes[type];

const populateIssuer = ({
  type = 'UNIVERSITY',
  legalName = 'Free University Tbilisi',
  name,
  did
}) => {
  const issuerData = new IssuerData();

  issuerData.setIssuertype(translateIssuerType(type));
  issuerData.setIssuerlegalname(legalName);
  issuerData.setAcademicauthority(name);
  issuerData.setDid(did);

  return issuerData;
};

const DocType = {
  NationalIdCard: 0,
  Passporrt: 1
};

const translateDocType = type => DocType[type];

const populateDate = ({ year = 1999, month = 5, day = 5 }) => {
  const date = new Date();

  date.setYear(year);
  date.setMonth(month);
  date.setDay(day);

  return date;
};

const populatePersonalId = ({ id = '12345678', type = 'NationalIdCard' }) => {
  const personalId = new PersonalId();

  personalId.setId(id);
  personalId.setDocumenttype(translateDocType(type));

  return personalId;
};

const populateSubject = ({ names, surnames, birthDate = {}, idInfo = {} }) => {
  const subjectData = new SubjectData();

  const dateOfBirth = populateDate(birthDate);
  subjectData.setDateofbirth(dateOfBirth);

  const personalId = populatePersonalId(idInfo);
  subjectData.setIddocument(personalId);

  subjectData.setNamesList(names);
  subjectData.setSurnamesList(surnames);

  return subjectData;
};

const populateSigner = ({ names, surnames, role, did, title }) => {
  const signer = Signer();

  signer.setNames(names);
  signer.setSurnames(surnames);
  signer.setRole(role);
  signer.setDid(did);
  signer.setTittle(title);

  return signer;
};

const populateSigners = (signerArray = []) => signerArray.map(populateSigner);

const setAdditionalInfo = (
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
) => {
  credential.setGrantingdecision(grantingDecision);
  credential.setDegreeawarded(degree);
  credential.setAdditionalspeciality(speciality);
  credential.setIssuenumber(issueNumber);
  credential.setRegistrationnumber(registrationNumber);
  credential.setDecisionnumber(decisionNumber);
  // TODO Clarify
  credential.setYearcompletedbystudent('');
  credential.setDescription(description);
};

const setDates = (
  credential,
  { issuedOn = {}, expiresOn = {}, admissionDate, graduationDate, attainmentDate = {} }
) => {
  credential.setIssuedon(populateDate(issuedOn));
  credential.setExpireson(populateDate(expiresOn));
  credential.setAdmissiondate(populateDate(admissionDate));
  credential.setGraduationdate(populateDate(graduationDate));
  credential.setAttainmentdate(populateDate(attainmentDate));
};

const populateCredential = ({ issuerInfo, subjectInfo, signersInfo, additionalInfo }) => {
  const credential = new Credential();

  const issuerData = populateIssuer(issuerInfo);
  credential.setIssuertype(issuerData);

  const subjectData = populateSubject(subjectInfo);
  credential.setSubjectdata(subjectData);

  const signers = populateSigners(signersInfo);
  credential.setSigningauthoritiesList(signers);

  setAdditionalInfo(credential, additionalInfo);
  setDates(credential, additionalInfo);

  return credential;
};

const getNamesAndSurames = fullName => {
  const wordsSeparator = '@';
  const [joinedNames, joinedSurnames] = fullName.split(' ');

  const names = joinedNames.split(wordsSeparator);
  const surnames = joinedSurnames.split(wordsSeparator);

  return { names, surnames };
};

const parseAndPopulate = async (credentialData, studentData) => {
  const {
    enrollmentDate,
    graduationDate,
    groupname,
    id = 'f435d7ef-9854-4ccb-b7d8-f1d4d3c2e95f',
    issuerid,
    studentid,
    studentname,
    title
  } = credentialData;

  const { REACT_APP_ISSUER_NAME, REACT_APP_ISSUER_DID } = window._env_;

  const issuerInfo = {
    name: REACT_APP_ISSUER_NAME,
    did: REACT_APP_ISSUER_DID
  };

  const {
    admissiondate,
    connectionid,
    connectionstatus,
    connectiontoken,
    email,
    fullname,
    universityassignedid
  } = studentData;

  const subjectInfo = {
    ...getNamesAndSurames(fullname)
  };

  const additionalInfo = {
    degree: title,
    issueNumber: id,
    admissionDate: enrollmentDate,
    graduationDate
  };

  const credential = await populateCredential({
    issuerInfo,
    subjectInfo,
    additionalInfo
  });

  return credential;
};

export const getCredentialBinary = async (connectionData, studentData) => {
  const sentCredential = new SentCredential();
  const issuerCredential = new IssuerSentCredential();

  const credential = await parseAndPopulate(connectionData, studentData);
  issuerCredential.setCredential(credential);

  sentCredential.setIssuersentcredential(issuerCredential);

  return sentCredential.serializeBinary();
};

export const registerUser = async (name, did, file) => {
  const registerRequest = new RegisterRequest();
  const logo = new TextEncoder().encode(file);

  registerRequest.setName(name);
  registerRequest.setDid(did);
  registerRequest.setLogo(logo);

  const response = await credentialsService.register(registerRequest, {
    userId: issuerId
  });

  const { id } = response.toObject();

  return id;
};
