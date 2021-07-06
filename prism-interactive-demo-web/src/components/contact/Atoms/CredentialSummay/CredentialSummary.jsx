import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';

const CredentialSummary = ({ credentialText, credentialIcon }) => (
  <div className="CredentialSummary">
    <img src={credentialIcon} alt="CredentialIcons" />
    <h3>{credentialText}</h3>
  </div>
);

CredentialSummary.propTypes = {
  credentialText: PropTypes.string.isRequired,
  credentialIcon: PropTypes.string.isRequired
};

export default CredentialSummary;
