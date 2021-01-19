import _ from 'lodash';
import moment from 'moment';
import { COMMON_CREDENTIALS_HEADERS, DEFAULT_DATE_FORMAT, EXTERNAL_ID_KEY } from './constants';
import { isEmptyRow, trimEmptyRows } from './fileHelpers';

// Credentials data bulk-import validations
export const validateCredentialDataBulk = (
  credentialType,
  credentialsData,
  inputHeaders,
  headersMapping,
  recipients
) => {
  // trim last empty rows
  const trimmedCredentials = trimEmptyRows(credentialsData);

  if (!trimmedCredentials.length) return generateEmptyFileError();

  const expectedHeaders = getExpectedHeaders(headersMapping);

  const headerErrors = validateHeaders(inputHeaders, expectedHeaders);
  let credentialsDataErrors = [];

  // validate contacts data only if headers are valid
  if (!arrayContainsErrors(headerErrors)) {
    credentialsDataErrors = validateCredentialsData(
      trimmedCredentials,
      credentialType.fields,
      expectedHeaders,
      headersMapping,
      recipients
    );
  }

  const validationErrors = [...headerErrors, ...credentialsDataErrors];

  return {
    validationErrors,
    containsErrors: arrayContainsErrors(validationErrors)
  };
};

const getExpectedHeaders = headersMapping => {
  const allExpectedHeaders = headersMapping.map(h => h.translation);

  const expectedCommonHeaders = COMMON_CREDENTIALS_HEADERS.map(ch => {
    const commonMappings = headersMapping.find(hm => hm.key === ch);
    return commonMappings.translation;
  });

  const expectedSpecificHeaders = allExpectedHeaders.filter(
    h => !expectedCommonHeaders.includes(h)
  );

  return {
    expectedCommonHeaders,
    expectedSpecificHeaders,
    allExpectedHeaders
  };
};

const validateCredentialsData = (
  credentialsData,
  fieldsValidations,
  expectedHeaders,
  headersMapping,
  recipients
) =>
  credentialsData.map((dataRow, index) =>
    credentialDataValidation(
      { ...dataRow, index },
      fieldsValidations,
      expectedHeaders,
      headersMapping,
      recipients
    )
  );

const validateHeaders = (inputHeaders, expectedHeaders) => {
  const { allExpectedHeaders } = expectedHeaders;
  const trimmedHeaders = trimLastEmptyElements(inputHeaders);

  const headerErrors = _.eq(trimmedHeaders, allExpectedHeaders)
    ? [[]]
    : [generateInvalidHeadersError(trimmedHeaders, allExpectedHeaders)];

  return headerErrors;
};

const trimLastEmptyElements = array => {
  if (!array?.length) return [];
  return array[array.length - 1] ? array : trimLastEmptyElements(array.slice(0, array.length - 1));
};

// validate that parsed-csv contains correct credential data
const credentialDataValidation = (
  dataRow,
  fieldsValidations,
  expectedHeaders,
  headersMapping,
  recipients
) => {
  if (isEmptyRow(dataRow)) return generateEmptyRowError(dataRow.index);

  const commonFieldsErrors = validateCommonFields(
    dataRow,
    expectedHeaders,
    recipients,
    headersMapping
  );

  const specificFieldsErrors = validateCredentialFields(
    dataRow,
    fieldsValidations,
    expectedHeaders,
    headersMapping
  );

  const extraFieldsErrors = validateNoExtraFields(dataRow, expectedHeaders);

  return [...commonFieldsErrors, ...specificFieldsErrors, ...extraFieldsErrors];
};

const validateCommonFields = (
  dataRow,
  { expectedCommonHeaders, allExpectedHeaders },
  recipients,
  headersMapping
) =>
  expectedCommonHeaders
    .map(header => {
      const { key } = headersMapping.find(({ translation }) => translation === header);
      const { translation: translatedHeader } = headersMapping.find(
        ({ key: externalIdKey }) => externalIdKey === EXTERNAL_ID_KEY
      );
      const isExternalID = key === EXTERNAL_ID_KEY;
      const importedValue = dataRow[header];
      const importedExternalID = dataRow[translatedHeader];
      if (!importedValue)
        return generateCommonFieldError('required', dataRow, header, allExpectedHeaders);

      return recipients.some(
        ({ [key]: expectedValue, [EXTERNAL_ID_KEY]: expectedExternalID }) =>
          importedValue === expectedValue && importedExternalID === expectedExternalID
      )
        ? null
        : generateCommonFieldError(
            isExternalID ? 'unexpectedExternalID' : 'valueDoesNotMatch',
            dataRow,
            header,
            allExpectedHeaders
          );
    })
    .filter(Boolean);

const generateInvalidHeadersError = (input, allExpectedHeaders) =>
  input
    .map((inputHeader, idx) =>
      allExpectedHeaders.includes(inputHeader)
        ? validateHeaderPosition(inputHeader, idx, allExpectedHeaders)
        : generateExcessHeaderError(inputHeader, idx, allExpectedHeaders)
    )
    .filter(Boolean);

