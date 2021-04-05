import { saveAs } from 'file-saver';
import Papa from 'papaparse';
import i18n from 'i18next';

export const downloadTemplateCsv = (inputData, headersMapping) => {
  const csvData = inputData?.contacts?.length
    ? generateCsvFromInputData(inputData, headersMapping)
    : generateDefaultCsv(headersMapping.map(h => h.translation));
  const filename = getFilename(inputData);
  downloadCsvFile(filename, csvData);
};

const generateDefaultCsv = headers => headersToCsvString(headers);

const generateCsvFromInputData = ({ contacts }, headersMapping) => {
  const templateJSON = [...contacts].map(contact =>
    headersMapping.reduce((acc, h) => Object.assign(acc, { [h.translation]: contact[h.key] }), {})
  );

  return Papa.unparse(templateJSON);
};

const getFilename = ({ credentialType }) =>
  credentialType?.name
    ? `${i18n.t(credentialType.name)}.csv`
    : `${i18n.t('generic.contactsTemplate')}.csv`;

// empty csv from headers array
const headersToCsvString = headers => headers.reduce((acc, h) => (!acc ? h : `${acc},${h}`), null);

const downloadCsvFile = (filename, data) => {
  const blob = new Blob([data], { type: 'text/csv;charset=utf-8' });
  saveAs(blob, filename);
};

// remove óíúáé, blank spaces, etc from keys
const getKeysFromArrayOfArrays = arrayOfArrays =>
  arrayOfArrays[0].map(key =>
    key
      .replace(/ /g, ' ')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
  );

// take an array of arrays and turn it into an array of objects
export const arrayOfArraysToObjects = arrayOfArrays => {
  const keys = getKeysFromArrayOfArrays(arrayOfArrays);
  // eslint-disable-next-line no-magic-numbers
  const values = arrayOfArrays.slice(1);

  return values.map(array =>
    array.reduce((acc, value, idx) => Object.assign(acc, { [keys[idx]]: value }), {
      originalArray: array
    })
  );
};

export const getColName = col => {
  const columnLetters = 26;
  const asciiShift = 65;

  const fold = Math.floor(col / columnLetters);
  /* eslint-disable no-magic-numbers */
  return fold >= 0
    ? getColName(fold - 1) + String.fromCharCode(asciiShift + (col % columnLetters))
    : '';
  /* eslint-enable no-magic-numbers */
};

// spreadsheets row numbers offset:
// +1 because row numeration starts from 1 (instead of 0)
// +1 because of headers row
// eslint-disable-next-line no-magic-numbers
export const getRowNumber = rowIndex => rowIndex + 1 + 1;

export const trimEmptyRows = contacts => {
  if (!contacts?.length) return [];
  /* eslint-disable no-magic-numbers */
  return isEmptyRow(contacts[contacts.length - 1])
    ? trimEmptyRows(contacts.slice(0, contacts.length - 1))
    : contacts;
  /* eslint-enable no-magic-numbers */
};

export const isEmptyRow = ({ originalArray }) => originalArray.every(field => !field);
