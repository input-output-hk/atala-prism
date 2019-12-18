import React, { useEffect, useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import Settings from './Settings';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';

const SettingsContainer = ({ api: { getSettings, editSettings } }) => {
  const { t } = useTranslation();

  const [studentPay, setStudentPay] = useState(false);
  const [amount, setAmount] = useState('');

  useEffect(() => {
    getSettings()
      .then(({ studentPay: currentPay, amount: currentAmount }) => {
        setStudentPay(currentPay);
        setAmount(currentAmount);
      })
      .catch(error => {
        Logger.error('[SettingsContainer.getSettings]', error);
        message.error(t('errors.errorGetting', { model: t('settings.title') }));
      });
  }, []);

  const handleSettingsSave = () => {
    const invalidAmount = !amount || amount.charAt(amount.length - 1) === '.';

    if (studentPay && invalidAmount) {
      message.error(t('errors.invalidSettings'));
      return;
    }

    const amountToSave = studentPay ? amount : 0;

    editSettings({ studentPay, amount: amountToSave })
      .then(() => message.success(t('settings.success')))
      .catch(error => {
        Logger.error('[SettingsContainer.editSettings]', error);
        message.error(t('errors.updateSettings'));
      });
  };

  const toggleStudentPay = () => setStudentPay(!studentPay);

  return (
    <Settings
      saveSettings={handleSettingsSave}
      studentPay={studentPay}
      toggleStudentPay={toggleStudentPay}
      amount={amount}
      setAmount={setAmount}
    />
  );
};

Settings.propTypes = {
  api: PropTypes.shape({ getSettings: PropTypes.func, editSettings: PropTypes.func }).isRequired
};

export default withApi(SettingsContainer);
