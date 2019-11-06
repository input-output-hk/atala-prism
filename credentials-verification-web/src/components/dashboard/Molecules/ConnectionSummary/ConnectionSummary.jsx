import React from 'react';
import { useTranslation } from 'react-i18next';
import moment from 'moment';
import PropTypes from 'prop-types';
import SubSummary from '../../Atoms/Summaries/SubSummary';
import YearSummary from '../../Atoms/Summaries/YearSummary';
import { getBrowserLanguage } from '../../../../helpers/languageUtils';

import './_style.scss';

const ConnectionSummary = ({ weekAmount, monthAmount, yearAmount }) => {
  const { t } = useTranslation();

  moment.locale(getBrowserLanguage());
  const to = moment();
  const oneWeekAgo = moment().subtract(1, 'weeks');
  const oneMonthAgo = moment().subtract(1, 'months');
  const oneYearAgo = moment().subtract(1, 'years');

  return (
    <div className="ConnectionSummary">
      <h3 className="line-bottom">{t('dashboard.connectionSummary.title')}</h3>
      <div className="SummaryContent">
        <SubSummary
          type={t('dashboard.summary.week')}
          from={oneWeekAgo}
          to={to}
          amount={weekAmount}
        />
        <SubSummary
          type={t('dashboard.summary.month')}
          from={oneMonthAgo}
          to={to}
          amount={monthAmount}
        />
        <YearSummary year={oneYearAgo} amount={yearAmount} />
      </div>
    </div>
  );
};

ConnectionSummary.propTypes = {
  weekAmount: PropTypes.number.isRequired,
  monthAmount: PropTypes.number.isRequired,
  yearAmount: PropTypes.number.isRequired
};

export default ConnectionSummary;
