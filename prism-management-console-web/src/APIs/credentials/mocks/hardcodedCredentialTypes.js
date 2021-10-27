import governmentIdLogo from '../../../images/government-id-logo.svg';
import educationalLogo from '../../../images/educational-credential-logo.svg';
import proofOfEmploymentLogo from '../../../images/proof-of-employment-logo.svg';
import healthInsuranceLogo from '../../../images/health-insurance-logo.svg';
import governmentIdSample from '../../../images/government-id-sample.svg';
import educationalSample from '../../../images/educational-credential-sample.svg';
import proofOfEmploymentSample from '../../../images/proof-of-employment-sample.svg';
import healthInsuranceSample from '../../../images/health-insurance-sample.svg';
import georgiaEducationalDegreeLogo from '../../../images/GeorgiaEducationalDegreeLogo.png';
import georgiaEducationalDegreeTranscriptLogo from '../../../images/GeorgiaEducationalDegreeTranscriptLogo.png';
import georgiaNationalIDLogo from '../../../images/GeorgiaNationalIDLogo.png';
import georgiaEducationalDegreeSample from '../../../images/GeorgiaEducationalDegreeSample.svg';
import georgiaEducationalDegreeTranscriptSample from '../../../images/GeorgiaEducationalDegreeTranscriptSample.svg';
import georgiaNationalIDSample from '../../../images/GeorgiaNationalIDSample.svg';
import { georgiaCoursesView, ethiopiaCoursesView } from '../credentialExtraViews';

const governmentId = {
  id: 1,
  enabled: false,
  isMultiRow: false,
  name: 'Government ID',
  icon: governmentIdLogo,
  sampleImage: governmentIdSample,
  category: '1',
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
  isMultiRow: false,
  name: 'Educational',
  icon: educationalLogo,
  sampleImage: educationalSample,
  category: '2',
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
  isMultiRow: false,
  name: 'Proof Of Employment',
  icon: proofOfEmploymentLogo,
  sampleImage: proofOfEmploymentSample,
  category: '3',
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
  isMultiRow: false,
  name: 'Health Insurance',
  icon: healthInsuranceLogo,
  sampleImage: healthInsuranceSample,
  category: '4',
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
  enabled: false,
  isMultiRow: false,
  name: 'credentials.type.governmentId',
  icon: georgiaNationalIDLogo,
  sampleImage: georgiaNationalIDSample,
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
  enabled: false,
  isMultiRow: false,
  name: 'credentials.type.educational',
  icon: georgiaEducationalDegreeLogo,
  sampleImage: georgiaEducationalDegreeSample,
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
  enabled: false,
  isMultiRow: true,
  multiRowKey: 'courses',
  multiRowView: georgiaCoursesView,
  name: 'credentials.type.transcript',
  icon: georgiaEducationalDegreeTranscriptLogo,
  sampleImage: georgiaEducationalDegreeTranscriptSample,
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
      key: 'courseName',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'courseCode',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'credits',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'score',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'grade',
      type: 'string',
      validations: ['required'],
      isRowField: true
    }
  ],
  placeholders: {
    fullname: '{{credentialSubject.name}}',
    degreeName: '{{degreeName}}',
    cumulativeScore: '{{cumulativeScore}}'
  }
};

const EthiopiaNationalID = {
  id: 8,
  enabled: true,
  isMultiRow: false,
  name: 'credentials.type.governmentId',
  icon: georgiaNationalIDLogo,
  sampleImage: georgiaNationalIDSample,
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

const EthiopiaEducationalDegree = {
  id: 9,
  enabled: true,
  isMultiRow: false,
  name: 'credentials.type.educational',
  icon: georgiaEducationalDegreeLogo,
  sampleImage: georgiaEducationalDegreeSample,
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

const EthiopiaEducationalDegreeTranscript = {
  id: 10,
  enabled: true,
  isMultiRow: true,
  multiRowKey: 'courses',
  multiRowView: ethiopiaCoursesView,
  name: 'credentials.type.transcript',
  icon: georgiaEducationalDegreeTranscriptLogo,
  sampleImage: georgiaEducationalDegreeTranscriptSample,
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
      key: 'courseName',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'courseCode',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'credits',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'score',
      type: 'string',
      validations: ['required'],
      isRowField: true
    },
    {
      key: 'grade',
      type: 'string',
      validations: ['required'],
      isRowField: true
    }
  ],
  placeholders: {
    fullname: '{{credentialSubject.name}}',
    degreeName: '{{degreeName}}',
    cumulativeScore: '{{cumulativeScore}}'
  }
};

export default {
  governmentId,
  educational,
  proofOfEmployment,
  healthIsurance,
  GeorgiaNationalID,
  GeorgiaEducationalDegree,
  GeorgiaEducationalDegreeTranscript,
  EthiopiaNationalID,
  EthiopiaEducationalDegree,
  EthiopiaEducationalDegreeTranscript
};
