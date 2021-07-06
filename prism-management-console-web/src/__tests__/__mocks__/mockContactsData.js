export const validContacts = {
  inputAoA: [
    ['Contact Name', 'External ID'],
    ['Astrid Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1'],
    ['Lyla Dodd', 'd8619d78-e516-433a-ad45-24c91e39c7c6'],
    ['Roger Knights', '6b59c558-79f3-455d-9df0-0a4b641e519d'],
    ['Israel Hatfield', '434b5e14-a2a3-425f-89a9-ae036d5b7df8'],
    ['Corban Bateman', 'b9692696-d19f-4fce-84c1-9f3465ea6bec'],
    ['Montgomery Britt', 'e32703db-2196-4623-b4ec-3149e91c015c'],
    ['Fateh Daugherty', '94e3903c-2665-4c87-96f9-f2d0f53175e7'],
    ['Connar Snow', '49fd1b95-d997-412e-9770-87e194fe4b28'],
    ['Zaynah Mccoy', '7c45b6f0-643d-4b3b-8a56-93b7e926923f'],
    ['Kaci Middleton', 'd8bf8777-b24a-4be1-80b6-d9e5aff48fbc'],
    ['Meredith Burch', '35e0c0f6-613a-499d-8159-aa8a4b7a23d5'],
    ['Emmeline Cisneros', 'e55ca046-06c4-4e06-98b6-964dcc74529b'],
    ['Rueben Cross', '9dae3176-91ae-4ed0-bf7e-889d753785c0'],
    ['Cleveland Mccarthy', '795fa27c-9cee-46f4-a693-6c74e77844c2'],
    ['Gracie-Mae Knight', '8cb7440d-df29-4a95-b9a7-26d26f01a96e']
  ],
  expectedParse: [
    {
      contactName: 'Astrid Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1'
    },
    {
      contactName: 'Lyla Dodd',
      externalId: 'd8619d78-e516-433a-ad45-24c91e39c7c6'
    },
    {
      contactName: 'Roger Knights',
      externalId: '6b59c558-79f3-455d-9df0-0a4b641e519d'
    },
    {
      contactName: 'Israel Hatfield',
      externalId: '434b5e14-a2a3-425f-89a9-ae036d5b7df8'
    },
    {
      contactName: 'Corban Bateman',
      externalId: 'b9692696-d19f-4fce-84c1-9f3465ea6bec'
    },
    {
      contactName: 'Montgomery Britt',
      externalId: 'e32703db-2196-4623-b4ec-3149e91c015c'
    },
    {
      contactName: 'Fateh Daugherty',
      externalId: '94e3903c-2665-4c87-96f9-f2d0f53175e7'
    },
    {
      contactName: 'Connar Snow',
      externalId: '49fd1b95-d997-412e-9770-87e194fe4b28'
    },
    {
      contactName: 'Zaynah Mccoy',
      externalId: '7c45b6f0-643d-4b3b-8a56-93b7e926923f'
    },
    {
      contactName: 'Kaci Middleton',
      externalId: 'd8bf8777-b24a-4be1-80b6-d9e5aff48fbc'
    },
    {
      contactName: 'Meredith Burch',
      externalId: '35e0c0f6-613a-499d-8159-aa8a4b7a23d5'
    },
    {
      contactName: 'Emmeline Cisneros',
      externalId: 'e55ca046-06c4-4e06-98b6-964dcc74529b'
    },
    {
      contactName: 'Rueben Cross',
      externalId: '9dae3176-91ae-4ed0-bf7e-889d753785c0'
    },
    {
      contactName: 'Cleveland Mccarthy',
      externalId: '795fa27c-9cee-46f4-a693-6c74e77844c2'
    },
    {
      contactName: 'Gracie-Mae Knight',
      externalId: '8cb7440d-df29-4a95-b9a7-26d26f01a96e'
    }
  ]
};

export const invalidHeaderNames = {
  inputAoA: [
    ['fírstNámé', 'lástNámé', 'ËXTèrnàlÏD'],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1']
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      EXTernalID: '454cda5e-16d5-4b86-b903-535125f78fe1',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1']
    }
  ]
};

