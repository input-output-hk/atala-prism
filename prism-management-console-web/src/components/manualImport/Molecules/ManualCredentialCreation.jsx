import React from 'react';
import CredentialCreationTable from '../Organisms/Tables/CredentialCreationTable';

const ManualCredentialCreation = props => (
  <div className="ManualImportWrapper">
    <div className="ManualImportContent">
      <CredentialCreationTable {...props} />
    </div>
  </div>
);

export default ManualCredentialCreation;
