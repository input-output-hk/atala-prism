import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import Payment from './Payment';
import Logger from '../../helpers/Logger';
import { withApi } from '../providers/witApi';

const PaymentContainer = ({ api: { getCurrencies, getAmounts, getPayments } }) => {
  const { t } = useTranslation();

  const [currencies, setCurrencies] = useState([]);
  const [amounts, setAmounts] = useState([]);
  const [payments, setPayments] = useState([]);
  const [totalPayments, setTotalPayments] = useState(0);
  const [offset, setOffset] = useState(0);
  const [from, setFrom] = useState(0);
  const [to, setTo] = useState(0);
  const [payer, setPayer] = useState('');
  const [sort, setSort] = useState({ field: '', order: '' });

  const handlePaymentRequest = filters =>
    getPayments(filters)
      .then(({ payments: paymentList, paymentCount }) => {
        setPayments(paymentList);
        setTotalPayments(paymentCount);
        return getCurrencies();
      })
      .catch(error => {
        Logger.error('[PaymentContainer.getPayments] Error: ', error);
        message.error(t('errors.errorGetting', { model: t('payment.title') }));
      });

  useEffect(() => {
    handlePaymentRequest({ offset });
  }, []);

  useEffect(() => {
    handlePaymentRequest({ from, to, payer, offset, sort });
  }, [from, to, payer, offset, sort]);

  useEffect(() => {
    getCurrencies()
      .then(currenciesList => {
        setCurrencies(currenciesList);
        return getAmounts();
      })
      .catch(error => {
        Logger.error('[PaymentContainer.getCurrencies] Error: ', error);
        message.error(t('errors.errorGetting', { model: t('payment.currency.title') }));
      });
  }, []);

  useEffect(() => {
    getAmounts()
      .then(amountList => setAmounts(amountList))
      .catch(error => {
        Logger.error('[PaymentContainer.getAmounts] Error: ', error);
        message.error(t('errors.errorGetting', { model: t('payment.amount.title') }));
      });
  }, []);

  const changeSort = ({ columnKey, order }) => setSort({ field: columnKey, order });

  const updateFilter = (value, setField) => {
    setOffset(0);
    setField(value);
  };

  const paymentProps = {
    currencies,
    amounts,
    payments,
    totalPayments,
    offset,
    setOffset: value => updateFilter(value, setOffset),
    setFrom: value => updateFilter(value, setFrom),
    setTo: value => updateFilter(value, setTo),
    setPayer: value => updateFilter(value, setPayer),
    payer,
    changeSort
  };
  return <Payment {...paymentProps} />;
};

PaymentContainer.propTypes = {
  api: PropTypes.shape({
    getCurrencies: PropTypes.func,
    getAmounts: PropTypes.func,
    getPayments: PropTypes.func
  }).isRequired
};

export default withApi(PaymentContainer);
