import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'antd';
import { useTranslation } from 'react-i18next';
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
      <CustomButton
        buttonProps={{
          onClick: submit,
          className: 'theme-primary',
          disabled: false
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
