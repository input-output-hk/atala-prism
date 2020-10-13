import { aoaToObjects } from '../helpers/fileHelpers';
import { validateContactsBulk } from '../helpers/contactValidations';
import {
  validContacts,
  invalidHeadersOrder,
  extraHeaders,
  invalidContactData,
  emptyRows,
  emptyData,
  emptyFile
} from './__mocks__/contactsMockData';

it('validates contacts bulk import w/good data', () => {
  const testInput = validContacts.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(false);
  expect(validationErrors.length).toEqual(testInput.length);
  expect(parsedAoA).toEqual(validContacts.expectedParse);
});

it('validates contacts bulk import validation w/extra headers', () => {
  const testInput = extraHeaders.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(extraHeaders.expectedErrors);
  expect(parsedAoA).toEqual(extraHeaders.expectedParse);
});

it('validates contacts bulk import validation w/invalid headers order', () => {
  const testInput = invalidHeadersOrder.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidHeadersOrder.expectedErrors);
  expect(parsedAoA).toEqual(invalidHeadersOrder.expectedParse);
});

it('validates contacts bulk import validation w/invalid contact data', () => {
  const testInput = invalidContactData.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(invalidContactData.expectedErrors);
  expect(parsedAoA).toEqual(invalidContactData.expectedParse);
});

it('validates contacts bulk import validation w/empty rows', () => {
  const testInput = emptyRows.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyRows.expectedErrors);
  expect(parsedAoA).toEqual(emptyRows.expectedParse);
});

it('validates contacts bulk import validation w/empty file', () => {
  const testInput = emptyFile.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyFile.expectedErrors);
  expect(parsedAoA).toEqual(emptyFile.expectedParse);
});

it('validates contacts bulk import validation w/empty data', () => {
  const testInput = emptyData.inputAoA;

  const parsedAoA = aoaToObjects(testInput);
  const { containsErrors, validationErrors } = validateContactsBulk(parsedAoA, testInput[0]);

  expect(containsErrors).toEqual(true);
  expect(validationErrors).toEqual(emptyData.expectedErrors);
  expect(parsedAoA).toEqual(emptyData.expectedParse);
});
