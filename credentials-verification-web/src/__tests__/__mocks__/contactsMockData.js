export const validContacts = {
  inputAoA: [
    ['firstName', 'lastName', 'externalId'],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1'],
    ['Lyla', 'Dodd', 'd8619d78-e516-433a-ad45-24c91e39c7c6'],
    ['Roger', 'Knights', '6b59c558-79f3-455d-9df0-0a4b641e519d'],
    ['Israel', 'Hatfield', '434b5e14-a2a3-425f-89a9-ae036d5b7df8'],
    ['Corban', 'Bateman', 'b9692696-d19f-4fce-84c1-9f3465ea6bec'],
    ['Montgomery', 'Britt', 'e32703db-2196-4623-b4ec-3149e91c015c'],
    ['Fateh', 'Daugherty', '94e3903c-2665-4c87-96f9-f2d0f53175e7'],
    ['Connar', 'Snow', '49fd1b95-d997-412e-9770-87e194fe4b28'],
    ['Zaynah', 'Mccoy', '7c45b6f0-643d-4b3b-8a56-93b7e926923f'],
    ['Kaci', 'Middleton', 'd8bf8777-b24a-4be1-80b6-d9e5aff48fbc'],
    ['Meredith', 'Burch', '35e0c0f6-613a-499d-8159-aa8a4b7a23d5'],
    ['Emmeline', 'Cisneros', 'e55ca046-06c4-4e06-98b6-964dcc74529b'],
    ['Rueben', 'Cross', '9dae3176-91ae-4ed0-bf7e-889d753785c0'],
    ['Cleveland', 'Mccarthy', '795fa27c-9cee-46f4-a693-6c74e77844c2'],
    ['Gracie-Mae', 'Knight', '8cb7440d-df29-4a95-b9a7-26d26f01a96e']
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1']
    },
    {
      firstName: 'Lyla',
      lastName: 'Dodd',
      externalId: 'd8619d78-e516-433a-ad45-24c91e39c7c6',
      originalArray: ['Lyla', 'Dodd', 'd8619d78-e516-433a-ad45-24c91e39c7c6']
    },
    {
      firstName: 'Roger',
      lastName: 'Knights',
      externalId: '6b59c558-79f3-455d-9df0-0a4b641e519d',
      originalArray: ['Roger', 'Knights', '6b59c558-79f3-455d-9df0-0a4b641e519d']
    },
    {
      firstName: 'Israel',
      lastName: 'Hatfield',
      externalId: '434b5e14-a2a3-425f-89a9-ae036d5b7df8',
      originalArray: ['Israel', 'Hatfield', '434b5e14-a2a3-425f-89a9-ae036d5b7df8']
    },
    {
      firstName: 'Corban',
      lastName: 'Bateman',
      externalId: 'b9692696-d19f-4fce-84c1-9f3465ea6bec',
      originalArray: ['Corban', 'Bateman', 'b9692696-d19f-4fce-84c1-9f3465ea6bec']
    },
    {
      firstName: 'Montgomery',
      lastName: 'Britt',
      externalId: 'e32703db-2196-4623-b4ec-3149e91c015c',
      originalArray: ['Montgomery', 'Britt', 'e32703db-2196-4623-b4ec-3149e91c015c']
    },
    {
      firstName: 'Fateh',
      lastName: 'Daugherty',
      externalId: '94e3903c-2665-4c87-96f9-f2d0f53175e7',
      originalArray: ['Fateh', 'Daugherty', '94e3903c-2665-4c87-96f9-f2d0f53175e7']
    },
    {
      firstName: 'Connar',
      lastName: 'Snow',
      externalId: '49fd1b95-d997-412e-9770-87e194fe4b28',
      originalArray: ['Connar', 'Snow', '49fd1b95-d997-412e-9770-87e194fe4b28']
    },
    {
      firstName: 'Zaynah',
      lastName: 'Mccoy',
      externalId: '7c45b6f0-643d-4b3b-8a56-93b7e926923f',
      originalArray: ['Zaynah', 'Mccoy', '7c45b6f0-643d-4b3b-8a56-93b7e926923f']
    },
    {
      firstName: 'Kaci',
      lastName: 'Middleton',
      externalId: 'd8bf8777-b24a-4be1-80b6-d9e5aff48fbc',
      originalArray: ['Kaci', 'Middleton', 'd8bf8777-b24a-4be1-80b6-d9e5aff48fbc']
    },
    {
      firstName: 'Meredith',
      lastName: 'Burch',
      externalId: '35e0c0f6-613a-499d-8159-aa8a4b7a23d5',
      originalArray: ['Meredith', 'Burch', '35e0c0f6-613a-499d-8159-aa8a4b7a23d5']
    },
    {
      firstName: 'Emmeline',
      lastName: 'Cisneros',
      externalId: 'e55ca046-06c4-4e06-98b6-964dcc74529b',
      originalArray: ['Emmeline', 'Cisneros', 'e55ca046-06c4-4e06-98b6-964dcc74529b']
    },
    {
      firstName: 'Rueben',
      lastName: 'Cross',
      externalId: '9dae3176-91ae-4ed0-bf7e-889d753785c0',
      originalArray: ['Rueben', 'Cross', '9dae3176-91ae-4ed0-bf7e-889d753785c0']
    },
    {
      firstName: 'Cleveland',
      lastName: 'Mccarthy',
      externalId: '795fa27c-9cee-46f4-a693-6c74e77844c2',
      originalArray: ['Cleveland', 'Mccarthy', '795fa27c-9cee-46f4-a693-6c74e77844c2']
    },
    {
      firstName: 'Gracie-Mae',
      lastName: 'Knight',
      externalId: '8cb7440d-df29-4a95-b9a7-26d26f01a96e',
      originalArray: ['Gracie-Mae', 'Knight', '8cb7440d-df29-4a95-b9a7-26d26f01a96e']
    }
  ]
};

