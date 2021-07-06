import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import calendar from '../../../../images/redCalendar.svg';
import Summary from './Summary';
import { monthDayFormat } from '../../../../helpers/formatters';

const YearSummary = ({ year, amount }) => {
  const { t } = useTranslation();

  const props = {
    type: {
      icon: { src: calendar, alt: t('dashboard.summary.calendar') },
      type: t('dashboard.summary.year')
    },
    info: monthDayFormat(year),
    amount
  };

  return <Summary {...props} />;
};

YearSummary.propTypes = {
  year: PropTypes.instanceOf(moment).isRequired,
  amount: PropTypes.number.isRequired
};

export default YearSummary;
