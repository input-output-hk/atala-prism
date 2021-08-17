import React from 'react';
import { IMPORT_CONTACTS } from '../../helpers/constants';
import ManualContactCreation from './Molecules/ManualContactCreation';
import ManualCredentialCreation from './Molecules/ManualCredentialCreation';
import { importUseCasePropType } from '../../helpers/propShapes';

import './_style.scss';

const ManualImport = ({ useCase, ...restProps }) =>
  useCase === IMPORT_CONTACTS ? (
    <ManualContactCreation {...restProps} />
  ) : (
    <ManualCredentialCreation {...restProps} />
  );

ManualImport.propTypes = {
  useCase: importUseCasePropType.isRequired
};

export default ManualImport;
