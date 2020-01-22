import React from 'react';
import PropTypes from 'prop-types';
import RegistrationFooter from './Molecules/Footer/RegistrationFooter';

import './_style.scss';
import LanguageSelector from '../common/Molecules/LanguageSelector/LanguageSelector';

const Registration = ({ renderContent, footerProps, renderFooter }) => (
  <div className="RegistrationContainer">
    <div className="LangSelector">
      <LanguageSelector />
    </div>
    <div className="RegistrationContent">
      {renderContent()}
      {renderFooter && <RegistrationFooter {...footerProps} />}
    </div>
  </div>
);

Registration.propTypes = {
  renderContent: PropTypes.element.isRequired,
  footerProps: PropTypes.shape({
    next: PropTypes.func,
    previous: PropTypes.func,
    requiresAgreement: PropTypes.bool,
    renderFooter: PropTypes.bool,
    disabled: PropTypes.bool
  }).isRequired,
  renderFooter: PropTypes.bool.isRequired
};

export default Registration;
