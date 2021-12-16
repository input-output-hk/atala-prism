import moment from 'moment';
import i18n from 'i18next';
import { humanizeCamelCaseString } from './genericHelpers';

export const isValueInListByKey = (list, value, field) =>
  !!value && list?.some(item => item[field] === value);

export const isValueUniqueInObjectListByKey = (list, value, field) => {
  const filteredList = list?.filter(item => item[field] === value);
  return !value || filteredList.length <= 1;
};

export const pastDate = (value, cb, compareTo) => {
  if (!value) return cb('error');
  const isAfter = moment(value).isSameOrAfter(compareTo);

  if (isAfter) {
    cb('error');
  } else {
    cb();
  }
};

export const futureDate = (value, cb, compareTo) => {
  if (!compareTo) {
    cb();
  } else {
    const isBefore = moment(value).isSameOrBefore(compareTo);

    if (isBefore) {
      cb('error');
    } else {
      cb();
    }
  }
};

export const noEmptyInput = message => ({
  required: true,
  whitespace: true,
  message
});

export const noEmptyInputExternalValue = (value, cb) => {
  if (!value) cb('error');
  else cb();
};

export const minOneElement = (value, cb) => {
  if (value && value.length) cb();
  else cb('error');
};

export const passwordValidation = (value, cb, otherPass) => {
  if (!value || !otherPass || value === otherPass) cb();
  else cb('error');
};

const validations = [
  { name: 'number', regex: /\d/ }, // At least 1 number
  { name: 'lowercase', regex: /[a-z]/ }, // At least 1 lower case
  { name: 'uppercase', regex: /[A-Z]/ }, // At least 1 upper case
  { name: 'minlength', regex: /.{8,}/ }, // At leas 8 characters
  { name: 'nospaces', regex: /^((?! ).)*$/ } // No blank spaces
];

const validatePassword = (value = '') =>
  validations.reduce(
    (accum, current) => (current.regex.test(value) ? accum : accum.concat(current.name)),
    []
  );

const formatPasswordErrors = errors => {
  const prefix = 'registration.password.invalidPassword';
  const initialValue = i18n.t(`${prefix}.initial`);
  const and = i18n.t(`${prefix}.and`);

  return errors.reduce((accum, current, currentIndex) => {
    let link = '';
    /* eslint no-magic-numbers: "off" */
    if (currentIndex < errors.length - 2) link = ',';
    else if (currentIndex < errors.length - 1) link = ` ${and}`;
    else if (currentIndex === errors.length - 1) link = '.';

    const currentError = i18n.t(`${prefix}.${current}`);

    return `${accum} ${currentError}${link}`;
  }, initialValue);
};

export const passwordFormatValidation = (value, cb) => {
  const errors = validatePassword(value);
  if (!errors.length) cb();
  else cb(formatPasswordErrors(errors));
};

export const generateRequiredRule = (isDate, dataIndex) =>
  Object.assign(
    isDate
      ? {
          validator: (rule, value, cb) => {
            if (!value?._isValid) cb(rule.message);
            else cb();
          }
        }
      : { required: true, whitespace: true },
    {
      message: i18n.t('manualImport.table.required', {
        field: i18n.t(`contacts.table.columns.${dataIndex}`, {
          defaultValue: humanizeCamelCaseString(dataIndex)
        })
      })
    }
  );

export const expectValueNotExist = (list, value, field, callback) => {
  if (isValueInListByKey(list, value, field)) throw new Error(`${field} already exists`);
  callback();
};

export const expectUniqueValue = (list, value, field, callback) => {
  if (!isValueUniqueInObjectListByKey(list, value, field))
    throw new Error(`${field} must be unique`);
  callback();
};
