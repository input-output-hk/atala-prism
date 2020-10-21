import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
  BULK_IMPORT,
  MANUAL_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../helpers/constants';
import ImportTypeSelectionContainer from '../ImportTypeSelection/ImportTypeSelectionContainer';
import BulkImport from '../bulkImport/BulkImport';
import { aoaToObjects } from '../../helpers/fileHelpers';
import UnderContsructionMessage from '../common/Atoms/UnderContsructionMessage/UnderContsructionMessage';

const showGroupSelection = {
  [IMPORT_CONTACTS]: true,
  [IMPORT_CREDENTIALS_DATA]: false
};

const isEmbedded = {
  [IMPORT_CONTACTS]: false,
  [IMPORT_CREDENTIALS_DATA]: true
};

const ImportDataContainer = ({ getTargets, bulkValidator, onFinish, onCancel, useCase }) => {
  const [selection, setSelection] = useState();

  const resetSelection = () => setSelection();

  const handleBulkImport = (fileData, selectedGroups, setResults) => {
    const { dataObjects, containsErrors, validationErrors } = parseFile(fileData, bulkValidator);
    if (containsErrors) setResults({ fileData, validationErrors });
    else onFinish(dataObjects, selectedGroups, setResults);
  };

  const parseFile = (fileData, validator) => {
    const inputHeaders = fileData.data[0];
    const dataObjects = aoaToObjects(fileData.data);

    const { containsErrors, validationErrors } = validator(dataObjects, inputHeaders);

    return {
      dataObjects,
      containsErrors,
      validationErrors
    };
  };

  switch (selection) {
    case BULK_IMPORT: {
      return (
        <BulkImport
          onUpload={handleBulkImport}
          cancelImport={resetSelection}
          getTargets={getTargets}
          showGroupSelection={showGroupSelection[useCase]}
          useCase={useCase}
        />
      );
    }
    case MANUAL_IMPORT: {
      return <UnderContsructionMessage goBack={() => setSelection()} />;
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
  getTargets: null,
  bulkValidator: null,
  useCase: IMPORT_CONTACTS
};

ImportDataContainer.propTypes = {
  getTargets: PropTypes.func,
  bulkValidator: PropTypes.func,
  onFinish: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default ImportDataContainer;
