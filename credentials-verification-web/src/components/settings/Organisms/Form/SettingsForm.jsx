import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox, Input } from 'antd';

const numberOrDot = /^[0-9]+(\.[0-9]+)?/;
const dotCountRegex = /\./g;

const SettingsForm = ({ payForCredential, togglePayForCredential, amount, setAmount }) => {
  const { t } = useTranslation();

  const handleAmountChange = value => {
    const isNumberOrDot = numberOrDot.test(value);
    const hasZeroOrOneDot = (value.match(dotCountRegex) || []).length < 2;

    if (!value || (isNumberOrDot && hasZeroOrOneDot)) setAmount(value);
  };

  return (
    <Fragment>
      <div className="box">
        <h3>{t('settings.form.option.title')}</h3>
        <div className="flex">
          <Checkbox onChange={togglePayForCredential} checked={payForCredential} />
          <p>{t('settings.form.option.studentPay')}</p>
        </div>
      </div>
      <div className="box">
        <h3>{t('settings.form.enterAmount')}</h3>
        <label className="label">{t('settings.form.amount')}</label>
        <Input
          disabled={!payForCredential}
          value={amount}
          onChange={({ target: { value } }) => handleAmountChange(value)}
        />
      </div>
    </Fragment>
  );
};

SettingsForm.defaultProps = {
  amount: ''
};

SettingsForm.propTypes = {
  payForCredential: PropTypes.bool.isRequired,
  togglePayForCredential: PropTypes.func.isRequired,
  amount: PropTypes.string,
  setAmount: PropTypes.func.isRequired
};

export default SettingsForm;
