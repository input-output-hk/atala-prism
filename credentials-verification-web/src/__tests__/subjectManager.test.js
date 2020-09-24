import { parseName } from '../APIs/helpers';

it('invalid JSONs do not break parsing', () => {
  const parsedJsons = invalidJsons.map(({ input }) => parseName(input));
  expect(parsedJsons).toEqual(invalidJsons.map(({ expectedOutput }) => expectedOutput));
});

it('parses all valid JSONs', () => {
  const parsedJsons = validJsons.map(({ input }) => parseName(input));
  expect(parsedJsons).toEqual(validJsons.map(({ expectedOutput }) => expectedOutput));
});

const invalidJsons = [
  {
    input: {},
    expectedOutput: {
      firstName: '',
      midNames: '',
      lastName: '',
      fullname: ''
    }
  },
  {
    input: { fullname: 22 },
    expectedOutput: {
      firstName: '22',
      midNames: '',
      lastName: '',
      fullname: '22'
    }
  },
  {
    input: { midNames: 'Amelia', lastName: 'Collins' },
    expectedOutput: {
      firstName: '',
      midNames: 'Amelia',
      lastName: 'Collins',
      fullname: 'Amelia Collins'
    }
  },
  {
    input: { firstName: 'Rodrigo', fullname: 'Amelia Collins', lastName: 'Collins' },
    expectedOutput: {
      firstName: 'Amelia',
      midNames: '',
      lastName: 'Collins',
      fullname: 'Amelia Collins'
    }
  }
];

