import 'core-js';
import { arrayOfArraysToObjects } from '../helpers/fileHelpers';
import { validateCredentialDataBulk } from '../helpers/credentialDataValidation';
import { mockGovernmentId } from './__mocks__/mockCredentialTypes';
import {
  contacts,
  validCredentials,
  extraHeaders,
  invalidHeadersOrder,
  invalidCredentialsData,
  emptyRows,
  emptyFile,
  emptyData,
  invalidDates
} from './__mocks__/mockCredentialsData';
import { mockGovernmentIdHeadersMapping } from './__mocks__/mockHelpers';
import { translateBackSpreadsheetNamesToContactKeys } from '../helpers/contactValidations';

const parseAndValidate = input => {
  const parsedAoA = arrayOfArraysToObjects(input);

  const { containsErrors, validationErrors } = validateCredentialDataBulk(
    mockGovernmentId,
    parsedAoA,
    input[0],
    mockGovernmentIdHeadersMapping,
    contacts
  );

  const translatedBackCredentials = translateBackSpreadsheetNamesToContactKeys(
    parsedAoA,
    mockGovernmentIdHeadersMapping
  );

  return {
    containsErrors,
    validationErrors,
    translatedBackCredentials
  };
};

it('validates credentials bulk import w/good data', () => {
  const testInput = validCredentials.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(false);
  expect(validationErrors.length).toEqual(testInput.length);
  expect(translatedBackCredentials).toEqual(validCredentials.expectedParse);
});

it('validates credentials bulk import validation w/extra headers', () => {
  const testInput = extraHeaders.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(extraHeaders.expectedErrors);
  expect(translatedBackCredentials).toEqual(extraHeaders.expectedParse);
});

it('validates credentials bulk import validation w/invalid headers order', () => {
  const testInput = invalidHeadersOrder.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidHeadersOrder.expectedErrors);
  expect(translatedBackCredentials).toEqual(invalidHeadersOrder.expectedParse);
});

it('validates credentials bulk import validation w/invalid contact data', () => {
  const testInput = invalidCredentialsData.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidCredentialsData.expectedErrors);
  expect(translatedBackCredentials).toEqual(invalidCredentialsData.expectedParse);
});

it('validates credentials bulk import validation w/empty rows', () => {
  const testInput = emptyRows.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyRows.expectedErrors);
  expect(translatedBackCredentials).toEqual(emptyRows.expectedParse);
});

it('validates credentials bulk import validation w/empty file', () => {
  const testInput = emptyFile.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyFile.expectedErrors);
  expect(translatedBackCredentials).toEqual(emptyFile.expectedParse);
});

it('validates credentials bulk import validation w/empty data', () => {
  const testInput = emptyData.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyData.expectedErrors);
  expect(translatedBackCredentials).toEqual(emptyData.expectedParse);
});

it('validates credentials bulk import validation w/invalid dates', () => {
  const testInput = invalidDates.inputAoA;
  const { containsErrors, validationErrors, translatedBackCredentials } = parseAndValidate(
    testInput
  );

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidDates.expectedErrors);
  expect(translatedBackCredentials).toEqual(invalidDates.expectedParse);
});
