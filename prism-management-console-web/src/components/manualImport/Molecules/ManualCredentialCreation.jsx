import React from 'react';
import CredentialCreationTable from '../Organisms/Tables/CredentialCreationTable';
import './_style.scss';

const ManualCredentialCreation = props => (
  <div className="ManualImportWrapper CredentialsImportWrapper">
    <div className="ManualImportContent">
      <CredentialCreationTable {...props} />
    </div>
  </div>
);

export default ManualCredentialCreation;
