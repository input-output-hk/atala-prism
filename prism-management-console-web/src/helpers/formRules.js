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
