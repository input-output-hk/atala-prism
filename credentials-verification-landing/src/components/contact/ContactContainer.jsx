import React, { useContext, useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Col, Row, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import ContactForm from './Organisms/ContactInformation/ContactInformation';
import MyCredentials from './Molecules/MyCredentials/MyCredentials';
import { UserContext } from '../providers/userContext';

const ContactContainer = ({ api }) => {
  const { t } = useTranslation();
  const { user } = useContext(UserContext);

  const contactInfoRef = useRef();
  const [consent, setConsent] = useState(false);

  useEffect(() => {
    if (contactInfoRef.current.getForm())
      contactInfoRef.current.getForm().setFieldsValue({
        fullName: `${user.firstName} ${user.lastName}`
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
    <div>
      <Row>
        <Col xs={24} lg={18}>
          <h3>{t('credential.contactInformation.congrats')}</h3>
          <h1>{t('credential.contactInformation.title')}</h1>
          <h1>{t('credential.contactInformation.title2')}</h1>
          <div className="Form">
            <ContactForm
              submit={submitForm}
              contactInfoRef={contactInfoRef}
              onConsent={value => setConsent(value)}
            />
          </div>
        </Col>
        <Col xs={24} lg={6}>
          <MyCredentials />
        </Col>
      </Row>
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
