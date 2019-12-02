import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col } from 'antd';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import SettingsForm from './Organisms/Form/SettingsForm';
import './_style.scss';

const Settings = ({ saveSettings, studentPay, toggleStudentPay, amount, setAmount }) => {
  const { t } = useTranslation();

  return (
    <div className="Wrapper SettingsContainer PageContainer">
      <Fragment>
        <div className="ContentHeader">
          <h1>{t('settings.title')}</h1>
        </div>
        <Col span={6}>
          <SettingsForm
            amount={amount}
            setAmount={setAmount}
            payForCredential={studentPay}
            togglePayForCredential={toggleStudentPay}
          />
          <div className="ControlButtons">
            <CustomButton
              buttonProps={{
                className: 'theme-outline',
                onClick: saveSettings
              }}
              buttonText={t('settings.save')}
            />
          </div>
        </Col>
      </Fragment>
    </div>
  );
};

Settings.defaultProps = {
  amount: ''
};

Settings.propTypes = {
  saveSettings: PropTypes.func.isRequired,
  studentPay: PropTypes.bool.isRequired,
  toggleStudentPay: PropTypes.func.isRequired,
  amount: PropTypes.string,
  setAmount: PropTypes.func.isRequired
};

export default Settings;
