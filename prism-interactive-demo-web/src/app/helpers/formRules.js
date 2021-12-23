import moment from 'moment';

export const futureDate = (value, cb, compareTo) => {
  const isAfter = moment(value).isSameOrAfter(compareTo);

  if (isAfter) {
    cb('error');
  } else {
    cb();
  }
};

export const noEmptyInput = message => ({
  required: true,
  whitespace: true,
  message
});