export const invalidHeaderNames = {
  inputAoA: [
    ['fírstNámé', 'lástNámé', 'ëxtèrnàlÏd'],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1']
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1']
    }
  ]
};

export const extraHeaders = {
  inputAoA: [
    ['firstName', 'lastName', 'externalId', 'extraHeader1', '', 'extraHeader3'],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', '', '', '']
  ],
  expectedErrors: [
    [
      { error: 'excessHeader', row: { index: -1 }, col: { index: 3, name: 'extraHeader1' } },
      { error: 'excessHeader', row: { index: -1 }, col: { index: 4, name: '' } },
      { error: 'excessHeader', row: { index: -1 }, col: { index: 5, name: 'extraHeader3' } }
    ]
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      extraHeader1: '',
      '': '',
      extraHeader3: '',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', '', '', '']
    }
  ]
};

export const invalidHeadersOrder = {
  inputAoA: [
    ['firstName', 'extraHeader1', 'lastName', '', 'externalId', 'extraHeader3'],
    ['Astrid', '', 'Bernal', '', '454cda5e-16d5-4b86-b903-535125f78fe1', '']
  ],
  expectedErrors: [
    [
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 1, name: 'extraHeader1' }
      },
      {
        error: 'invalidHeaderPosition',
        row: { index: -1 },
        col: { index: 2, expectedIndex: 1, name: 'lastName' }
      },
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 3, name: '' }
      },
      {
        error: 'invalidHeaderPosition',
        row: { index: -1 },
        col: { index: 4, expectedIndex: 2, name: 'externalId' }
      },
      {
        error: 'excessHeader',
        row: { index: -1 },
        col: { index: 5, name: 'extraHeader3' }
      }
    ]
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      extraHeader1: '',
      lastName: 'Bernal',
      '': '',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      extraHeader3: '',
      originalArray: ['Astrid', '', 'Bernal', '', '454cda5e-16d5-4b86-b903-535125f78fe1', '']
    }
  ]
};

export const invalidContactData = {
  inputAoA: [
    ['firstName', 'lastName', 'externalId', ''],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1'],
    ['Lyla', 'Dodd'],
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
        col: { index: 3, content: 'extra field 1' }
      }
    ],
    [
      {
        error: 'required',
        row: { index: 1 },
        col: { index: 2, name: 'externalId' }
      }
    ]
  ],
  expectedParse: [
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      '': 'extra field 1',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1']
    },
    {
      firstName: 'Lyla',
      lastName: 'Dodd',
      originalArray: ['Lyla', 'Dodd']
    },
    {
      externalId: '',
      firstName: '',
      lastName: '',
      originalArray: ['', '', '']
    },
    {
      originalArray: []
    },
    {
      originalArray: []
    }
  ]
};

export const emptyRows = {
  inputAoA: [
    ['firstName', 'lastName', 'externalId', ''],
    ['', '', ''],
    ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1'],
    [],
    ['Lyla', 'Dodd'],
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
        col: { index: 3, content: 'extra field 1' }
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
        col: { index: 2, name: 'externalId' }
      }
    ]
  ],
  expectedParse: [
    {
      firstName: '',
      lastName: '',
      externalId: '',
      originalArray: ['', '', '']
    },
    {
      firstName: 'Astrid',
      lastName: 'Bernal',
      externalId: '454cda5e-16d5-4b86-b903-535125f78fe1',
      '': 'extra field 1',
      originalArray: ['Astrid', 'Bernal', '454cda5e-16d5-4b86-b903-535125f78fe1', 'extra field 1']
    },
    {
      originalArray: []
    },
    {
      firstName: 'Lyla',
      lastName: 'Dodd',
      originalArray: ['Lyla', 'Dodd']
    },
    {
      originalArray: []
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
