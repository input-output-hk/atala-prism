import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { paymentShape } from '../../helpers/propShapes';
import PaymentTable from './Organisms/Table/PaymentTable';
import PaymentFilter from './Molecules/Filter/PaymentFilter';

const Payment = ({
  payments,
  totalPayments,
  offset,
  setOffset,
  setFrom,
  setTo,
  setPayer,
  payer,
  changeSort
}) => {
  const { t } = useTranslation();

  const a = { setFrom, setTo, setPayer, payer };

  return (
    <Fragment>
      <div>
        <h1>{t('payment.title')}</h1>
      </div>
      <PaymentFilter {...a} />
      {payments.length ? (
        <PaymentTable
          payments={payments}
          paymentCount={totalPayments}
          offset={offset}
          setOffset={setOffset}
          changeSort={changeSort}
        />
      ) : (
        <div />
      )}
    </Fragment>
  );
};

Payment.defaultProps = {
  offset: 0
};

Payment.propTypes = {
  setFrom: PropTypes.func.isRequired,
  setTo: PropTypes.func.isRequired,
  setPayer: PropTypes.func.isRequired,
  payer: PropTypes.func.isRequired,
  payments: PropTypes.arrayOf(paymentShape).isRequired,
  totalPayments: PropTypes.number.isRequired,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired
};

export default Payment;
