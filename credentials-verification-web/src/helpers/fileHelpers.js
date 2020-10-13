import { saveAs } from 'file-saver';
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

export const downloadTemplateCsv = inputData => {
  const csvData = inputData ? generateCsvFromInputData(inputData) : generateDefaultCsv();
  const filename = inputData ? getFilename(inputData) : 'template.csv';
  downloadCsvFile(filename, csvData);
};

const generateDefaultCsv = () => headersToCsvString(COMMON_CONTACT_HEADERS);

const generateCsvFromInputData = inputData => {
  // TODO: generate csv from contacts and credential type(s?)
};

const getFilename = inputData => {
  // TODO: generate filename from credential type(s?)
};

// empty csv from headers array
const headersToCsvString = headers => headers.reduce((acc, h) => (!acc ? h : `${acc},${h}`), null);

const downloadCsvFile = (filename, data) => {
  const blob = new Blob([data], { type: 'text/csv;charset=utf-8' });
  saveAs(blob, filename);
};

// remove óíúáé, blank spaces, etc from keys
const getKeysFromAoA = aoa =>
  aoa[0].map(key =>
    key
      .replace(/ /g, ' ')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
  );

export const aoaToObjects = aoa => {
  const keys = getKeysFromAoA(aoa);
  const values = aoa.slice(1);

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
