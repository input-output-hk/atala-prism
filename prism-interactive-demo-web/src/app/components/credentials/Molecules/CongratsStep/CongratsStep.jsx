import React from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import ContactButton from '../../Atoms/ContactButton/ContactButton';
import './_style.scss';

const CongratsStep = ({ redirector: { redirectToContact } }) => {
  const { t } = useTranslation();

  return (
    <div className="CongratsStep">
      <Row>
        <Col xs={24} lg={10} className="RightSideCongrats">
          <img src="/images/all-credentials.png" alt={t('atalaLogo')} />
        </Col>
        <Col xs={24} lg={14} className="LeftSideCongrats">
          <h1>
            <strong>{t('landing.CongratsStep.congrats')}</strong>
          </h1>
          <h1>{t('landing.CongratsStep.finishProcess')}</h1>
          <ContactButton toContactForm={redirectToContact} />
        </Col>
      </Row>
    </div>
  );
};

CongratsStep.propTypes = {
  redirector: PropTypes.shape({
    redirectToContact: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(CongratsStep);
