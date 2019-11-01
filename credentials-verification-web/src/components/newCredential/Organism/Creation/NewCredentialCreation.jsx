import React from 'react';
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

export default NewCredentialCreation;
