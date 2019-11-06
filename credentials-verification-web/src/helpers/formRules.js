import moment from 'moment';

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
