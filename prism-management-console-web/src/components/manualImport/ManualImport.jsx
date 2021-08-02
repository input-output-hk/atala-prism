import React from 'react';
import PropTypes from 'prop-types';
import { IMPORT_CONTACTS } from '../../helpers/constants';
import ManualContactCreation from './Molecules/ManualContactCreation';

import './_style.scss';
import ManualCredentialCreation from './Molecules/ManualCredentialCreation';

const ManualImport = ({ useCase, ...restProps }) =>
  useCase === IMPORT_CONTACTS ? (
    <ManualContactCreation {...restProps} />
  ) : (
    <ManualCredentialCreation {...restProps} />
  );

ManualImport.defaultProps = {
  credentialType: {}
};

ManualImport.propTypes = {
  // FIXME: add proptypes
  // tableProps: PropTypes.shape({
  //   dataSource: PropTypes.shape(contactCreationShape)
  // }).isRequired,
  // groupsProps: PropTypes.shape({
  //   groups: PropTypes.shape(groupShape).isRequired,
  //   selectedGroups: PropTypes.func.isRequired,
  //   setSelectedGroups: PropTypes.func.isRequired
  // }).isRequired,
  // useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  // isEmbedded: PropTypes.bool.isRequired,
  // credentialType: PropTypes.shape(credentialTypeShape),
  // addEntity: PropTypes.func.isRequired
};

export default ManualImport;
