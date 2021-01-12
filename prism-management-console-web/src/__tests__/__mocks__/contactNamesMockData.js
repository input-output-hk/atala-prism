export const invalidJsons = [
  {
    input: {},
    expectedOutput: {
      firstName: '',
      midNames: '',
      lastName: '',
      contactName: ''
    }
  },
  {
    input: { contactName: 22 },
    expectedOutput: {
      firstName: '22',
      midNames: '',
      lastName: '',
      contactName: '22'
    }
  },
  {
    input: { midNames: 'Amelia', lastName: 'Collins' },
    expectedOutput: {
      firstName: '',
      midNames: 'Amelia',
      lastName: 'Collins',
      contactName: 'Amelia Collins'
    }
  },
  {
    input: { firstName: 'Rodrigo', contactName: 'Amelia Collins', lastName: 'Collins' },
    expectedOutput: {
      firstName: 'Amelia',
      midNames: '',
      lastName: 'Collins',
      contactName: 'Amelia Collins'
    }
  }
];

export const validJsons = [
  {
    input: { firstName: 'Abbas', midNames: 'Amelia', lastName: 'Collins' },
    expectedOutput: {
      firstName: 'Abbas',
      midNames: 'Amelia',
      lastName: 'Collins',
      contactName: 'Abbas Amelia Collins'
    }
  },
  {
    input: { firstName: 'Willemijn', midNames: 'Dores', lastName: 'Aiolfi' },
    expectedOutput: {
      firstName: 'Willemijn',
      midNames: 'Dores',
      lastName: 'Aiolfi',
      contactName: 'Willemijn Dores Aiolfi'
    }
  },
  {
    input: { firstName: 'Ivan', midNames: 'Tage', lastName: 'Greer' },
    expectedOutput: {
      firstName: 'Ivan',
      midNames: 'Tage',
      lastName: 'Greer',
      contactName: 'Ivan Tage Greer'
    }
  },
  {
    input: { firstName: 'Ki', midNames: 'Mikayla', lastName: 'Bălan' },
    expectedOutput: {
      firstName: 'Ki',
      midNames: 'Mikayla',
      lastName: 'Bălan',
      contactName: 'Ki Mikayla Bălan'
    }
  },
  {
    input: { firstName: 'Oluwasegun', midNames: 'Diarmait', lastName: 'Stenger' },
    expectedOutput: {
      firstName: 'Oluwasegun',
      midNames: 'Diarmait',
      lastName: 'Stenger',
      contactName: 'Oluwasegun Diarmait Stenger'
    }
  },
  {
    input: { firstName: 'Ennio', midNames: 'Dániel', lastName: 'Bailey' },
    expectedOutput: {
      firstName: 'Ennio',
      midNames: 'Dániel',
      lastName: 'Bailey',
      contactName: 'Ennio Dániel Bailey'
    }
  },
  {
    input: { firstName: 'Rodrigo', midNames: 'Jupiter', lastName: 'Dreier' },
    expectedOutput: {
      firstName: 'Rodrigo',
      midNames: 'Jupiter',
      lastName: 'Dreier',
      contactName: 'Rodrigo Jupiter Dreier'
    }
  },
  {
    input: { firstName: 'Uttara', midNames: 'Shokufeh', lastName: 'Steed' },
    expectedOutput: {
      firstName: 'Uttara',
      midNames: 'Shokufeh',
      lastName: 'Steed',
      contactName: 'Uttara Shokufeh Steed'
    }
  },
  {
    input: { firstName: 'Gerard', midNames: 'Plamen', lastName: 'Willoughby' },
    expectedOutput: {
      firstName: 'Gerard',
      midNames: 'Plamen',
      lastName: 'Willoughby',
      contactName: 'Gerard Plamen Willoughby'
    }
  },
  {
    input: { firstName: 'Maia', midNames: 'Clodagh', lastName: 'Dale' },
    expectedOutput: {
      firstName: 'Maia',
      midNames: 'Clodagh',
      lastName: 'Dale',
      contactName: 'Maia Clodagh Dale'
    }
  },
  {
    input: { firstName: 'Rajiv', midNames: 'Bảo Jayanta', lastName: 'Slater' },
    expectedOutput: {
      firstName: 'Rajiv',
      midNames: 'Bảo Jayanta',
      lastName: 'Slater',
      contactName: 'Rajiv Bảo Jayanta Slater'
    }
  },
  {
    input: { firstName: 'Freda', midNames: 'Aelia Wealhmær', lastName: 'Eckstein' },
    expectedOutput: {
      firstName: 'Freda',
      midNames: 'Aelia Wealhmær',
      lastName: 'Eckstein',
      contactName: 'Freda Aelia Wealhmær Eckstein'
    }
  },
  {
    input: { firstName: 'Marika', midNames: 'Allard Dobrilo', lastName: 'Fábián' },
    expectedOutput: {
      firstName: 'Marika',
      midNames: 'Allard Dobrilo',
      lastName: 'Fábián',
      contactName: 'Marika Allard Dobrilo Fábián'
    }
  },
  {
    input: { firstName: 'Leia Okropir', midNames: 'Eckehard', lastName: 'Irvin' },
    expectedOutput: {
      firstName: 'Leia Okropir',
      midNames: 'Eckehard',
      lastName: 'Irvin',
      contactName: 'Leia Okropir Eckehard Irvin'
    }
  },
  {
    input: { firstName: 'Baladeva Grigorios', midNames: '', lastName: 'Mahir Ármannsson' },
    expectedOutput: {
      firstName: 'Baladeva Grigorios',
      midNames: '',
      lastName: 'Mahir Ármannsson',
      contactName: 'Baladeva Grigorios Mahir Ármannsson'
    }
  },
  {
    input: { contactName: 'Abbas Amelia Collins' },
    expectedOutput: {
      firstName: 'Abbas',
      midNames: 'Amelia',
      lastName: 'Collins',
      contactName: 'Abbas Amelia Collins'
    }
  },
  {
    input: { contactName: 'Willemijn Dores Aiolfi' },
    expectedOutput: {
      firstName: 'Willemijn',
      midNames: 'Dores',
      lastName: 'Aiolfi',
      contactName: 'Willemijn Dores Aiolfi'
    }
  },
  {
    input: { contactName: 'Ivan Tage Greer' },
    expectedOutput: {
      firstName: 'Ivan',
      midNames: 'Tage',
      lastName: 'Greer',
      contactName: 'Ivan Tage Greer'
    }
  },
  {
    input: { contactName: 'Ki Mikayla Bălan' },
    expectedOutput: {
      firstName: 'Ki',
      midNames: 'Mikayla',
      lastName: 'Bălan',
      contactName: 'Ki Mikayla Bălan'
    }
  },
  {
    input: { contactName: 'Oluwasegun Diarmait Stenger' },
    expectedOutput: {
      firstName: 'Oluwasegun',
      midNames: 'Diarmait',
      lastName: 'Stenger',
      contactName: 'Oluwasegun Diarmait Stenger'
    }
  },
  {
    input: { contactName: 'Ennio Dániel Bailey' },
    expectedOutput: {
      firstName: 'Ennio',
      midNames: 'Dániel',
      lastName: 'Bailey',
      contactName: 'Ennio Dániel Bailey'
    }
  },
  {
    input: { contactName: 'Rodrigo Jupiter Dreier' },
    expectedOutput: {
      firstName: 'Rodrigo',
      midNames: 'Jupiter',
      lastName: 'Dreier',
      contactName: 'Rodrigo Jupiter Dreier'
    }
  },
  {
    input: { contactName: 'Uttara Shokufeh Steed' },
    expectedOutput: {
      firstName: 'Uttara',
      midNames: 'Shokufeh',
      lastName: 'Steed',
      contactName: 'Uttara Shokufeh Steed'
    }
  },
  {
    input: { contactName: 'Gerard Plamen Willoughby' },
    expectedOutput: {
      firstName: 'Gerard',
      midNames: 'Plamen',
      lastName: 'Willoughby',
      contactName: 'Gerard Plamen Willoughby'
    }
  },
  {
    input: { contactName: 'Maia Clodagh Dale' },
    expectedOutput: {
      firstName: 'Maia',
      midNames: 'Clodagh',
      lastName: 'Dale',
      contactName: 'Maia Clodagh Dale'
    }
  },
  {
    input: { contactName: 'Rajiv Bảo Jayanta Slater' },
    expectedOutput: {
      firstName: 'Rajiv',
      midNames: 'Bảo Jayanta',
      lastName: 'Slater',
      contactName: 'Rajiv Bảo Jayanta Slater'
    }
  },
  {
    input: { contactName: 'Freda Aelia Wealhmær Eckstein' },
    expectedOutput: {
      firstName: 'Freda',
      midNames: 'Aelia Wealhmær',
      lastName: 'Eckstein',
      contactName: 'Freda Aelia Wealhmær Eckstein'
    }
  },
  {
    input: { contactName: 'Marika Allard Dobrilo Fábián' },
    expectedOutput: {
      firstName: 'Marika',
      midNames: 'Allard Dobrilo',
      lastName: 'Fábián',
      contactName: 'Marika Allard Dobrilo Fábián'
    }
  },
  {
    input: { contactName: 'Leia@Okropir Eckehard Irvin' },
    expectedOutput: {
      firstName: 'Leia Okropir',
      midNames: 'Eckehard',
      lastName: 'Irvin',
      contactName: 'Leia Okropir Eckehard Irvin'
    }
  },
  {
    input: { contactName: 'Baladeva@Grigorios Mahir@Ármannsson' },
    expectedOutput: {
      firstName: 'Baladeva Grigorios',
      midNames: '',
      lastName: 'Mahir Ármannsson',
      contactName: 'Baladeva Grigorios Mahir Ármannsson'
    }
  }
];