export const extraHeaders = {
  inputAoA: [
    ['Contact Name', 'External ID', 'extraHeader1', '', 'extraHeader3'],
    ['Astrid Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', '', '', '']
  ],
  expectedErrors: [
    [
      { error: 'excessHeader', row: { index: -1 }, col: { index: 2, name: 'extraHeader1' } },
      { error: 'excessHeader', row: { index: -1 }, col: { index: 3, name: '' } },
      { error: 'excessHeader', row: { index: -1 }, col: { index: 4, name: 'extraHeader3' } }
    ]
  ],
  expectedParse: [
    {
      contactName: 'Astrid Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1'
    }
  ]
};

export const invalidHeadersOrder = {
  inputAoA: [
    ['Contact Name', 'extraHeader1', '', 'External ID', 'extraHeader3'],
    ['Astrid Bernal', '', '', '454cda5e-16d5-4b86-b903-535125f78fe1', '']
  ],
  expectedErrors: [
    [
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 1, name: 'extraHeader1' }
      },
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 2, name: '' }
      },
      {
        error: 'invalidHeaderPosition',
        row: { index: -1 },
        col: { index: 3, expectedIndex: 1, name: 'External ID' }
      },
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 4, name: 'extraHeader3' }
      }
    ]
  ],
  expectedParse: [
    {
      contactName: 'Astrid Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1'
    }
  ]
};

export const invalidContactData = {
  inputAoA: [
    ['Contact Name', 'External ID', ''],
    ['Astrid Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1'],
    ['Lyla Dodd'],
    ['', '', ''],
    [],
    []
  ],
  expectedErrors: [
    [],
    [
      {
        error: 'extraField',
        row: { index: 0 },
        col: { index: 2, content: 'extra field 1' }
      }
    ],
    [
      {
        error: 'required',
        row: { index: 1 },
        col: { index: 1, name: 'External ID' }
      }
    ]
  ],
  expectedParse: [
    {
      contactName: 'Astrid Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1'
    },
    {
      contactName: 'Lyla Dodd',
      externalId: undefined
    },
    {
      contactName: '',
      externalId: ''
    },
    {
      contactName: undefined,
      externalId: undefined
    },
    {
      contactName: undefined,
      externalId: undefined
    }
  ]
};

export const emptyRows = {
  inputAoA: [
    ['Contact Name', 'External ID', ''],
    ['', '', ''],
    ['Astrid Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1'],
    [],
    ['Lyla Dodd'],
    []
  ],
  expectedErrors: [
    [],
    [
      {
        error: 'emptyRow',
        row: { index: 0 },
        col: { index: 0 }
      }
    ],
    [
      {
        error: 'extraField',
        row: { index: 1 },
        col: { index: 2, content: 'extra field 1' }
      }
    ],
    [
      {
        error: 'emptyRow',
        row: { index: 2 },
        col: { index: 0 }
      }
    ],
    [
      {
        error: 'required',
        row: { index: 3 },
        col: { index: 1, name: 'External ID' }
      }
    ]
  ],
  expectedParse: [
    {
      contactName: '',
      externalId: ''
    },
    {
      contactName: 'Astrid Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1'
    },
    {
      contactName: undefined,
      externalId: undefined
    },
    {
      contactName: 'Lyla Dodd',
      externalId: undefined
    },
    {
      contactName: undefined,
      externalId: undefined
    }
  ]
};

export const emptyData = {
  inputAoA: [['firstName', 'lastName', 'externalId']],
  expectedErrors: [
    [
      {
        error: 'emptyFile',
        row: { index: 0 },
        col: { index: 0 }
      }
    ]
  ],
  expectedParse: []
};

export const emptyFile = {
  inputAoA: [[]],
  expectedErrors: [
    [
      {
        error: 'emptyFile',
        row: { index: 0 },
        col: { index: 0 }
      }
    ]
  ],
  expectedParse: []
};

export const contactListToFilter = {
  input: {
    contactName: 'Lyla Dodd',
    externalId: 'd8619d78-e516-433a-ad45-24c91e39c7c6'
  },
  list: validContacts.expectedParse
};
