import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, Icon } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import ContactInformationForm from '../ContactInformationForm/ContactInformationForm';
import './_style.scss';

const ContactInformation = ({ submit, contactInfoRef, onConsent }) => {
  const { t } = useTranslation();

  return (
    <div className="contactInformation">
      <ContactInformationForm ref={contactInfoRef} />
      <Checkbox onChange={event => onConsent(event.target.checked)}>
        {t('credential.contactInformation.consent')}
      </Checkbox>
      <hr />
      <Checkbox onChange={event => onConsent(event.target.checked)}>
        {t('credential.contactInformation.consent2')}
        <a
          href="https://legal.atalaprism.io/terms-and-conditions.html "
          target="_blank"
          rel="noopener noreferrer"
        >
          {t('credential.contactInformation.termsConditions1')}
        </a>
        {t('credential.contactInformation.termsConditions2')}
        <a
          href="https://legal.atalaprism.io/privacy-policy.html "
          target="_blank"
          rel="noopener noreferrer"
        >
          {t('credential.contactInformation.termsConditions3')}
        </a>
      </Checkbox>
      <div className="FAQContainer">
        <p>
          {t('credential.contactInformation.faq1')}{' '}
          <a href="/">{t('credential.contactInformation.faq2')}</a>{' '}
          {t('credential.contactInformation.faq3')}
        </p>
      </div>
      <CustomButton
        buttonProps={{
          onClick: submit,
          className: 'theme-primary'
        }}
        buttonText={t('credential.contactInformation.send')}
      />
    </div>
  );
};

ContactInformation.propTypes = {
  submit: PropTypes.func.isRequired,
  onConsent: PropTypes.func.isRequired,
  contactInfoRef: PropTypes.shape.isRequired
};

export default ContactInformation;
