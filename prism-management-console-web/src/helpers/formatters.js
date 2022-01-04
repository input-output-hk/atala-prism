import moment from 'moment';
// If the module isn't explicitly imported moment wouldn't recognize the georgian language setting
import 'moment/locale/ka';
import { getCurrentLanguage } from './languageUtils';
import { LONG_DATE_FORMAT, DEFAULT_DATE_FORMAT } from './constants';
import { Date } from '../protos/common_models_pb';

const MILLIS_IN_1_SECOND = 1000;

const completeFrontendDateFormatter = (date, format = DEFAULT_DATE_FORMAT) => {
  const lang = getCurrentLanguage();
  moment.locale(lang);
  // The dates come from the backend as unix timestamp with miliseconds
  return moment.unix(date / MILLIS_IN_1_SECOND).format(format);
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
  moment(unixDate * MILLIS_IN_1_SECOND).format(DEFAULT_DATE_FORMAT);

export const frontendDateFormatter = format => date => completeFrontendDateFormatter(date, format);

export const longDateFormatter = date => {
  const lang = getCurrentLanguage();
  return moment(date)
    .locale(lang)
    .format(LONG_DATE_FORMAT);
};
export const shortDateFormatter = frontendDateFormatter();
export const dayMonthYearFormatter = frontendDateFormatter();

export const monthDayFormat = frontendDateFormatter('MMM DD');
