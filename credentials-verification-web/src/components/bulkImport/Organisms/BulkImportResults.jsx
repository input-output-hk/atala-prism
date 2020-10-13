import React from 'react';
import PropTypes from 'prop-types';
import BulkImportErrorLog from '../Molecules/Results/BulkImportErrorLog';
import BulkImportSuccess from '../Molecules/Results/BulkImportSuccess';

const BulkImportResult = ({
  fileData,
  validationErrors,
  contactCreations,
  groupsCreations,
  returnToUploadStep
}) => {
  const errorProps = {
    fileData,
    validationErrors,
    returnToUploadStep
  };

  const successfulUpdates = {
    contactCreations,
    groupsCreations
  };

  return validationErrors ? (
    <BulkImportErrorLog {...errorProps} />
  ) : (
    <BulkImportSuccess {...successfulUpdates} />
  );
};

BulkImportResult.defaultProps = {
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  validationErrors: null,
  contactCreations: 0,
  groupsCreations: 0
};

BulkImportResult.propTypes = {
  fileData: PropTypes.shape({ data: PropTypes.string }).isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  validationErrors: PropTypes.arrayOf(
    PropTypes.arrayOf(PropTypes.shape({ error: PropTypes.string }))
  ),
  contactCreations: PropTypes.number,
  groupsCreations: PropTypes.number,
  returnToUploadStep: PropTypes.func.isRequired
};

export default BulkImportResult;
