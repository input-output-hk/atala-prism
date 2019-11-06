import React from 'react';
import { Card } from 'antd';
import PropTypes from 'prop-types';
import RegistrationFooter from './Molecules/Footer/RegistrationFooter';

const Registration = ({
  renderContent,
  next,
  renderFooter,
  accepted,
  toggleAccept,
  previous,
  documentToAccept
}) => (
  <Card>
    {renderContent()}
    {renderFooter && (
      <RegistrationFooter
        next={next}
        previous={previous}
        toggleAccept={toggleAccept}
        accepted={accepted}
        documentToAccept={documentToAccept}
      />
    )}
  </Card>
);

Registration.defaultProps = {
  next: null,
  previous: null,
  toggleAccept: null,
  documentToAccept: '',
  accepted: false
};

Registration.propTypes = {
  renderContent: PropTypes.element.isRequired,
  next: PropTypes.func,
  previous: PropTypes.func,
  toggleAccept: PropTypes.func,
  documentToAccept: PropTypes.string,
  accepted: PropTypes.bool
};

export default Registration;
