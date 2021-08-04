import React from 'react';
import './_style.scss';

const CredentialSummary = ({ credentialText, credentialIcon }) => (
  <div className="CredentialSummary">
    <img src={credentialIcon} alt="CredentialIcons" />
    <h3>{credentialText}</h3>
  </div>
);

export default CredentialSummary;
