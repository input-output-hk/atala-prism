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
import { isValueInListByKey, isValueUniqueInObjectListByKey } from '../helpers/formRules';

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

describe('isValueInListByKey', () => {
  const contacts = contactListToFilter.list;
  const { externalid, contactName } = contactListToFilter.input;

  it('should return true when value exists', () => {
    const isExternalidExists = isValueInListByKey(contacts, externalid, 'externalid');
    expect(isExternalidExists).toEqual(true);
  });

  it('should return false when value not exists', () => {
    const filteredContacts = contacts.filter(item => item.externalid !== externalid);
    const isExternalidExists = isValueInListByKey(filteredContacts, externalid, 'externalid');
    expect(isExternalidExists).toEqual(false);
  });

  it('should return false when the list is empty', () => {
    const withEmptyList = isValueInListByKey([], contactName, 'contactName');
    expect(withEmptyList).toEqual(false);
  });

  it('should return false when value is null and the list is empty', () => {
    const withContactNameNullAndEmptyList = isValueInListByKey([], null, 'contactName');
    expect(withContactNameNullAndEmptyList).toEqual(false);
  });

  it('should return false when value is undefined and the list is empty', () => {
    const withContactNameNullAndEmptyList = isValueInListByKey([], undefined, 'contactName');
    expect(withContactNameNullAndEmptyList).toEqual(false);
  });

  it('should return false when the field key is undefined', () => {
    const withUndefinedKey = isValueInListByKey(contacts, externalid, undefined);
    expect(withUndefinedKey).toEqual(false);
  });

  it('should return false when the value and list is null', () => {
    const withNullValueAndList = isValueInListByKey(null, null, 'contactName');
    expect(withNullValueAndList).toEqual(false);
  });
});

describe('isValueUniqueInObjectListByKey', () => {
  const { externalid, contactName } = contactListToFilter.input;

  it('should return true when value is unique', () => {
    const contacts = contactListToFilter.list;
    const isExternalidExists = isValueUniqueInObjectListByKey(contacts, externalid, 'externalid');
    expect(isExternalidExists).toEqual(true);
  });

  it('should return false when value is not unique', () => {
    const contacts = contactListToFilter.list;
    contacts.push(contactListToFilter.input);
    const isExternalidExists = isValueUniqueInObjectListByKey(contacts, externalid, 'externalid');
    expect(isExternalidExists).toEqual(false);
  });

  it('should return true when the list is empty', () => {
    const withEmptyList = isValueUniqueInObjectListByKey([], contactName, 'contactName');
    expect(withEmptyList).toEqual(true);
  });

  it('should return true when value is null and the list is empty', () => {
    const withContactNameNullAndEmptyList = isValueUniqueInObjectListByKey([], null, 'contactName');
    expect(withContactNameNullAndEmptyList).toEqual(true);
  });

  it('should return true when value is undefined and the list is empty', () => {
    const withContactNameNullAndEmptyList = isValueUniqueInObjectListByKey(
      [],
      undefined,
      'contactName'
    );
    expect(withContactNameNullAndEmptyList).toEqual(true);
  });

  it('should return true when the field key is undefined', () => {
    const contacts = contactListToFilter.list;
    const withUndefinedKey = isValueUniqueInObjectListByKey(contacts, externalid, undefined);
    expect(withUndefinedKey).toEqual(true);
  });

  it('should return true when the value and list is null', () => {
    const withNullValueAndList = isValueUniqueInObjectListByKey(null, null, 'contactName');
    expect(withNullValueAndList).toEqual(true);
  });
});
