import moment from 'moment';
import { ISSUER, VERIFIER } from '../../helpers/constants';

export const futureDate = (value, cb, compareTo) => {
  const isAfter = moment(value).isSameOrAfter(compareTo);

  if (isAfter) {
    cb('error');
  } else {
    cb();
  }
};

export const pastDate = (value, cb, compareTo) => {
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

const formatPasswordErrors = (errors, t) => {
  const prefix = 'registration.password.invalidPassword';
  const initialValue = t(`${prefix}.initial`);
  const and = t(`${prefix}.and`);

  return errors.reduce((accum, current, currentIndex) => {
    let link = '';

    if (currentIndex < errors.length - 2) link = ',';
    else if (currentIndex < errors.length - 1) link = ` ${and}`;
    else if (currentIndex === errors.length - 1) link = '.';

    const currentError = t(`${prefix}.${current}`);

    return `${accum} ${currentError}${link}`;
  }, initialValue);
};

export const passwordFormatValidation = (value, cb, t) => {
  const errors = validatePassword(value);
  if (!errors.length) cb();
  else cb(formatPasswordErrors(errors, t));
};

const emailRegex = /^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/;

export const emailFormatValidation = (value, cb) => (emailRegex.test(value) ? cb() : cb('error'));

export const isValidRole = (value, cb) => ([ISSUER, VERIFIER].includes(value) ? cb() : cb('error'));
