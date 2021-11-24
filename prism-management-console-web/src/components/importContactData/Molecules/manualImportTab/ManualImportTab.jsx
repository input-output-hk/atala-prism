import React from 'react';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import { importUseCasePropType } from '../../../../helpers/propShapes';
import ManualContactCreation from '../../../manualImport/Molecules/ManualContactCreation';
import ManualCredentialCreation from '../../../manualImport/Molecules/ManualCredentialCreation';

const ManualImportTab = ({ useCase, ...restProps }) => {
  const mockedRestProps = {
    groupsProps: {
      groups: [],
      selectedGroups: [],
      setSelectedGroups: () => {}
    },
    addEntity: () => {}
  };

  return useCase === IMPORT_CONTACTS ? (
    <ManualContactCreation {...mockedRestProps} />
  ) : (
    <ManualCredentialCreation {...restProps} />
  );
};

ManualImportTab.propTypes = {
  useCase: importUseCasePropType.isRequired
};

export default ManualImportTab;
