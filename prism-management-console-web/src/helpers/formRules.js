import moment from 'moment';

export const futureDate = (value, cb, compareTo) => {
  if (!value) return cb('error');
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
  { name: 'number', regex: new RegExp('(?=.*[0-9])') }, // At least 1 number
  { name: 'lowercase', regex: new RegExp('(?=.*[a-z])') }, // At least 1 lower case
  { name: 'uppercase', regex: new RegExp('(?=.*[A-Z])') }, // At least 1 upper case
  { name: 'minlength', regex: new RegExp('.{8,}') }, // At leas 8 characters
  { name: 'nospaces', regex: new RegExp('^((?! ).)*$') } // No blank spaces
];

const validatePassword = (value = '') =>
  validations.reduce(
    (accum, current) => (current.regex.test(value) ? accum : accum.concat(current.name)),
    []
  );

const formatPasswordErrors = (errors, t) => {
  const prefix = 'registration.password.invalidPassword.';

  return errors.reduce((accum, current, currentIndex) => {
    let link = '';

    if (currentIndex < errors.length - 2) link = ', ';
    else if (currentIndex < errors.length - 1) link = ` ${t(`${prefix}and`)} `;
    else if (currentIndex === errors.length - 1) link = '.';

    return accum + t(`${prefix}${current}`) + link;
  }, `${t(`${prefix}initial`)} `);
};

export const passwordFormatValidation = (value, cb, t) => {
  // if (!value) cb();
  const errors = validatePassword(value);
  if (!errors.length) cb();
  else cb(formatPasswordErrors(errors, t));
};
