import React from 'react';
import PropTypes from 'prop-types';
import BulkImportErrorLog from '../Molecules/Results/BulkImportErrorLog';
import SuccessPage from '../../common/Molecules/SuccessPage/SuccessPage';
import { BULK_IMPORT, IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../../helpers/constants';

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
    importType: BULK_IMPORT,
    useCase
  };

  return validationErrors ? (
    <BulkImportErrorLog {...errorProps} />
  ) : (
    <SuccessPage {...successfulUpdates} />
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
