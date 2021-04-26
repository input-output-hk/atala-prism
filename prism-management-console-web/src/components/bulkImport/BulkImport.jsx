import React from 'react';
import PropTypes from 'prop-types';
import BulkImportSteps from './Organisms/BulkImportSteps';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';

const BulkImport = ({
  cancelImport,
  recipients,
  credentialType,
  useCaseProps,
  headersMapping,
  loading,
  fileData,
  setFileData,
  selectedGroups,
  setSelectedGroups,
  skipGroupsAssignment,
  setSkipGroupsAssignment
}) => {
  const stepsProps = {
    fileData,
    setFileData,
    selectedGroups,
    setSelectedGroups,
    skipGroupsAssignment,
    setSkipGroupsAssignment,
    cancelImport,
    recipients,
    credentialType,
    headersMapping,
    loading
  };

  return <BulkImportSteps {...stepsProps} {...useCaseProps} />;
};

BulkImport.defaultProps = {
  cancelImport: () => {},
  recipients: null,
  credentialType: null,
  loading: false
};

BulkImport.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  credentialType: PropTypes.shape(credentialTypeShape),
  onUpload: PropTypes.func.isRequired,
  cancelImport: PropTypes.func,
  loading: PropTypes.bool,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired,
  useCaseProps: PropTypes.shape({
    useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
    showGroupSelection: PropTypes.bool.isRequired,
    isEmbedded: PropTypes.bool
  }).isRequired
};

export default BulkImport;
