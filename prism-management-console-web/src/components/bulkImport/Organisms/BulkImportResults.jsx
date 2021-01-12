import React from 'react';
import PropTypes from 'prop-types';
import BulkImportErrorLog from '../Molecules/Results/BulkImportErrorLog';
import BulkImportSuccess from '../Molecules/Results/BulkImportSuccess';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../../helpers/constants';

const BulkImportResult = ({
  fileData,
  validationErrors,
  contactCreations,
  returnToUploadStep,
  credentialDataImported,
  continueCallback,
  useCase
}) => {
  const errorProps = {
    fileData,
    validationErrors,
    returnToUploadStep
  };

  const successfulUpdates = {
    contactCreations,
    credentialDataImported,
    continueCallback,
    useCase
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
  credentialDataImported: 0,
  continueCallback: null
};

BulkImportResult.propTypes = {
  fileData: PropTypes.shape({ data: PropTypes.string }).isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  validationErrors: PropTypes.arrayOf(
    PropTypes.arrayOf(PropTypes.shape({ error: PropTypes.string }))
  ),
  contactCreations: PropTypes.number,
  returnToUploadStep: PropTypes.func.isRequired,
  credentialDataImported: PropTypes.number,
  continueCallback: PropTypes.func,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default BulkImportResult;
