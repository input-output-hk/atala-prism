import React from 'react';
import { Col, Row, Icon } from 'antd';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import Contact from './Organisms/Contact/Contact';
import { LEFT } from '../../../helpers/constants';
import CustomButton from '../../../components/customButton/CustomButton';
import { withRedirector } from '../providers/withRedirector';
import SupportButton from '../common/Atoms/SupportButton/SupportButton';

import '../credentials/_style.scss';
import './_style.scss';

const ContactContainer = ({ redirector: { redirectToLanding } }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialContainer">
      <div className="LogoContent">
        <img src="/images/logo-atala-prism.svg" alt={t('atalaLogo')} />
      </div>
      <div className="CredentialStepContent">
        <Row className="CredentialsForm">
          <Col xs={24} lg={13}>
            <CustomButton
              buttonProps={{
                onClick: redirectToLanding,
                className: 'theme-link'
              }}
              icon={{ icon: <Icon type="left" />, side: LEFT }}
              buttonText={t('credential.backHome')}
            />
            <div className="TitleContent">
              <h1>{t('credential.contactInformation.title')}</h1>
              <p>{t('credential.contactInformation.congrats')}</p>
              <img
                src="/images/icon-decorative.svg"
                className="IconDecorative"
                alt={t('atalaLogo')}
              />
            </div>
            <div className="Form">
              <Contact />
            </div>
          </Col>
          <Col xs={24} lg={11} className="ImageSide">
            <img src="/images/credentials-phone-form.png" alt={t('atalaLogo')} />
          </Col>
        </Row>
      </div>
      <SupportButton />
      <div className="LogoContent">
        <img src="/images/logo-cardano.svg" alt={t('atalaLogo')} />
      </div>
    </div>
  );
};

export default withRedirector(ContactContainer);
