import React from 'react';
import PropTypes from 'prop-types';
import CredentialCreationTable from '../Organisms/Tables/CredentialCreationTable';

const ManualCredentialCreation = props => (
  <div className="ManualImportWrapper">
    <div className="ManualImportContent">
      <CredentialCreationTable {...props} />
    </div>
  </div>
);

ManualCredentialCreation.propTypes = {
  // FIXME: add proptypes
};

export default ManualCredentialCreation;
