import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { arrayOfArraysToObjects } from '../../helpers/fileHelpers';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';
import ImportTypeSelectionContainer from '../ImportTypeSelection/ImportTypeSelectionContainer';
import BulkImport from '../bulkImport/BulkImport';
import ManualImportContainer from '../manualImport/ManualImportContainer';
import {
  filterEmptyContact,
  translateBackSpreadsheetNamesToContactKeys
} from '../../helpers/contactValidations';
import {
  BULK_IMPORT,
  MANUAL_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../helpers/constants';
import { ImportResults } from './Molecules/ImportResults';

const showGroupSelection = {
  [IMPORT_CONTACTS]: true,
  [IMPORT_CREDENTIALS_DATA]: false
};

const isEmbedded = {
  [IMPORT_CONTACTS]: false,
  [IMPORT_CREDENTIALS_DATA]: true
};

const ImportDataContainer = ({
  bulkValidator,
  onFinish,
  onCancel,
  useCase,
  recipients,
  credentialType,
  headersMapping,
  loading
}) => {
  const [selection, setSelection] = useState();
  const [results, setResults] = useState();

  const resetSelection = () => setSelection();

  const handleManualImport = (contacts, groups) => {
    const filteredContacts = contacts.filter(filterEmptyContact);
    onFinish(filteredContacts, groups, setResults);
  };

  const handleBulkImport = (fileData, selectedGroups) => {
    const { dataObjects, containsErrors, validationErrors } = parseFile(fileData, bulkValidator);
    if (containsErrors) setResults({ fileData, validationErrors });
    else {
      const translatedContacts = translateBackSpreadsheetNamesToContactKeys(
        dataObjects,
        headersMapping
      );
      onFinish(translatedContacts, selectedGroups, setResults);
    }
  };

  const parseFile = (fileData, validator) => {
    const inputHeaders = fileData.data[0];
    const dataObjects = arrayOfArraysToObjects(fileData.data);

    const { containsErrors, validationErrors } = validator(
      dataObjects,
      inputHeaders,
      headersMapping
    );

    return {
      dataObjects,
      containsErrors,
      validationErrors
    };
  };

  const handleReturnToUploadStep = () => {
    setResults(null);
  };

  const useCaseProps = {
    useCase,
    showGroupSelection: showGroupSelection[useCase],
    isEmbedded: isEmbedded[useCase]
  };

  if (results)
    return (
      <ImportResults
        results={results}
        importType={selection}
        useCaseProps={useCaseProps}
        returnToUploadStep={handleReturnToUploadStep}
      />
    );

  switch (selection) {
    case BULK_IMPORT: {
      return (
        <BulkImport
          onUpload={handleBulkImport}
          cancelImport={resetSelection}
          recipients={recipients}
          credentialType={credentialType}
          useCaseProps={useCaseProps}
          headersMapping={headersMapping}
          loading={loading}
        />
      );
    }
    case MANUAL_IMPORT: {
      return (
        <ManualImportContainer
          onSave={handleManualImport}
          cancelImport={resetSelection}
          useCaseProps={useCaseProps}
          loading={loading}
        />
      );
    }
    default: {
      return (
        <ImportTypeSelectionContainer
          onCancel={onCancel}
          onFinish={setSelection}
          isEmbedded={isEmbedded[useCase]}
          useCase={useCase}
        />
      );
    }
  }
};

ImportDataContainer.defaultProps = {
  recipients: null,
  credentialType: null,
  bulkValidator: null,
  loading: false,
  useCase: IMPORT_CONTACTS
};

ImportDataContainer.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  credentialType: PropTypes.shape(credentialTypeShape),
  bulkValidator: PropTypes.func,
  onFinish: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired
};

export default ImportDataContainer;
