import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const RegistrationFooter = ({ next, previous, accepted, toggleAccept, documentToAccept }) => {
  const { t } = useTranslation();

  return (
    <div className="RegistrationFooter">
      <div className="LeftButtons">
        {toggleAccept && (
          <div className="CheckControl">
            <Checkbox onChange={toggleAccept} checked={accepted} />
            <p>
              {t('registration.accept')}
              <strong>{t(`registration.${documentToAccept}`)}</strong>
            </p>
          </div>
        )}
      </div>
      <div className="RightButtons">
        {previous && (
          <CustomButton
            buttonProps={{
              onClick: previous,
              className: 'theme-grey'
            }}
            buttonText={t('registration.back')}
          />
        )}
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-primary'
          }}
          buttonText={t('registration.next')}
        />
      </div>
    </div>
  );
};

RegistrationFooter.defaultProps = {
  next: null,
  previous: null,
  toggleAccept: null,
  documentToAccept: ''
};

RegistrationFooter.propTypes = {
  next: PropTypes.func,
  previous: PropTypes.func,
  accepted: PropTypes.bool.isRequired,
  toggleAccept: PropTypes.func,
  documentToAccept: PropTypes.string
};

export default RegistrationFooter;
