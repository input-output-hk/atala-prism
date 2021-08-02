import React from 'react';
import PropTypes from 'prop-types';
import CredentialCreationTable from '../Organisms/Tables/CredentialCreationTable';

const ManualCredentialCreation = ({ tableProps, credentialType }) => (
  <div className="ManualImportWrapper">
    <div className="ManualImportContent">
      <CredentialCreationTable tableProps={tableProps} credentialType={credentialType} />
    </div>
  </div>
);

ManualCredentialCreation.propTypes = {
  // FIXME: add proptypes
};

export default ManualCredentialCreation;
