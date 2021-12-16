import React from 'react';
import PropTypes from 'prop-types';
import {
  BULK_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT
} from '../../../helpers/constants';
import BulkImportResult from '../../bulkImport/Organisms/BulkImportResults';
import ManualImportResults from '../../manualImport/Organisms/ManualImportResults';

export const ImportResults = ({ results, importType, useCaseProps }) =>
  importType === MANUAL_IMPORT ? (
    <ManualImportResults {...results} {...useCaseProps} />
  ) : (
    <BulkImportResult {...results} {...useCaseProps} />
  );

ImportResults.propTypes = {
  results: PropTypes.oneOfType([
    PropTypes.shape({
      fileData: PropTypes.shape({
        fileObj: PropTypes.shape({ name: PropTypes.string })
      }),
      validationErrors: PropTypes.arrayOf(
        PropTypes.arrayOf({
          error: PropTypes.string,
          row: PropTypes.number,
          col: PropTypes.number
        })
      )
    }),
    PropTypes.shape({
      contactCreations: PropTypes.number,
      credentialDataImported: PropTypes.number,
      continueCallback: PropTypes.func
    })
  ]).isRequired,
  importType: PropTypes.oneOf([MANUAL_IMPORT, BULK_IMPORT]).isRequired,
  useCaseProps: PropTypes.shape({
    useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
    showGroupSelection: PropTypes.bool.isRequired,
    isEmbedded: PropTypes.bool
  }).isRequired
};

export default ImportResults;