const validJsons = [
  {
    input: { firstName: 'Abbas', midNames: 'Amelia', lastName: 'Collins' },
    expectedOutput: {
      firstName: 'Abbas',
      midNames: 'Amelia',
      lastName: 'Collins',
      fullname: 'Abbas Amelia Collins'
    }
  },
  {
    input: { firstName: 'Willemijn', midNames: 'Dores', lastName: 'Aiolfi' },
    expectedOutput: {
      firstName: 'Willemijn',
      midNames: 'Dores',
      lastName: 'Aiolfi',
      fullname: 'Willemijn Dores Aiolfi'
    }
  },
  {
    input: { firstName: 'Ivan', midNames: 'Tage', lastName: 'Greer' },
    expectedOutput: {
      firstName: 'Ivan',
      midNames: 'Tage',
      lastName: 'Greer',
      fullname: 'Ivan Tage Greer'
    }
  },
  {
    input: { firstName: 'Ki', midNames: 'Mikayla', lastName: 'Bălan' },
    expectedOutput: {
      firstName: 'Ki',
      midNames: 'Mikayla',
      lastName: 'Bălan',
      fullname: 'Ki Mikayla Bălan'
    }
  },
  {
    input: { firstName: 'Oluwasegun', midNames: 'Diarmait', lastName: 'Stenger' },
    expectedOutput: {
      firstName: 'Oluwasegun',
      midNames: 'Diarmait',
      lastName: 'Stenger',
      fullname: 'Oluwasegun Diarmait Stenger'
    }
  },
  {
    input: { firstName: 'Ennio', midNames: 'Dániel', lastName: 'Bailey' },
    expectedOutput: {
      firstName: 'Ennio',
      midNames: 'Dániel',
      lastName: 'Bailey',
      fullname: 'Ennio Dániel Bailey'
    }
  },
  {
    input: { firstName: 'Rodrigo', midNames: 'Jupiter', lastName: 'Dreier' },
    expectedOutput: {
      firstName: 'Rodrigo',
      midNames: 'Jupiter',
      lastName: 'Dreier',
      fullname: 'Rodrigo Jupiter Dreier'
    }
  },
  {
    input: { firstName: 'Uttara', midNames: 'Shokufeh', lastName: 'Steed' },
    expectedOutput: {
      firstName: 'Uttara',
      midNames: 'Shokufeh',
      lastName: 'Steed',
      fullname: 'Uttara Shokufeh Steed'
    }
  },
  {
    input: { firstName: 'Gerard', midNames: 'Plamen', lastName: 'Willoughby' },
    expectedOutput: {
      firstName: 'Gerard',
      midNames: 'Plamen',
      lastName: 'Willoughby',
      fullname: 'Gerard Plamen Willoughby'
    }
  },
  {
    input: { firstName: 'Maia', midNames: 'Clodagh', lastName: 'Dale' },
    expectedOutput: {
      firstName: 'Maia',
      midNames: 'Clodagh',
      lastName: 'Dale',
      fullname: 'Maia Clodagh Dale'
    }
  },
  {
    input: { firstName: 'Rajiv', midNames: 'Bảo Jayanta', lastName: 'Slater' },
    expectedOutput: {
      firstName: 'Rajiv',
      midNames: 'Bảo Jayanta',
      lastName: 'Slater',
      fullname: 'Rajiv Bảo Jayanta Slater'
    }
  },
  {
    input: { firstName: 'Freda', midNames: 'Aelia Wealhmær', lastName: 'Eckstein' },
    expectedOutput: {
      firstName: 'Freda',
      midNames: 'Aelia Wealhmær',
      lastName: 'Eckstein',
      fullname: 'Freda Aelia Wealhmær Eckstein'
    }
  },
  {
    input: { firstName: 'Marika', midNames: 'Allard Dobrilo', lastName: 'Fábián' },
    expectedOutput: {
      firstName: 'Marika',
      midNames: 'Allard Dobrilo',
      lastName: 'Fábián',
      fullname: 'Marika Allard Dobrilo Fábián'
    }
  },
  {
    input: { firstName: 'Leia Okropir', midNames: 'Eckehard', lastName: 'Irvin' },
    expectedOutput: {
      firstName: 'Leia Okropir',
      midNames: 'Eckehard',
      lastName: 'Irvin',
      fullname: 'Leia Okropir Eckehard Irvin'
    }
  },
  {
    input: { firstName: 'Baladeva Grigorios', midNames: '', lastName: 'Mahir Ármannsson' },
    expectedOutput: {
      firstName: 'Baladeva Grigorios',
      midNames: '',
      lastName: 'Mahir Ármannsson',
      fullname: 'Baladeva Grigorios Mahir Ármannsson'
    }
  },
  {
    input: { fullname: 'Abbas Amelia Collins' },
    expectedOutput: {
      firstName: 'Abbas',
      midNames: 'Amelia',
      lastName: 'Collins',
      fullname: 'Abbas Amelia Collins'
    }
  },
  {
    input: { fullname: 'Willemijn Dores Aiolfi' },
    expectedOutput: {
      firstName: 'Willemijn',
      midNames: 'Dores',
      lastName: 'Aiolfi',
      fullname: 'Willemijn Dores Aiolfi'
    }
  },
  {
    input: { fullname: 'Ivan Tage Greer' },
    expectedOutput: {
      firstName: 'Ivan',
      midNames: 'Tage',
      lastName: 'Greer',
      fullname: 'Ivan Tage Greer'
    }
  },
  {
    input: { fullname: 'Ki Mikayla Bălan' },
    expectedOutput: {
      firstName: 'Ki',
      midNames: 'Mikayla',
      lastName: 'Bălan',
      fullname: 'Ki Mikayla Bălan'
    }
  },
  {
    input: { fullname: 'Oluwasegun Diarmait Stenger' },
    expectedOutput: {
      firstName: 'Oluwasegun',
      midNames: 'Diarmait',
      lastName: 'Stenger',
      fullname: 'Oluwasegun Diarmait Stenger'
    }
  },
  {
    input: { fullname: 'Ennio Dániel Bailey' },
    expectedOutput: {
      firstName: 'Ennio',
      midNames: 'Dániel',
      lastName: 'Bailey',
      fullname: 'Ennio Dániel Bailey'
    }
  },
  {
    input: { fullname: 'Rodrigo Jupiter Dreier' },
    expectedOutput: {
      firstName: 'Rodrigo',
      midNames: 'Jupiter',
      lastName: 'Dreier',
      fullname: 'Rodrigo Jupiter Dreier'
    }
  },
  {
    input: { fullname: 'Uttara Shokufeh Steed' },
    expectedOutput: {
      firstName: 'Uttara',
      midNames: 'Shokufeh',
      lastName: 'Steed',
      fullname: 'Uttara Shokufeh Steed'
    }
  },
  {
    input: { fullname: 'Gerard Plamen Willoughby' },
    expectedOutput: {
      firstName: 'Gerard',
      midNames: 'Plamen',
      lastName: 'Willoughby',
      fullname: 'Gerard Plamen Willoughby'
    }
  },
  {
    input: { fullname: 'Maia Clodagh Dale' },
    expectedOutput: {
      firstName: 'Maia',
      midNames: 'Clodagh',
      lastName: 'Dale',
      fullname: 'Maia Clodagh Dale'
    }
  },
  {
    input: { fullname: 'Rajiv Bảo Jayanta Slater' },
    expectedOutput: {
      firstName: 'Rajiv',
      midNames: 'Bảo Jayanta',
      lastName: 'Slater',
      fullname: 'Rajiv Bảo Jayanta Slater'
    }
  },
  {
    input: { fullname: 'Freda Aelia Wealhmær Eckstein' },
    expectedOutput: {
      firstName: 'Freda',
      midNames: 'Aelia Wealhmær',
      lastName: 'Eckstein',
      fullname: 'Freda Aelia Wealhmær Eckstein'
    }
  },
  {
    input: { fullname: 'Marika Allard Dobrilo Fábián' },
    expectedOutput: {
      firstName: 'Marika',
      midNames: 'Allard Dobrilo',
      lastName: 'Fábián',
      fullname: 'Marika Allard Dobrilo Fábián'
    }
  },
  {
    input: { fullname: 'Leia@Okropir Eckehard Irvin' },
    expectedOutput: {
      firstName: 'Leia Okropir',
      midNames: 'Eckehard',
      lastName: 'Irvin',
      fullname: 'Leia Okropir Eckehard Irvin'
    }
  },
  {
    input: { fullname: 'Baladeva@Grigorios Mahir@Ármannsson' },
    expectedOutput: {
      firstName: 'Baladeva Grigorios',
      midNames: '',
      lastName: 'Mahir Ármannsson',
      fullname: 'Baladeva Grigorios Mahir Ármannsson'
    }
  }
];
