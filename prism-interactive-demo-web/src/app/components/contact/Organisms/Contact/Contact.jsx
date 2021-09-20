import React, { useRef, useState } from 'react';
import jsonp from 'jsonp';
import queryString from 'query-string';
import { message } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import firebase from 'gatsby-plugin-firebase';
import ContactForm from '../ContactInformation/ContactInformation';
import { config } from '../../../../APIs/configs';
import { CONTACT_US_EVENT } from '../../../../../helpers/constants';

const { mailchimpURL, mailchimpU, mailchimpID } = config;

const Contact = () => {
  const { t } = useTranslation();

  const contactInfoRef = useRef();
  const [consent, setConsent] = useState(false);

  const submitForm = () => {
    const form = contactInfoRef.current.getForm();
    form.validateFields(
      ['fullName', 'email', 'inquiryType', 'formMessage'],
      (errors, { fullName, email, inquiryType, formMessage }) => {
        if (!consent) return message.error(t('errors.consent'));
        if (errors) return;

        const atIndex = email.indexOf('@');
        const randomNumber = Math.floor(Math.random() * 1000);
        const finalEmail = `${email.slice(0, atIndex)}+${randomNumber}${email.slice(atIndex)}`;

        subscribeToNewsLetter({
          EMAIL: finalEmail,
          FULLNAME: fullName,
          INQUIRY: inquiryType,
          MESSAGE: formMessage
        });
        form.resetFields();
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
        console.log('err:', data);
        if (err) return message.error(t('contact.unexpectedError'));
        if (data) {
          firebase.analytics().logEvent(CONTACT_US_EVENT);
          data.result === 'success'
            ? message.success(t('contact.mail.success'))
            : message.error(t('contact.mail.error'));
        }
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
