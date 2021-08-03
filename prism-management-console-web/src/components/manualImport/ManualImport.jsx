import React from 'react';
import PropTypes from 'prop-types';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import ManualContactCreation from './Molecules/ManualContactCreation';

import './_style.scss';
import ManualCredentialCreation from './Molecules/ManualCredentialCreation';

const ManualImport = ({ useCase, ...restProps }) =>
  useCase === IMPORT_CONTACTS ? (
    <ManualContactCreation {...restProps} />
  ) : (
    <ManualCredentialCreation {...restProps} />
  );

ManualImport.propTypes = {
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default ManualImport;
