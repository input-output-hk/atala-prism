export const mockGovernmentId = {
  name: 'Government Id',
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
  ]
};

export const mockEducational = {
  name: 'Educational',
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
  ]
};

export const mockProofOfEmployment = {
  name: 'Proof Of Employment',
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
  ]
};

export const mockHealthIsurance = {
  name: 'Health Insurance',
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
  ]
};
