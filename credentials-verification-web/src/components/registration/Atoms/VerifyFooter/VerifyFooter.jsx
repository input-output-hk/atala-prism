import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const VerifyFooter = ({ triedToVerify, valid, validateWords }) => {
  const { t } = useTranslation();

  return (
    <Fragment>
      {triedToVerify && (
        <Fragment>
          <Icon type={valid ? 'check-circle' : 'close-circle'} />
          <label>{t(`registration.mnemonic.validation.${valid ? 'success' : 'failure'}`)}</label>
        </Fragment>
      )}
      {!(triedToVerify && valid) && (
        <CustomButton
          buttonProps={{
            className: 'theme-secondary',
            onClick: validateWords
          }}
          buttonText={t('registration.mnemonic.validation.verify')}
        />
      )}
    </Fragment>
  );
};

VerifyFooter.propTypes = {
  valid: PropTypes.bool.isRequired,
  triedToVerify: PropTypes.bool.isRequired,
  validateWords: PropTypes.func.isRequired
};

export default VerifyFooter;
