import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import ImportTypeSelector from './ImportTypeSelector';
import GenericFooter from '../common/Molecules/GenericFooter/GenericFooter';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import './_style.scss';

const ImportTypeSelectionContainer = ({ onFinish, onCancel, isEmbedded, useCase }) => {
  const { t } = useTranslation();
  const [selectedMethod, setSelectedMethod] = useState();

  const handleNext = () => {
    onFinish(selectedMethod);
  };

  const footerLabels = {
    previous: t(`${useCase}.buttons.previous`),
    next: t(`${useCase}.buttons.next`)
  };

  return (
    <div className={`ImportWrapper ${isEmbedded ? 'EmbeddedImportWrapper' : ''}`}>
      <div className="ContentHeader TitleAndSubtitle">
        <h2>{t(`${useCase}.importTypeSelection.title`)}</h2>
        <p>{t(`${useCase}.importTypeSelection.info`)}</p>
      </div>
      <ImportTypeSelector
        selected={selectedMethod}
        onSelect={setSelectedMethod}
        useCase={useCase}
      />
      <GenericFooter
        previous={onCancel}
        next={handleNext}
        disableNext={!selectedMethod}
        labels={footerLabels}
      />
    </div>
  );
};

ImportTypeSelectionContainer.defaultProps = {
  isEmbedded: false
};

ImportTypeSelectionContainer.propTypes = {
  onFinish: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  isEmbedded: PropTypes.bool,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default ImportTypeSelectionContainer;
