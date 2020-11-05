import { translateBackSpreadsheetNamesToContactKeys } from '../helpers/contactValidations';
import { arrayOfArraysToObjects } from '../helpers/fileHelpers';
import { validContacts, invalidHeaderNames } from './__mocks__/mockContactsData';
import { mockContactHeadersMapping } from './__mocks__/mockHelpers';

it('parses array of arrays to an object', () => {
  const testInput = validContacts.inputAoA;
  const expectedOutput = validContacts.expectedParse;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockContactHeadersMapping
  );

  expect(translatedBackContacts).toEqual(expectedOutput);
});

it('parses array of arrays with invalid keys to an object', () => {
  /* this tests that if passed a header containing áéíóú like symbols,
  the parsing normalizes it with a plain vowel, to be setted as an object key */
  const testInput = invalidHeaderNames.inputAoA;
  const expectedOutput = invalidHeaderNames.expectedParse;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  expect(parsedAoA).toEqual(expectedOutput);
});
