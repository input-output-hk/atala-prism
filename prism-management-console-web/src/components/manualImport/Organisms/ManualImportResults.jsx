import React from 'react';
import PropTypes from 'prop-types';
import SuccessPage from '../../common/Molecules/SuccessPage/SuccessPage';
import {
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT
} from '../../../helpers/constants';

const ManualImportResults = ({
  contactCreations,
  credentialDataImported,
  continueCallback,
  useCase
}) => {
  const successfulUpdates = {
    contactCreations,
    credentialDataImported,
    useCase,
    importType: MANUAL_IMPORT,
    continueCallback
  };

  return <SuccessPage {...successfulUpdates} />;
};

ManualImportResults.defaultProps = {
  contactCreations: 0,
  credentialDataImported: 0
};

ManualImportResults.propTypes = {
  contactCreations: PropTypes.number,
  credentialDataImported: PropTypes.number,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default ManualImportResults;
