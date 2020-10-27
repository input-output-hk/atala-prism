import { arrayOfArraysToObjects } from '../helpers/fileHelpers';
import {
  translateBackSpreadsheetNamesToContactKeys,
  validateContactsBulk
} from '../helpers/contactValidations';
import {
  validContacts,
  invalidHeadersOrder,
  extraHeaders,
  invalidContactData,
  emptyRows,
  emptyData,
  emptyFile
} from './__mocks__/contactsMockData';
import { mockHeadersMapping } from './__mocks__/mockHelpers';

it('validates contacts bulk import w/good data', () => {
  const testInput = validContacts.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(false);
  expect(validationErrors.length).toEqual(testInput.length);
  expect(translatedBackContacts).toEqual(validContacts.expectedParse);
});

it('validates contacts bulk import validation w/extra headers', () => {
  const testInput = extraHeaders.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(extraHeaders.expectedErrors);
  expect(translatedBackContacts).toEqual(extraHeaders.expectedParse);
});

it('validates contacts bulk import validation w/invalid headers order', () => {
  const testInput = invalidHeadersOrder.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidHeadersOrder.expectedErrors);
  expect(translatedBackContacts).toEqual(invalidHeadersOrder.expectedParse);
});

it('validates contacts bulk import validation w/invalid contact data', () => {
  const testInput = invalidContactData.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidContactData.expectedErrors);
  expect(translatedBackContacts).toEqual(invalidContactData.expectedParse);
});

it('validates contacts bulk import validation w/empty rows', () => {
  const testInput = emptyRows.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyRows.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyRows.expectedParse);
});

it('validates contacts bulk import validation w/empty file', () => {
  const testInput = emptyFile.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyFile.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyFile.expectedParse);
});

it('validates contacts bulk import validation w/empty data', () => {
  const testInput = emptyData.inputAoA;

  const parsedAoA = arrayOfArraysToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    testInput[0],
    mockHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockHeadersMapping
  );
  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyData.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyData.expectedParse);
});
