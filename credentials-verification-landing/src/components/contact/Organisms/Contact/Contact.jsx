import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../../../providers/withApi';
import ContactForm from '../ContactInformation/ContactInformation';

const Contact = ({ api }) => {
  const { t } = useTranslation();

  const contactInfoRef = useRef();
  const [consent, setConsent] = useState(false);

  const submitForm = () => {
    contactInfoRef.current
      .getForm()
      .validateFields(
        ['fullName', 'email', 'formMessage'],
        (errors, { fullName, email, formMessage }) => {
          if (!consent) return message.error(t('errors.consent'));
          if (errors) return;

          // TODO integrate with Backend Service when exist
          console.log('fields=', fullName, email, formMessage, consent);
        }
      );
  };

  return (
    <ContactForm
      submit={submitForm}
      contactInfoRef={contactInfoRef}
      onConsent={value => setConsent(value)}
    />
  );
};

Contact.propTypes = {
  api: PropTypes.shape({
    getConnectionToken: PropTypes.func,
    startSubjectStatusStream: PropTypes.func,
    SetPersonalData: PropTypes.func
  }).isRequired
};

export default withApi(Contact);