const validateHeaderPosition = (inputHeader, idx, expectedHeaders) =>
  expectedHeaders[idx] !== inputHeader
    ? generateHeaderPositionError(inputHeader, idx, expectedHeaders)
    : null;

// validate that each credential contains required data
const validateCredentialFields = (
  dataRow,
  fieldsValidations,
  { allExpectedHeaders },
  headersMapping
) => {
  const specificFields = fieldsValidations.filter(excludeCommonHeadersFilter);
  const specificFieldsValidations = specificFields.map(field =>
    field.validations
      .map(validation => {
        const validator = getValidationByName(validation);
        return validator(dataRow, getTranslatedKey(field, headersMapping), allExpectedHeaders);
      })
      .filter(Boolean)
  );
  const filteredNoErrors = specificFieldsValidations.filter(array => array.length);
  const result = filteredNoErrors.flat();

  return result;
};

const getValidationByName = validationName => {
  switch (validationName) {
    case 'required': {
      return requiredValidation;
    }
    case 'isDate': {
      return isDateValidation;
    }
    case 'pastDate': {
      return pastDateValidation;
    }
    case 'futureDate': {
      return futureDateValidation;
    }
    default: {
      return defaultValidation;
    }
  }
};

const getTranslatedKey = (field, headersMapping) => {
  const hm = headersMapping.find(h => h.key === field.key);
  return hm.translation;
};

const defaultValidation = () => null;

const requiredValidation = (dataRow, translatedKey, expectedHeaders) => {
  const row = dataRow.index;
  const col = expectedHeaders.indexOf(translatedKey);
  return dataRow[translatedKey] ? null : generateRequiredFieldError(row, col, translatedKey);
};

const isDateValidation = (dataRow, translatedKey, expectedHeaders) => {
  const inputDate = dataRow[translatedKey];
  if (!inputDate) return null;

  const dateAsMoment = parseDateFromCsv(inputDate);

  if (!dateAsMoment.isValid()) {
    const row = dataRow.index;
    const col = expectedHeaders.indexOf(translatedKey);
    return generateDateFormatError(row, col, translatedKey);
  }
};

const pastDateValidation = (dataRow, translatedKey, expectedHeaders) => {
  const inputDate = dataRow[translatedKey];
  if (!inputDate) return null;
  const now = moment();

  const dateAsMoment = parseDateFromCsv(inputDate);

  if (!dateAsMoment.isValid()) {
    const row = dataRow.index;
    const col = expectedHeaders.indexOf(translatedKey);
    return generateDateFormatError(row, col, translatedKey);
  }

  const isBefore = moment(dateAsMoment).isSameOrBefore(now);

  if (!isBefore) {
    const row = dataRow.index;
    const col = expectedHeaders.indexOf(translatedKey);
    return generateNotAPastDateError(row, col, translatedKey);
  }

  return null;
};

const futureDateValidation = (dataRow, translatedKey, expectedHeaders) => {
  const inputDate = dataRow[translatedKey];
  if (!inputDate) return null;

  const now = moment();

  const dateAsMoment = parseDateFromCsv(inputDate);

  if (!dateAsMoment.isValid()) {
    const row = dataRow.index;
    const col = expectedHeaders.indexOf(translatedKey);
    return generateDateFormatError(row, col, translatedKey);
  }

  const isAfter = moment(dateAsMoment).isSameOrAfter(now);

  if (!isAfter) {
    const row = dataRow.index;
    const col = expectedHeaders.indexOf(translatedKey);
    return generateNotAFutureDateError(row, col, translatedKey);
  }

  return null;
};

const parseDateFromCsv = (inputDate, expectedFormat = DEFAULT_DATE_FORMAT) =>
  moment(inputDate, expectedFormat);

// validate that each credential doesn't contain excess data
const validateNoExtraFields = (dataRow, expectedHeaders) => {
  const rowLength = dataRow.originalArray.length;
  const expectedLength = expectedHeaders.length;

  const difference = rowLength - expectedLength;

  return difference > 0
    ? generateExtraFieldsErrors(dataRow.index, dataRow.originalArray, expectedLength)
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

const generateRequiredFieldError = (row, col, translatedKey) => ({
  error: 'required',
  row: { index: row },
  col: { index: col, name: translatedKey }
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

const generateCommonFieldError = (error, dataRow, translatedKey, allExpectedHeaders) => ({
  error,
  row: { index: dataRow.index },
  col: { index: allExpectedHeaders.indexOf(translatedKey), name: translatedKey }
});

const generateDateFormatError = (row, col, key) => ({
  error: 'dateFormat',
  row: { index: row },
  col: { index: col, name: key }
});

const generateNotAPastDateError = (row, col, key) => ({
  error: 'notAPastDate',
  row: { index: row },
  col: { index: col, name: key }
});

const generateNotAFutureDateError = (row, col, key) => ({
  error: 'notAFutureDate',
  row: { index: row },
  col: { index: col, name: key }
});

const arrayContainsErrors = validationsArray => validationsArray.some(array => array.length);

const excludeCommonHeadersFilter = ({ key }) => !COMMON_CREDENTIALS_HEADERS.includes(key);
