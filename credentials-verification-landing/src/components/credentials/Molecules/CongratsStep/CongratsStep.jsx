import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import ContactButton from '../../Atoms/ContactButton/ContactButton';
import './_style.scss';

const CongratsStep = ({ redirector: { redirectToContact } }) => {
  const { t } = useTranslation();

  return (
    <div className="CongratsStep">
      <h1>
        <strong>{t('credential.CongratsStep.congrats')}</strong>
      </h1>
      <ContactButton toContactForm={redirectToContact} />
    </div>
  );
};

CongratsStep.propTypes = {
  redirector: PropTypes.shape({
    redirectToContact: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(CongratsStep);
