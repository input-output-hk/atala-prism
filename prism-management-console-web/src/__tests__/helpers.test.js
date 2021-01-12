import { translateBackSpreadsheetNamesToContactKeys } from '../helpers/contactValidations';
import { arrayOfArraysToObjects } from '../helpers/fileHelpers';
import {
  validContacts,
  invalidHeaderNames,
  contactListToFilter
} from './__mocks__/mockContactsData';
import { mockContactHeadersMapping } from './__mocks__/mockHelpers';
import { filterByManyFields } from '../helpers/filterHelpers';
import { CONTACT_NAME_KEY, EXTERNAL_ID_KEY } from '../helpers/constants';

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

it('filter a list based on a single text and given list fields', () => {
  const contacts = contactListToFilter.list;

  const { externalid, contactName } = contactListToFilter.input;
  const filteredByName = filterByManyFields(contacts, contactName, [
    CONTACT_NAME_KEY,
    EXTERNAL_ID_KEY
  ]);
  const filteredByExternalId = filterByManyFields(contacts, externalid, [
    CONTACT_NAME_KEY,
    EXTERNAL_ID_KEY
  ]);

  expect(filteredByName).toEqual(filteredByExternalId);
});
