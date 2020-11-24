import React, { useState } from 'react';
import PropTypes from 'prop-types';
import BulkImportResult from './Organisms/BulkImportResults';
import BulkImportSteps from './Organisms/BulkImportSteps';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';

const BulkImport = ({
  onUpload,
  cancelImport,
  showGroupSelection,
  getTargets,
  useCase,
  headersMapping,
  loading
}) => {
  const [fileData, setFileData] = useState();
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [skipGroupsAssignment, setSkipGroupsAssignment] = useState();
  const [results, setResults] = useState();

  const stepsProps = {
    fileData,
    setFileData,
    selectedGroups,
    setSelectedGroups,
    skipGroupsAssignment,
    setSkipGroupsAssignment,
    showGroupSelection,
    onFinish: () => onUpload(fileData, skipGroupsAssignment ? [] : selectedGroups, setResults),
    cancelImport,
    getTargets,
    useCase,
    headersMapping,
    loading
  };

  const handleReturnToUploadStep = () => {
    setFileData(null);
    setResults(null);
  };

  const onSteps = !results;

  return onSteps ? (
    <BulkImportSteps {...stepsProps} />
  ) : (
    <BulkImportResult
      {...results}
      returnToUploadStep={handleReturnToUploadStep}
      useCase={useCase}
    />
  );
};

BulkImport.defaultProps = {
  cancelImport: () => {},
  showGroupSelection: false,
  getTargets: null,
  loading: false
};

BulkImport.propTypes = {
  getTargets: PropTypes.func,
  onUpload: PropTypes.func.isRequired,
  cancelImport: PropTypes.func,
  showGroupSelection: PropTypes.bool,
  loading: PropTypes.bool,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired
};

export default BulkImport;
