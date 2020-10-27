import { saveAs } from 'file-saver';
import Papa from 'papaparse';
import _ from 'lodash';
import {
  ALLOWED_IMAGE_TYPES,
  MAX_FILE_SIZE,
  ALLOWED_EXCEL_TYPES,
  TOO_LARGE,
  INVALID_TYPE,
  IMAGE,
  EXCEL,
  COMMON_CONTACT_HEADERS
} from './constants';

const isInvalidFile = size => (size > MAX_FILE_SIZE ? TOO_LARGE : null);

const isInvalidPicture = ({ type, size }) => {
  const invalidType = !ALLOWED_IMAGE_TYPES.includes(type);

  if (invalidType) return INVALID_TYPE;

  return isInvalidFile(size);
};

const isInvalidExcel = ({ type, size }) => {
  const invalidType = !ALLOWED_EXCEL_TYPES.includes(type);

  if (invalidType) return INVALID_TYPE;

  return isInvalidFile(size);
};

const handlerDictionary = {
  image: isInvalidPicture,
  excel: isInvalidExcel,
  any: ({ size }) => isInvalidFile(size)
};

const fileToFileReader = (file, type) =>
  new Promise((resolve, reject) => {
    const invalidFile = handlerDictionary[type](file);

    if (invalidFile) return reject(new Error(invalidFile));

    const reader = new FileReader();
    reader.readAsArrayBuffer(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });

export const imageToFileReader = image => fileToFileReader(image, IMAGE);
export const excelToFileReader = excel => fileToFileReader(excel, EXCEL);

export const downloadTemplateCsv = (inputData, headers) => {
  const csvData = inputData ? generateCsvFromInputData(inputData) : generateDefaultCsv(headers);
  const filename = inputData ? getFilename(inputData) : 'template.csv';
  downloadCsvFile(filename, csvData);
};

const generateDefaultCsv = headers => headersToCsvString(headers);

const generateCsvFromInputData = ({ contacts, credentialType }) => {
  if (!contacts.length) {
    return { error: 'no target subjects' };
  }
  const headers = [...COMMON_CONTACT_HEADERS, ...Object.keys(credentialType.fields)];

  const noRepeatedHeaders = _.uniq(headers);

  const templateJSON = contacts.map(contact =>
    noRepeatedHeaders.reduce((acc, h) => Object.assign(acc, { [h]: contact[h] }), {})
  );

  return Papa.unparse(templateJSON);
};

const getFilename = ({ credentialType }) => `${credentialType.name}.csv`;

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
  const values = arrayOfArrays.slice(1);

  return values.map(array =>
    array.reduce((acc, value, idx) => Object.assign(acc, { [keys[idx]]: value }), {
      originalArray: array
    })
  );
};

export const getColName = col => {
  const fold = Math.floor(col / 26);
  return fold >= 0 ? getColName(fold - 1) + String.fromCharCode(65 + (col % 26)) : '';
};

// spreadsheets row numbers offset:
// +1 because row numeration starts from 1 (instead of 0)
// +1 because of headers row
export const getRowNumber = rowIndex => rowIndex + 1 + 1;
