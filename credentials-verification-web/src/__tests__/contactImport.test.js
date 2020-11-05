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
} from './__mocks__/mockContactsData';
import { mockContactHeadersMapping } from './__mocks__/mockHelpers';

const parseAndValidate = input => {
  const parsedAoA = arrayOfArraysToObjects(input);
  const { containsErrors, validationErrors } = validateContactsBulk(
    parsedAoA,
    input[0],
    mockContactHeadersMapping
  );
  const translatedBackContacts = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockContactHeadersMapping
  );

  return {
    containsErrors,
    validationErrors,
    translatedBackContacts
  };
};

it('validates contacts bulk import w/good data', () => {
  const testInput = validContacts.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(false);
  expect(validationErrors.length).toEqual(testInput.length);
  expect(translatedBackContacts).toEqual(validContacts.expectedParse);
});

it('validates contacts bulk import validation w/extra headers', () => {
  const testInput = extraHeaders.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(extraHeaders.expectedErrors);
  expect(translatedBackContacts).toEqual(extraHeaders.expectedParse);
});

it('validates contacts bulk import validation w/invalid headers order', () => {
  const testInput = invalidHeadersOrder.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidHeadersOrder.expectedErrors);
  expect(translatedBackContacts).toEqual(invalidHeadersOrder.expectedParse);
});

it('validates contacts bulk import validation w/invalid contact data', () => {
  const testInput = invalidContactData.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidContactData.expectedErrors);
  expect(translatedBackContacts).toEqual(invalidContactData.expectedParse);
});

it('validates contacts bulk import validation w/empty rows', () => {
  const testInput = emptyRows.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyRows.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyRows.expectedParse);
});

it('validates contacts bulk import validation w/empty file', () => {
  const testInput = emptyFile.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyFile.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyFile.expectedParse);
});

it('validates contacts bulk import validation w/empty data', () => {
  const testInput = emptyData.inputAoA;
  const { containsErrors, validationErrors, translatedBackContacts } = parseAndValidate(testInput);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyData.expectedErrors);
  expect(translatedBackContacts).toEqual(emptyData.expectedParse);
});
