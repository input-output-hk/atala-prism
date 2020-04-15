import React, { useContext, useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Col, Row, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import ContactForm from './Organisms/ContactInformation/ContactInformation';
import MyCredentials from './Molecules/MyCredentials/MyCredentials';
import { UserContext } from '../providers/userContext';

import '../credentials/_style.scss';
import './_style.scss';

const ContactContainer = ({ api }) => {
  const { t } = useTranslation();
  const { user } = useContext(UserContext);

  const contactInfoRef = useRef();
  const [consent, setConsent] = useState(false);

  useEffect(() => {
    if (contactInfoRef.current.getForm())
      contactInfoRef.current.getForm().setFieldsValue({
        fullName: user.firstName
      });
  }, [contactInfoRef.current]);

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
    <div className="CredentialContainer">
      <div className="LogoContent">
        <img src="images/logo-atala-prism.svg" alt={t('atalaLogo')} />
      </div>
      <div className="CredentialStepContent">
        <Row className="CredentialsForm">
          <Col xs={24} lg={13}>
            <div className="TitleContent">
              <h1>{t('credential.contactInformation.title')}</h1>
              <p>{t('credential.contactInformation.congrats')}</p>
              <img src="images/icon-decorative.svg" className="IconDecorative" alt={t('atalaLogo')} />
            </div>
            <div className="Form">
              <ContactForm
                submit={submitForm}
                contactInfoRef={contactInfoRef}
                onConsent={value => setConsent(value)}
              />
            </div>
          </Col>
          <Col xs={24} lg={11} className="ImageSide">
            <img src="images/credentials-phone-form.png" alt={t('atalaLogo')} />
          </Col>
        </Row>
      </div>
      <div className="LogoContent">
        <img src="images/logo-cardano.svg" alt={t('atalaLogo')} />
      </div>
    </div>
  );
};

ContactContainer.propTypes = {
  api: PropTypes.shape({
    getConnectionToken: PropTypes.func,
    startSubjectStatusStream: PropTypes.func,
    SetPersonalData: PropTypes.func
  }).isRequired
};

export default withApi(ContactContainer);
