import { svgPathToEncodedBase64 } from '../../helpers/genericHelpers';
import hardCodedCredentialTypes from '../credentials/mocks/hardcodedCredentialTypes';

const credentialTypeEquivalents = {
  id: hardCodedCredentialTypes.governmentId,
  educational: hardCodedCredentialTypes.educational,
  employment: hardCodedCredentialTypes.proofOfEmployment,
  health: hardCodedCredentialTypes.healthIsurance
};

const placeholdersReplacements = {
  id: {
    identityNumber: '{{identityNumber}}',
    fullName: '{{fullName}}',
    dateOfBirth: '{{dateOfBirth}}',
    expiryDate: '{{expiryDate}}',
    issuerName: '{{issuerName}}'
  },
  educational: {
    degreeName: '{{degreeName}}',
    universityName: '{{universityName}}',
    award: '{{award}}',
    fullName: '{{fullName}}',
    startDate: '{{startDate}}',
    graduationDate: '{{graduationDate}}'
  },
  employment: {
    issuerName: '{{issuerName}}',
    employeeName: '{{employeeName}}',
    employerAddress: '{{employerAddress}}',
    employmentStatus: '{{employmentStatus}}',
    employmentStartDate: '{{employmentStartDate}}'
  },
  health: {
    fullName: '{{fullName}}',
    productClass: '{{productClass}}',
    policyNumber: '{{policyNumber}}',
    expiryDate: '{{expiryDate}}',
    issuerName: '{{issuerName}}'
  }
};

const b64ImagePrefix = 'data:image/svg+xml;base64';

export const adaptCredentialType = ({ id, name, icon, ...rest } = {}) => ({
  ...rest,
  id,
  name,
  icon: icon && `${b64ImagePrefix},${icon}`,
  ...credentialTypeEquivalents[name],
  placeholders: placeholdersReplacements[name]
});

export const getCredentialTypeAttributes = async credentialList => {
  const { name, icon } = credentialList[0]?.credentialData.credentialTypeDetails;

  const encodedIcon = await svgPathToEncodedBase64(icon);

  return {
    credentialTypeName: name,
    credentialTypeIcon: encodedIcon,
    credentialTypeIconFormat: 'svg'
  };
};

export const splitBase64String = b64String => {
  const [prefix, data] = b64String.split(',');
  return {
    prefix,
    data
  };
};
