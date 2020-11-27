import React from 'react';
import { DatePicker } from 'antd';
import localeKa from 'moment/locale/ka';
import localeEn from 'moment/locale/en-gb';
import { getCurrentLanguage } from '../../../../helpers/languageUtils';
import { DEFAULT_DATE_FORMAT } from '../../../../helpers/constants';

const CustomDatePicker = props => {
  const locale = getCurrentLanguage() === 'en' ? localeEn : localeKa;

  return <DatePicker locale={locale} format={DEFAULT_DATE_FORMAT} {...props} />;
};

export default CustomDatePicker;
