import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getCurrentLanguage } from './languageUtils';
import { LONG_DATE_FORMAT, DEFAULT_DATE_FORMAT } from './constants';
import { Date } from '../protos/common_models_pb';

const completeDateFormatter = (date, format = DEFAULT_DATE_FORMAT) => {
  const lang = getCurrentLanguage();

  try {
    return moment
      .utc()
      .year(date.year)
      .month(date.month - 1) // because Months are zero indexed on moment js
      .date(date.day)
      .locale(lang)
      .format(format);
  } catch (err) {
    return '-';
  }
};

const completeFrontendDateFormatter = (date, format = DEFAULT_DATE_FORMAT) => {
  const lang = getCurrentLanguage();
  moment.locale(lang);
  // The dates come from the backend as unix timestamp with miliseconds
  return moment.unix(date / 1000).format(format);
};

// TODO: Replace most of the code by calling fromMomentToProtoDateFormatter
export const fromUnixToProtoDateFormatter = date => {
  const lang = getCurrentLanguage();
  const dateAsNumberArray = moment(date)
    .locale(lang)
    .format('L')
    .split('/')
    .map(Number);

  let year;
  let month;
  let day;

  if (lang === 'en') {
    [month, day, year] = dateAsNumberArray;
  } else {
    [day, month, year] = dateAsNumberArray;
  }

  return {
    year,
    month,
    day
  };
};

export const fromMomentToProtoDateFormatter = date => {
  const year = date.year();
  const month = date.month() + 1; // moment js month starts in 0 while the proto date starts in 1
  const day = date.date();
  return {
    year,
    month,
    day
  };
};

export const dateFormat = date => moment(date, DEFAULT_DATE_FORMAT);

export const getProtoDate = date => {
  const formattedDate = fromMomentToProtoDateFormatter(dateFormat(date));
  const newDate = new Date();
  newDate.setYear(formattedDate.year);
  newDate.setMonth(formattedDate.month);
  newDate.setDay(formattedDate.day);
  return newDate;
};

export const backendDateFormat = unixDate =>
  // backend gives dates as timestamp expressed in seconds, moment takes it as milliseconds
  moment(unixDate * 1000).format(DEFAULT_DATE_FORMAT);

export const contactBackendDateFormat = unixDate =>
  // same as backendDateFormat but w/o *1000 due to an inconsistency on the timestamp formats
  moment(unixDate).format(DEFAULT_DATE_FORMAT);

export const backendDateFormatter = () => date => completeDateFormatter(date);
export const frontendDateFormatter = format => date => completeFrontendDateFormatter(date, format);
export const simpleMomentFormatter = date => date.format('L');

export const longDateFormatter = date => {
  const lang = getCurrentLanguage();
  return moment(date)
    .locale(lang)
    .format(LONG_DATE_FORMAT);
};
export const shortDateFormatter = frontendDateFormatter();
export const shortBackendDateFormatter = backendDateFormatter();
export const dayMonthYearFormatter = frontendDateFormatter();
export const dayMonthYearBackendFormatter = backendDateFormatter();

export const monthDayFormat = frontendDateFormatter('MMM DD');

export const dateAsUnix = date => (date ? moment(date).unix() : 0);
