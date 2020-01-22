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
import { getStudents } from './studentsManager';
import { getDid } from '../wallet/wallet';
import { CONNECTION_STATUSES, CONNECTION_ACCEPTED } from '../../helpers/constants';

const { config } = require('../config');

const credentialsService = new CredentialsServicePromiseClient(config.grpcClient, null, null);

export const getCredentials = async (limit, lastSeenCredentialId = null) => {
  Logger.info(`getting credentials from ${lastSeenCredentialId}, limit ${limit}`);
  const getCredentialsRequest = new GetCredentialsRequest();
  getCredentialsRequest.setLimit(limit);
  const result = await credentialsService.getCredentials(getCredentialsRequest, {
    userId: config.issuerId
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

const getAllStudents = async () => {
  const allStudents = [];
  const limit = 100;
  let response;

  // Since in the alpha version there will be only one group, all the created credentials
  // will belong to it. Therefore all the credentials must be created for every student.
  // Since there's no way to know how many students are in the database, every time the
  // students are recieved it must be checked whether if those were all the remaining
  // ones.
  do {
    // This gets the id of the last student so the backend can filter them
    const { id } = allStudents.length ? allStudents[allStudents.length - 1] : {};

    // The next 100 students are requested
    // eslint-disable-next-line no-await-in-loop
    response = await getStudents(config.issuerId, id, limit);

    const connectedStudents = response.filter(
      ({ connectionstatus }) => connectionstatus === CONNECTION_ACCEPTED
    );

    allStudents.push(...connectedStudents);

    // If less than the requested students are returned it means all the students have
    // already been brought
  } while (response.length === limit);

  return allStudents;
};

export const createCredential = async ({ title, enrollmentDate, graduationDate, groupName }) => {
  Logger.info('Creating credentials for the all the subjects as the issuer: ', config.issuerId);

  const enrollmentDateObject = new Date();
  const graduationDateObject = new Date();

  setDateInfoFromJSON(enrollmentDateObject, enrollmentDate);
  setDateInfoFromJSON(graduationDateObject, graduationDate);

  const allStudents = await getAllStudents();

  const credentialStudentsPromises = allStudents.map(student => {
    const createCredentialRequest = createAndPopulateCreationRequest(
      student.id,
      title,
      enrollmentDateObject,
      graduationDateObject,
      groupName
    );

    return credentialsService.createCredential(createCredentialRequest, {
      userId: config.issuerId
    });
  });

  return Promise.all(credentialStudentsPromises);
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
    enrollmentdate,
    graduationdate,
    groupname,
    id,
    issuerid,
    issuername,
    studentid,
    studentname,
    title
  } = credentialData;

  const did = await getDid();

  const issuerInfo = {
    name: issuername,
    did
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
    admissionDate: enrollmentdate,
    graduationDate: graduationdate
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
    userId: config.issuerId
  });

  const { id } = response.toObject();

  return id;
};
