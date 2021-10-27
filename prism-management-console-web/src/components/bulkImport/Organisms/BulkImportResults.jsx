import React from 'react';
import PropTypes from 'prop-types';
import BulkImportErrorLog from '../Molecules/Results/BulkImportErrorLog';
import SuccessPage from '../../common/Molecules/SuccessPage/SuccessPage';
import { BULK_IMPORT } from '../../../helpers/constants';
import { importUseCasePropType } from '../../../helpers/propShapes';

const BulkImportResults = ({
  fileData,
  validationErrors,
  contactCreations,
  credentialDataImported,
  continueCallback,
  useCase
}) => {
  const errorProps = {
    fileData,
    validationErrors
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

BulkImportResults.defaultProps = {
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  validationErrors: null,
  contactCreations: 0,
  credentialDataImported: 0,
  continueCallback: null
};

BulkImportResults.propTypes = {
  fileData: PropTypes.shape({ data: PropTypes.string }).isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  validationErrors: PropTypes.arrayOf(
    PropTypes.arrayOf(PropTypes.shape({ error: PropTypes.string }))
  ),
  contactCreations: PropTypes.number,
  credentialDataImported: PropTypes.number,
  continueCallback: PropTypes.func,
  useCase: importUseCasePropType.isRequired
};

export default BulkImportResults;
