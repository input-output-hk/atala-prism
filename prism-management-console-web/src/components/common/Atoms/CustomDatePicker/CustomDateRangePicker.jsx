import React from 'react';
import PropTypes from 'prop-types';
import { DatePicker } from 'antd';
import localeKa from 'moment/locale/ka';
import localeEn from 'moment/locale/en-gb';
import { getCurrentLanguage } from '../../../../helpers/languageUtils';
import { DEFAULT_DATE_FORMAT } from '../../../../helpers/constants';

import './_style.scss';

const { RangePicker } = DatePicker;

const CustomDateRangePicker = ({ placeholder, suffixIcon, onChange }) => {
  const locale = getCurrentLanguage() === 'en' ? localeEn : localeKa;

  return (
    <RangePicker
      locale={locale}
      format={DEFAULT_DATE_FORMAT}
      placeholder={placeholder}
      suffixIcon={suffixIcon}
      onChange={onChange}
    />
  );
};

CustomDateRangePicker.defaultProps = {
  suffixIcon: null
};

CustomDateRangePicker.propTypes = {
  placeholder: PropTypes.arrayOf(PropTypes.string).isRequired,
  suffixIcon: PropTypes.element,
  onChange: PropTypes.func.isRequired
};

export default CustomDateRangePicker;
