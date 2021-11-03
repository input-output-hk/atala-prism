import { ALLOWED_TYPES, MAX_FILE_SIZE } from '../../helpers/constants';
import Logger from '../../helpers/Logger';

const s3Endpoint = file => new Promise(resolve => setTimeout(() => resolve(file), 1000));

export const savePictureInS3 = file =>
  new Promise((resolve, reject) => {
    const { type, size } = file;

    const savePicture = 'errors.savePicture.';

    const invalidType = !ALLOWED_TYPES.includes(type);

    if (invalidType) {
      Logger.error(`The file was a ${type} when it should have been a jpeg or png`);
      reject(new Error(`${savePicture}invalidType`));
    }

    const tooLarge = MAX_FILE_SIZE < size;

    if (tooLarge) {
      Logger.error(`The file had a size of ${size} when the maximum is ${MAX_FILE_SIZE}`);
      reject(new Error(`${savePicture}tooLarge`, { maxSize: MAX_FILE_SIZE }));
    }

    s3Endpoint(file)
      .then(response => resolve(response))
      .catch(error => {
        Logger.error('An error ocurred while uploading the file. Error: ', error);
        reject(new Error(`${savePicture}uploadError`));
      });
  });

export const saveDraft = ({ degreeName, award, startDate, graduationDate, logoUniversity }) =>
  new Promise((resolve, reject) => {
    if (
      !degreeName &&
      !award &&
      !startDate &&
      !graduationDate &&
      (!logoUniversity || !logoUniversity.length)
    ) {
      Logger.error('Trying to save empty credential');
      reject(
        new Error({
          error: 'Trying to save empty credential',
          errorMessage: 'errors.emptyCredential'
        })
      );
    }

    const credentialToSave = {
      id: Math.ceil(Math.random() * 1000),
      degreeName,
      award,
      startDate,
      graduationDate,
      logoUniversity
    };

    resolve(credentialToSave);
  });

export const saveCredential = ({
  degreeName,
  award,
  startDate,
  graduationDate,
  logoUniversity,
  groupId
}) =>
  new Promise((resolve, reject) => {
    const missingParams =
      !degreeName || !award || !startDate || !graduationDate || !logoUniversity || !groupId;

    if (missingParams) return reject(new Error('errors.invalidCredential'));

    resolve(200);
  });
