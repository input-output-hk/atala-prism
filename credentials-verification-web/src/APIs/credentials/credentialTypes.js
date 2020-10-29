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
  fields: {
    idNumber: {
      type: 'string',
      validations: ['required']
    },
    fullname: {
      type: 'string',
      validations: ['required']
    },
    dateOfBirth: {
      type: 'date',
      validations: ['required', 'pastDate']
    },
    expirationDate: {
      type: 'date',
      validations: ['required', 'futureDate']
    },
    photo: {
      type: 'file',
      validations: ['required']
    }
  },
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
  fields: {
    degree: {
      type: 'string',
      validations: ['required']
    },
    award: {
      type: 'string',
      validations: ['required']
    },
    studentId: {
      type: 'string',
      validations: ['required']
    },
    startDate: {
      type: 'date',
      validations: ['required', 'pastDate']
    },
    graduationDate: {
      type: 'date',
      validations: ['required', 'pastDate']
    }
  },
  placeholders: {
    degree: '{{credentialSubject.degreeAwarded}}',
    award: '{{credentialSubject.degreeResult}}',
    startDate: '{{credentialSubject.startDate}}',
    graduationDate: '{{issuanceDate}}',
    fullname: '{{credentialSubject.name}}'
  }
};

const proofOfEmployment = {
  id: 3,
  name: 'credentials.type.proofOfEmployment',
  logo: proofOfEmploymentLogo,
  sampleImage: proofOfEmploymentSample,
  fields: {
    adress: {
      type: 'string',
      validations: ['required']
    },
    status: {
      type: 'string',
      validations: ['required']
    },
    startDate: {
      type: 'date',
      validations: ['required', 'pastDate']
    }
  },
  placeholders: {
    adress: '{{issuer.address}}',
    status: '{{employmentStatus}}',
    startDate: '{{employmentStartDate}}',
    fullname: '{{credentialSubject.name}}'
  }
};

const healthIsurance = {
  id: 4,
  name: 'credentials.type.healthInsurance',
  logo: healthInsuranceLogo,
  sampleImage: healthInsuranceSample,
  fields: {
    class: {
      type: 'string',
      validations: ['required']
    },
    policyNumber: {
      type: 'string',
      validations: ['required']
    },
    endDate: {
      type: 'date',
      validations: ['required', 'futureDate']
    }
  },
  placeholders: {
    class: '{{productClass}}',
    policyNumber: '{{policyNumber}}',
    endDate: '{{expiryDate}}',
    fullname: '{{credentialSubject.name}}'
  }
};

export default { governmentId, educational, proofOfEmployment, healthIsurance };
