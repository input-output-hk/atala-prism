import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import congratsIcon from '../../../../images/button_congrats.svg';
import alertIcon from '../../../../images/icon_alert.svg';

import './_style.scss';

const ContactButton = ({ toContactForm }) => {
  const { t } = useTranslation();

  return (
    <div className="ContactFormButtonContainer">
      <button className="ContactFormButton" type="button" onClick={toContactForm}>
        <img src={congratsIcon} alt="button congrats" className="ContactFormButtonImg" />
        <div className="TitleContainer">
          <h4>
            <strong>{t('credential.contactFormButton.congrats')}</strong>
          </h4>
        </div>
        <div className="ContactFormButtonContent">
          <h4>{t('credential.contactFormButton.congrats2')}</h4>
          <p>{t('credential.contactFormButton.knowMore')}</p>
          <p>{t('credential.contactFormButton.getInTouch')}</p>
        </div>
      </button>
      <div className="DisclaimerMessage">
      <img src={alertIcon} alt="alert message" className="AlertIcon" />
        <p>{t('credential.contactFormButton.disclaimerMessage')}</p>
      </div>
    </div>
  );
};

ContactButton.propTypes = {
  toContactForm: PropTypes.func.isRequired
};

export default ContactButton;
