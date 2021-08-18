import React from 'react';
import PropTypes from 'prop-types';
import SuccessPage from '../../common/Molecules/SuccessPage/SuccessPage';
import { MANUAL_IMPORT } from '../../../helpers/constants';
import { importUseCasePropType } from '../../../helpers/propShapes';

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
  continueCallback: PropTypes.func.isRequired,
  useCase: importUseCasePropType.isRequired
};

export default ManualImportResults;
