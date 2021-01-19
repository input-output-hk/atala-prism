import React from 'react';
import PropTypes from 'prop-types';
import { withRedirector } from '../../providers/withRedirector';
import SuccessPage from '../../common/Molecules/SuccessPage/SuccessPage';
import {
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT
} from '../../../helpers/constants';

const ManualImportResults = ({ contactCreations, useCase }) => {
  const successfulUpdates = {
    contactCreations,
    useCase,
    importType: MANUAL_IMPORT
  };

  return <SuccessPage {...successfulUpdates} />;
};

ManualImportResults.propTypes = {
  contactCreations: PropTypes.number.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default withRedirector(ManualImportResults);
