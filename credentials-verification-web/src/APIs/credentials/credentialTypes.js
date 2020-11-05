import governmentIdLogo from '../../images/government-id-logo.svg';
import educationalLogo from '../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../images/health-insurance-logo.svg';
import governmentIdSample from '../../images/government-id-sample.svg';
import educationalSample from '../../images/educational-credential-sample.svg';
import proofOfEmploymentSample from '../../images/proof-of-employment-sample.svg';
import healthInsuranceSample from '../../images/health-insurance-sample.svg';

const governmentId = {
  id: 1,
  name: 'credentials.type.governmentId',
  logo: governmentIdLogo,
  sampleImage: governmentIdSample,
  fields: [
    {
      key: 'idNumber',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'fullname',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'dateOfBirth',
      type: 'date',
      validations: ['required', 'pastDate']
    },
    {
      key: 'expirationDate',
      type: 'date',
      validations: ['required', 'futureDate']
    },
    {
      key: 'photo',
      type: 'file',
      validations: []
    }
  ],
  placeholders: {
    idNumber: '{{credentialSubject.identityNumber}}',
    fullname: '{{credentialSubject.name}}',
    dateOfBirth: '{{credentialSubject.dateOfBirth}}',
    expirationDate: '{{expiryDate}}'
  }
};

const educational = {
  id: 2,
  name: 'credentials.type.educational',
  logo: educationalLogo,
  sampleImage: educationalSample,
  fields: [
    {
      key: 'degree',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'award',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'studentId',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'startDate',
      type: 'date',
      validations: ['required', 'pastDate']
    },
    {
      key: 'graduationDate',
      type: 'date',
      validations: ['required', 'pastDate']
    }
  ],
  placeholders: {
    degree: '{{credentialSubject.degreeAwarded}}',
    award: '{{credentialSubject.degreeResult}}',
    startDate: '{{credentialSubject.startDate}}',
    graduationDate: '{{issuanceDate}}',
    contactName: '{{credentialSubject.name}}'
  }
};

const proofOfEmployment = {
  id: 3,
  name: 'credentials.type.proofOfEmployment',
  logo: proofOfEmploymentLogo,
  sampleImage: proofOfEmploymentSample,
  fields: [
    {
      key: 'adress',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'status',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'startDate',
      type: 'date',
      validations: ['required', 'pastDate']
    }
  ],
  placeholders: {
    adress: '{{issuer.address}}',
    status: '{{employmentStatus}}',
    startDate: '{{employmentStartDate}}',
    contactName: '{{credentialSubject.name}}'
  }
};

const healthIsurance = {
  id: 4,
  name: 'credentials.type.healthInsurance',
  logo: healthInsuranceLogo,
  sampleImage: healthInsuranceSample,
  fields: [
    {
      key: 'class',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'policyNumber',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'endDate',
      type: 'date',
      validations: ['required', 'futureDate']
    }
  ],
  placeholders: {
    class: '{{productClass}}',
    policyNumber: '{{policyNumber}}',
    endDate: '{{expiryDate}}',
    contactName: '{{credentialSubject.name}}'
  }
};

export default { governmentId, educational, proofOfEmployment, healthIsurance };
