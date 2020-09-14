import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const RegistrationFooter = ({ next, previous, disabled, requiresAgreement }) => {
  const { t } = useTranslation();

  return (
    <div className="RegistrationFooter">
      <div className="LeftButtons">
        {previous && (
          <CustomButton
            buttonProps={{
              onClick: previous,
              className: 'theme-grey'
            }}
            buttonText={t('registration.back')}
          />
        )}
      </div>
      <div className="RightButtons">
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-primary',
            disabled
          }}
          buttonText={t(`registration.${requiresAgreement ? 'nextAndAgree' : 'next'}`)}
        />
      </div>
    </div>
  );
};

RegistrationFooter.defaultProps = {
  next: null,
  previous: null
};

RegistrationFooter.propTypes = {
  next: PropTypes.func,
  previous: PropTypes.func,
  disabled: PropTypes.bool.isRequired,
  requiresAgreement: PropTypes.bool.isRequired
};

export default RegistrationFooter;
