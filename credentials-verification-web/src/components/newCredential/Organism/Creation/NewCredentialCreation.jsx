import React from 'react';
import PropTypes from 'prop-types';
import TemplateForm from '../TemplateForm/TemplateForm';
import ExampleCredential from '../../Atom/ExampleCredential/ExampleCredential';

import './_style.scss';

const NewCredentialCreation = ({ savePicture, formRef, credentialValues }) => (
  <div className="NewCredentialCreation">
    <div className="CredentialTemplateContainer">
      <ExampleCredential />
    </div>
    <div className="CredentialFormContainer">
      <TemplateForm savePicture={savePicture} credentialValues={credentialValues} ref={formRef} />
    </div>
  </div>
);

NewCredentialCreation.defaultProps = {
  credentialValues: {}
};

NewCredentialCreation.propTypes = {
  savePicture: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  credentialValues: PropTypes.shape()
};

export default NewCredentialCreation;
