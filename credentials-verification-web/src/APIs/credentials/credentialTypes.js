import governmentIdLogo from '../../images/government-id-logo.svg';
import educationalLogo from '../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../images/health-insurance-logo.svg';
import governmentIdSample from '../../images/government-id-sample.svg';
import educationalSample from '../../images/educational-credential-sample.svg';
import proofOfEmploymentSample from '../../images/proof-of-employment-sample.svg';
import healthInsuranceSample from '../../images/health-insurance-sample.svg';

const governmentId = {
  name: 'credentials.type.governmentId',
  logo: governmentIdLogo,
  sampleImage: governmentIdSample,
  fields: {
    idNumber: {
      type: 'string',
      validations: ['required']
    },
    fullName: {
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
  }
};

const educational = {
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
  }
};

const proofOfEmployment = {
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
  }
};

const healthIsurance = {
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
  }
};

export default { governmentId, educational, proofOfEmployment, healthIsurance };
