import fs from 'fs';
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
import { blobToBase64 } from '../helpers/genericHelpers';

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

  const { externalId, contactName } = contactListToFilter.input;
  const filteredByName = filterByManyFields(contacts, contactName, [
    CONTACT_NAME_KEY,
    EXTERNAL_ID_KEY
  ]);
  const filteredByExternalId = filterByManyFields(contacts, externalId, [
    CONTACT_NAME_KEY,
    EXTERNAL_ID_KEY
  ]);

  expect(filteredByName).toEqual(filteredByExternalId);
});

describe('isValueInListByKey', () => {
  const contacts = contactListToFilter.list;
  const { externalId, contactName } = contactListToFilter.input;

  it('should return true when value exists', () => {
    const isExternalidExists = isValueInListByKey(contacts, externalId, 'externalId');
    expect(isExternalidExists).toEqual(true);
  });

  it('should return false when value not exists', () => {
    const filteredContacts = contacts.filter(item => item.externalId !== externalId);
    const isExternalidExists = isValueInListByKey(filteredContacts, externalId, 'externalId');
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
    const withUndefinedKey = isValueInListByKey(contacts, externalId, undefined);
    expect(withUndefinedKey).toEqual(false);
  });

  it('should return false when the value and list is null', () => {
    const withNullValueAndList = isValueInListByKey(null, null, 'contactName');
    expect(withNullValueAndList).toEqual(false);
  });
});

describe('isValueUniqueInObjectListByKey', () => {
  const { externalId, contactName } = contactListToFilter.input;

  it('should return true when value is unique', () => {
    const contacts = contactListToFilter.list;
    const isExternalidExists = isValueUniqueInObjectListByKey(contacts, externalId, 'externalId');
    expect(isExternalidExists).toEqual(true);
  });

  it('should return false when value is not unique', () => {
    const contacts = contactListToFilter.list;
    contacts.push(contactListToFilter.input);
    const isExternalidExists = isValueUniqueInObjectListByKey(contacts, externalId, 'externalId');
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
    const withUndefinedKey = isValueUniqueInObjectListByKey(contacts, externalId, undefined);
    expect(withUndefinedKey).toEqual(true);
  });

  it('should return true when the value and list is null', () => {
    const withNullValueAndList = isValueUniqueInObjectListByKey(null, null, 'contactName');
    expect(withNullValueAndList).toEqual(true);
  });
});

describe('svgEncoding', () => {
  it('local svg path to base64 encoding works', async () => {
    const path = 'src/images/generic-id-01.svg';
    const imgFile = fs.readFileSync(path);
    const logoBlob = new File([imgFile], path, { type: 'image/svg+xml' });
    const encodedIcon = await blobToBase64(logoBlob);
    const expectedB64 =
      'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNTUiIGhlaWdodD0iNTkiIHZpZXdCb3g9IjAgMCA1NSA1OSIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTAuNTU2Mzk2IDMuNjI1NUMwLjU1NjM5NiAxLjY4Mjc5IDIuMTMxMjcgMC4xMDc5MSA0LjA3Mzk4IDAuMTA3OTFMNTAuOTcxNiAwLjEwNzkxQzUyLjkxNDMgMC4xMDc5MSA1NC40ODkyIDEuNjgyNzkgNTQuNDg5MiAzLjYyNTVWNTUuNDgyNEM1NC40ODkyIDU3LjQyNTEgNTIuOTE0MyA1OSA1MC45NzE2IDU5SDQuMDczOTlDMi4xMzEyOCA1OSAwLjU1NjM5NiA1Ny40MjUxIDAuNTU2Mzk2IDU1LjQ4MjRMMC41NTYzOTYgMy42MjU1WiIgZmlsbD0iI0Q4RDhEOCIvPgo8cGF0aCBkPSJNMjcuNjkzOSAyOS4xOTkzQzMxLjI5MTcgMjkuMTk5MyAzNC4yMDg2IDI1LjY1MzIgMzQuMjA4NiAyMS4yNzg4QzM0LjIwODYgMTYuOTA0MyAzMy4yNTEgMTMuMzU4MiAyNy42OTM5IDEzLjM1ODJDMjIuMTM2OCAxMy4zNTgyIDIxLjE3OSAxNi45MDQzIDIxLjE3OSAyMS4yNzg4QzIxLjE3OSAyNS42NTMyIDI0LjA5NTkgMjkuMTk5MyAyNy42OTM5IDI5LjE5OTNaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTUuMzg5NyA0MS4yOTQyQzE1LjM4ODYgNDEuMDI2NyAxNS4zODc1IDQxLjIxODggMTUuMzg5NyA0MS4yOTQyVjQxLjI5NDJaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMzkuOTk5OCA0MS41MDI1QzQwLjAwMzMgNDEuNDI5MyA0MC4wMDEgNDAuOTk0NCAzOS45OTk4IDQxLjUwMjVWNDEuNTAyNVoiIGZpbGw9IndoaXRlIi8+CjxwYXRoIGQ9Ik0zOS45ODQzIDQwLjk3MjhDMzkuODYzNiAzMy4zNTI1IDM4Ljg2OTMgMzEuMTgxMSAzMS4yNjA4IDI5LjgwNjZDMzEuMjYwOCAyOS44MDY2IDMwLjE4OTggMzEuMTcyNyAyNy42OTM1IDMxLjE3MjdDMjUuMTk3MiAzMS4xNzI3IDI0LjEyNiAyOS44MDY2IDI0LjEyNiAyOS44MDY2QzE2LjYwMDUgMzEuMTY2MSAxNS41NDU4IDMzLjMwNTMgMTUuNDA3MSA0MC43MjUzQzE1LjM5NTcgNDEuMzMxMSAxNS4zOTA1IDQxLjM2MyAxNS4zODg0IDQxLjI5MjZDMTUuMzg4OSA0MS40MjQ0IDE1LjM4OTQgNDEuNjY4MiAxNS4zODk0IDQyLjA5MzNDMTUuMzg5NCA0Mi4wOTMzIDE3LjIwMDggNDUuNzQ4NCAyNy42OTM1IDQ1Ljc0ODRDMzguMTg2IDQ1Ljc0ODQgMzkuOTk3NiA0Mi4wOTMzIDM5Ljk5NzYgNDIuMDkzM0MzOS45OTc2IDQxLjgyMDIgMzkuOTk3OCA0MS42MzAzIDM5Ljk5OCA0MS41MDExQzM5Ljk5NiA0MS41NDQ2IDM5Ljk5MTkgNDEuNDYwMiAzOS45ODQzIDQwLjk3MjhaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4K';
    expect(encodedIcon).toEqual(expectedB64);
  });
});
