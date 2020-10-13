import React, { useState } from 'react';
import PropTypes from 'prop-types';
import BulkImportResult from './Organisms/BulkImportResults';
import BulkImportSteps from './Organisms/BulkImportSteps';

const BulkImport = ({ onUpload, cancelImport, showGroupSelection }) => {
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
    cancelImport
  };

  const handleReturnToUploadStep = () => {
    setFileData(null);
    setResults(null);
  };

  const onSteps = !results;

  return onSteps ? (
    <BulkImportSteps {...stepsProps} />
  ) : (
    <BulkImportResult {...results} returnToUploadStep={handleReturnToUploadStep} />
  );
};

BulkImport.defaultProps = {
  cancelImport: () => {},
  showGroupSelection: false
};

BulkImport.propTypes = {
  onUpload: PropTypes.func.isRequired,
  cancelImport: PropTypes.func,
  showGroupSelection: PropTypes.bool
};

export default BulkImport;
