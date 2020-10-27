import _ from 'lodash';

// Contact's bulk-import validations
export const validateContactsBulk = (contacts, inputHeaders, headersMapping) => {
  // trim last empty rows
  const trimmedContacts = trimEmptyRows(contacts);

  if (!trimmedContacts.length) return generateEmptyFileError();

  const expectedHeaders = headersMapping.map(h => h.translation);

  const headerErrors = validateHeaders(inputHeaders, expectedHeaders);
  let contactDataErrors = [];

  // validate contacts data only if headers are valid
  if (!arrayContainsErrors(headerErrors)) {
    contactDataErrors = validateContactsData(trimmedContacts, expectedHeaders);
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

const validateContactsData = (contacts, expectedHeaders) =>
  contacts.map((contact, index) => contactDataValidation({ ...contact, index }, expectedHeaders));

const validateHeaders = (inputHeaders, expectedHeaders) => {
  const trimmedHeaders = trimLastEmptyElements(inputHeaders);

  const headerErrors = _.isEqual(trimmedHeaders, expectedHeaders)
    ? [[]]
    : [generateInvalidHeadersError(trimmedHeaders, expectedHeaders)];

  return headerErrors;
};

const trimLastEmptyElements = array => {
  if (!array?.length) return [];
  return array[array.length - 1] ? array : trimLastEmptyElements(array.slice(0, array.length - 1));
};

// validate that parsed-csv contains correct contact data
const contactDataValidation = (contact, expectedHeaders) => {
  if (isEmptyRow(contact)) return generateEmptyRowError(contact.index);

  const requiredFieldsErrors = validateRequiredFields(contact, expectedHeaders);
  const extraFieldsErrors = validateNoExtraFields(contact, expectedHeaders);

  return [...requiredFieldsErrors, ...extraFieldsErrors];
};

// validate that each contact contains required data
const validateRequiredFields = (contact, expectedHeaders) =>
  expectedHeaders
    .map(header =>
      contact[header] ? null : generateRequiredFieldError(contact, header, expectedHeaders)
    )
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

const generateInvalidHeadersError = (inputHeaders, expectedHeaders) =>
  inputHeaders
    .map((inputHeader, idx) =>
      expectedHeaders.includes(inputHeader)
        ? validateHeaderPosition(inputHeader, idx, expectedHeaders)
        : generateExcessHeaderError(inputHeader, idx)
    )
    .filter(Boolean);

const validateHeaderPosition = (inputHeader, idx, expectedHeaders) =>
  expectedHeaders[idx] !== inputHeader
    ? generateHeaderPositionError(inputHeader, idx, expectedHeaders)
    : null;

const generateHeaderPositionError = (inputHeader, idx, expectedHeaders) => ({
  error: 'invalidHeaderPosition',
  row: { index: -1 },
  col: {
    index: idx,
    expectedIndex: expectedHeaders.indexOf(inputHeader),
    name: inputHeader
  }
});

const generateExcessHeaderError = (inputHeaders, idx) => ({
  error: 'excessHeader',
  row: { index: -1 },
  col: { index: idx, name: inputHeaders }
});

const generateRequiredFieldError = (contact, header, expectedHeaders) => ({
  error: 'required',
  row: { index: contact.index },
  col: { index: expectedHeaders.indexOf(header), name: header }
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

// un-translate headers to the original object keys
export const translateBackSpreadsheetNamesToContactKeys = (contactsData, headersMapping) =>
  contactsData.map(contact => renameProperties(contact, headersMapping));

const renameProperties = (contact, headersMapping) =>
  headersMapping.reduce(
    (acc, { key, translation }) => Object.assign(acc, { [key]: contact[translation] }),
    {}
  );
