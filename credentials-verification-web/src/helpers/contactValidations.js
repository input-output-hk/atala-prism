import _ from 'lodash';
import { COMMON_CONTACT_HEADERS } from './constants';

// Contact's bulk-import validations
export const validateContactsBulk = (contacts, inputHeaders) => {
  // trim last empty rows
  const trimmedContacts = trimEmptyRows(contacts);

  if (!trimmedContacts.length) return generateEmptyFileError();

  const headerErrors = validateHeaders(inputHeaders);
  let contactDataErrors = [];

  // validate contacts data only if headers are valid
  if (!arrayContainsErrors(headerErrors)) {
    contactDataErrors = validateContactsData(trimmedContacts);
  }

  const validationErrors = [...headerErrors, ...contactDataErrors];

  return {
    validationErrors,
    containsErrors: arrayContainsErrors(validationErrors)
  };
};

const trimEmptyRows = contacts => {
  if (!contacts?.length) return [];
  return isEmptyRow(contacts[contacts.length - 1])
    ? trimEmptyRows(contacts.slice(0, contacts.length - 1))
    : contacts;
};

const isEmptyRow = ({ originalArray }) => originalArray.every(field => !field);

const validateContactsData = contacts =>
  contacts.map((contact, index) => contactDataValidation({ ...contact, index }));

const validateHeaders = inputHeaders => {
  const trimmedHeaders = trimLastEmptyElements(inputHeaders);

  return _.eq(trimmedHeaders, COMMON_CONTACT_HEADERS)
    ? [[]]
    : [generateInvalidHeadersError(trimmedHeaders)];
};

const trimLastEmptyElements = array => {
  if (!array?.length) return [];
  return array[array.length - 1] ? array : trimLastEmptyElements(array.slice(0, array.length - 1));
};

// validate that parsed-csv contains correct contact data
const contactDataValidation = (contact, headers = COMMON_CONTACT_HEADERS) => {
  if (isEmptyRow(contact)) return generateEmptyRowError(contact.index);

  const requiredFieldsErrors = validateRequiredFields(contact, headers);
  const extraFieldsErrors = validateNoExtraFields(contact, headers);

  return [...requiredFieldsErrors, ...extraFieldsErrors];
};

// validate that each contact contains required data
const validateRequiredFields = (contact, headers) =>
  headers
    .map(header => (contact[header] ? null : generateRequiredFieldError(contact, header)))
    .filter(Boolean);

// validate that each contact doesn't contain excess data
const validateNoExtraFields = (contact, headers) => {
  const rowLength = contact.originalArray.length;
  const expectedLength = headers.length;

  const difference = rowLength - expectedLength;

  return difference > 0
    ? generateExtraFieldsErrors(contact.index, contact.originalArray, expectedLength)
    : [];
};

// Error objects generation
const generateEmptyFileError = () => ({
  validationErrors: [
    [
      {
        error: 'emptyFile',
        row: { index: 0 },
        col: { index: 0 }
      }
    ]
  ],
  containsErrors: true
});

const generateEmptyRowError = row => [
  {
    error: 'emptyRow',
    row: { index: row },
    col: { index: 0 }
  }
];

const generateInvalidHeadersError = inputHeaders =>
  inputHeaders
    .map((inputHeader, idx) =>
      COMMON_CONTACT_HEADERS.includes(inputHeader)
        ? validateHeaderPosition(inputHeader, idx)
        : generateExcessHeaderError(inputHeader, idx)
    )
    .filter(Boolean);

const validateHeaderPosition = (inputHeader, idx) =>
  COMMON_CONTACT_HEADERS[idx] !== inputHeader
    ? generateHeaderPositionError(inputHeader, idx)
    : null;

const generateHeaderPositionError = (inputHeader, idx) => ({
  error: 'invalidHeaderPosition',
  row: { index: -1 },
  col: {
    index: idx,
    expectedIndex: COMMON_CONTACT_HEADERS.indexOf(inputHeader),
    name: inputHeader
  }
});

const generateExcessHeaderError = (inputHeaders, idx) => ({
  error: 'excessHeader',
  row: { index: -1 },
  col: { index: idx, name: inputHeaders }
});

const generateRequiredFieldError = (contact, header) => ({
  error: 'required',
  row: { index: contact.index },
  col: { index: COMMON_CONTACT_HEADERS.indexOf(header), name: header }
});

const generateExtraFieldsErrors = (rowIdx, data, lastValidIndex) =>
  data
    .slice(lastValidIndex)
    .map((extraField, colIdx) =>
      extraField
        ? {
            error: 'extraField',
            row: { index: rowIdx },
            col: { index: lastValidIndex + colIdx, content: extraField }
          }
        : null
    )
    .filter(Boolean);

const arrayContainsErrors = validationsArray => validationsArray.some(array => array.length);
