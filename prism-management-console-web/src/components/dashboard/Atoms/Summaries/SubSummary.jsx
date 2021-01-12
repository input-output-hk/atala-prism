import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import calendar from '../../../../images/calendar.svg';
import Summary from './Summary';
import { monthDayFormat } from '../../../../helpers/formatters';

const SubSummary = ({ type, from, to, amount }) => {
  const { t } = useTranslation();

  return (
    <Summary
      type={{ icon: { src: calendar, alt: t('dashboard.summary.calendar') }, type }}
      info={`${monthDayFormat(from)} - ${monthDayFormat(to)}`}
      amount={amount}
    />
  );
};

SubSummary.propTypes = {
  type: PropTypes.string.isRequired,
  from: PropTypes.instanceOf(moment).isRequired,
  to: PropTypes.instanceOf(moment).isRequired,
  amount: PropTypes.number.isRequired
};

export default SubSummary;
