import governmentIdLogo from '../../images/government-id-logo.svg';
import educationalLogo from '../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../images/health-insurance-logo.svg';
import governmentIdSample from '../../images/government-id-sample.svg';
import educationalSample from '../../images/educational-credential-sample.svg';
import proofOfEmploymentSample from '../../images/proof-of-employment-sample.svg';
import healthInsuranceSample from '../../images/health-insurance-sample.svg';
import GeorgiaEducationalDegreeLogo from '../../images/GeorgiaEducationalDegreeLogo.png';
import GeorgiaEducationalDegreeTranscriptLogo from '../../images/GeorgiaEducationalDegreeTranscriptLogo.png';
import GeorgiaNationalIDLogo from '../../images/GeorgiaNationalIDLogo.png';
import GeorgiaEducationalDegreeSample from '../../images/GeorgiaEducationalDegreeSample.svg';
import GeorgiaEducationalDegreeTranscriptSample from '../../images/GeorgiaEducationalDegreeTranscriptSample.svg';
import GeorgiaNationalIDSample from '../../images/GeorgiaNationalIDSample.svg';

const governmentId = {
  id: 1,
  enabled: false,
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
  enabled: false,
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
  enabled: false,
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
  enabled: false,
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

const GeorgiaNationalID = {
  id: 5,
  enabled: true,
  name: 'credentials.type.governmentId',
  logo: GeorgiaNationalIDLogo,
  sampleImage: GeorgiaNationalIDSample,
  fields: [
    {
      key: 'fullname',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'gender',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'countryOfCitizenship',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'placeOfBirth',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'dateOfBirth',
      type: 'date',
      validations: ['required', 'pastDate']
    },
    {
      key: 'cardNumber',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'expirationDate',
      type: 'date',
      validations: ['required', 'futureDate']
    },
    {
      key: 'personalNumber',
      type: 'string',
      validations: ['required']
    }
  ],
  placeholders: {
    fullname: '{{credentialSubject.name}}',
    gender: '{{credentialSubject.gender}}',
    countryOfCitizenship: '{{credentialSubject.country}}',
    placeOfBirth: '{{credentialSubject.placeOfBirth}}',
    dateOfBirth: '{{credentialSubject.dateOfBirth}}',
    cardNumber: '{{cardNumber}}',
    expirationDate: '{{expiryDate}}',
    personalNumber: '{{personalNumber}}'
  }
};

const GeorgiaEducationalDegree = {
  id: 6,
  enabled: true,
  name: 'credentials.type.educational',
  logo: GeorgiaEducationalDegreeLogo,
  sampleImage: GeorgiaEducationalDegreeSample,
  fields: [
    {
      key: 'degreeName',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'degreeResult',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'firstName',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'lastName',
      type: 'string',
      validations: ['required']
    }
  ],
  placeholders: {
    degreeName: '{{degreeName}}',
    degreeResult: '{{degreeResult}}',
    firstName: '{{credentialSubject.firstName}}',
    lastName: '{{credentialSubject.lastName}}'
  }
};

const GeorgiaEducationalDegreeTranscript = {
  id: 7,
  enabled: true,
  name: 'credentials.type.transcript',
  logo: GeorgiaEducationalDegreeTranscriptLogo,
  sampleImage: GeorgiaEducationalDegreeTranscriptSample,
  fields: [
    {
      key: 'fullname',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'degreeName',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'cumulativeScore',
      type: 'string',
      validations: ['required']
    },
    {
      key: 'subjects',
      type: 'array',
      validations: ['required']
    }
  ],
  placeholders: {
    fullname: '{{credentialSubject.name}}',
    degreeName: '{{degreeName}}',
    cumulativeScore: '{{cumulativeScore}}',
    subjects: '{{coursesHtml}}'
  }
};

export default {
  governmentId,
  educational,
  proofOfEmployment,
  healthIsurance,
  GeorgiaNationalID,
  GeorgiaEducationalDegree,
  GeorgiaEducationalDegreeTranscript
};
