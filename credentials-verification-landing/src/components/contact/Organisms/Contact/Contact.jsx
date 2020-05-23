import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import jsonp from 'jsonp';
import queryString from 'query-string';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../../../providers/withApi';
import ContactForm from '../ContactInformation/ContactInformation';
import { config } from '../../../../APIs/configs';

const { mailchimpURL, mailchimpU, mailchimpID } = config;

const Contact = () => {
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

          console.log('fields=', fullName, email, formMessage, consent);
          subscribeToNewsLetter({ EMAIL: email, FULLNAME: fullName, MESSAGE: formMessage });
        }
      );
  };

  const subscribeToNewsLetter = formData => {
    jsonp(
      `${mailchimpURL}/subscribe/post-json?u=${mailchimpU}&amp;id=${mailchimpID}&${queryString.stringify(
        formData
      )}`,
      { param: 'c' },
      (err, data) => {
        console.log('err:', err);
        console.log('data:', data);
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

export default Contact;
