import {
  ALLOWED_IMAGE_TYPES,
  MAX_FILE_SIZE,
  ALLOWED_EXCEL_TYPES,
  TOO_LARGE,
  INVALID_TYPE,
  IMAGE,
  EXCEL
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
