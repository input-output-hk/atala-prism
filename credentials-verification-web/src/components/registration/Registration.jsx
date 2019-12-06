import React from 'react';
import PropTypes from 'prop-types';
import RegistrationFooter from './Molecules/Footer/RegistrationFooter';

import './_style.scss';

const Registration = ({ renderContent, next, renderFooter, previous, requiresAgreement }) => (
  <div className="RegistrationContainer">
    <div className="RegistrationContent">
      {renderContent()}
      {renderFooter && (
        <RegistrationFooter next={next} previous={previous} requiresAgreement={requiresAgreement} />
      )}
    </div>
  </div>
);

Registration.defaultProps = {
  next: null,
  previous: null
};

Registration.propTypes = {
  renderContent: PropTypes.element.isRequired,
  next: PropTypes.func,
  previous: PropTypes.func,
  renderFooter: PropTypes.bool.isRequired,
  requiresAgreement: PropTypes.bool.isRequired
};

export default Registration;
