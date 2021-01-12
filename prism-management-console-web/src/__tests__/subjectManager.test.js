import { parseName } from '../APIs/helpers/contactHelpers';
import { invalidJsons, validJsons } from './__mocks__/contactNamesMockData';

it('invalid JSONs do not break parsing', () => {
  const parsedJsons = invalidJsons.map(({ input }) => parseName(input));
  expect(parsedJsons).toEqual(invalidJsons.map(({ expectedOutput }) => expectedOutput));
});

it('parses all valid JSONs', () => {
  const parsedJsons = validJsons.map(({ input }) => parseName(input));
  expect(parsedJsons).toEqual(validJsons.map(({ expectedOutput }) => expectedOutput));
});
