import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import PropTypes from 'prop-types';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const VerifyFooter = ({ triedToVerify, valid, validateWords }) => {
  const { t } = useTranslation();

  return (
    <div className="VerifyFooter">
      {triedToVerify && (
        <div className={`${'VerifyContainer'} ${valid ? 'Success' : 'Failure'}`}>
          <Icon type={valid ? 'check-circle' : 'close-circle'} theme="filled" />
          <p>{t(`registration.mnemonic.validation.${valid ? 'success' : 'failure'}`)}</p>
        </div>
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
    </div>
  );
};

VerifyFooter.propTypes = {
  valid: PropTypes.bool.isRequired,
  triedToVerify: PropTypes.bool.isRequired,
  validateWords: PropTypes.func.isRequired
};

export default VerifyFooter;
