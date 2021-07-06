import React from 'react';
import PropTypes from 'prop-types';
import ImportTypeSelector from './ImportTypeSelector';
import {
  BULK_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT
} from '../../helpers/constants';

import './_style.scss';

const ImportTypeSelectionContainer = ({
  selectedMethod,
  setSelectedMethod,
  isEmbedded,
  useCase,
  hasSelectedRecipients
}) => (
  <div className={`ImportWrapper ${isEmbedded ? 'EmbeddedImportWrapper' : ''}`}>
    <ImportTypeSelector
      selected={selectedMethod}
      onSelect={setSelectedMethod}
      useCase={useCase}
      disableManual={!hasSelectedRecipients}
    />
  </div>
);

ImportTypeSelectionContainer.defaultProps = {
  isEmbedded: false,
  hasSelectedRecipients: true,
  selectedMethod: null
};

ImportTypeSelectionContainer.propTypes = {
  selectedMethod: PropTypes.oneOf([BULK_IMPORT, MANUAL_IMPORT]),
  setSelectedMethod: PropTypes.func.isRequired,
  isEmbedded: PropTypes.bool,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  hasSelectedRecipients: PropTypes.bool
};

export default ImportTypeSelectionContainer;
