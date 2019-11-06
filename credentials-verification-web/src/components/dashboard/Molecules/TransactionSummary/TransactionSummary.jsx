import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import calendar from '../../../../images/calendar.svg';
import Summary from '../../Atoms/Summaries/Summary';

import './_style.scss';

const TransactionSummary = ({
  credentials: { week: weeklyCredentials, month: monthlyCredentials },
  proofRequests: { week: weeklyProofRequests, month: monthlyProofRequests }
}) => {
  const { t } = useTranslation();

  const icon = {
    src: calendar,
    alt: t('dashboard.summary.calendar')
  };

  const weekType = {
    icon,
    type: t('dashboard.summary.week')
  };

  const monthType = {
    icon,
    type: t('dashboard.summary.week')
  };

  return (
    <div className="TransactionSummary">
      <h3 className="line-bottom">{t('dashboard.transactionSummary.title')}</h3>
      <div className="SummaryContent">
        <Summary
          type={weekType}
          info={t('dashboard.transactionSummary.issuedCredentials')}
          amount={weeklyCredentials}
        />
        <Summary
          type={monthType}
          info={t('dashboard.transactionSummary.issuedCredentials')}
          amount={monthlyCredentials}
        />
        <Summary
          type={weekType}
          info={t('dashboard.transactionSummary.proofRequests')}
          amount={weeklyProofRequests}
        />
        <Summary
          type={monthType}
          info={t('dashboard.transactionSummary.proofRequests')}
          amount={monthlyProofRequests}
        />
      </div>
    </div>
  );
};

TransactionSummary.defaultProps = {
  credentials: { week: 10, month: 60 },
  proofRequests: { week: 10, month: 60 }
};

const timelyShape = PropTypes.shape({
  week: PropTypes.number.isRequired,
  month: PropTypes.number.isRequired
});

TransactionSummary.propTypes = {
  credentials: timelyShape,
  proofRequests: timelyShape
};

export default TransactionSummary;
