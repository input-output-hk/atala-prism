import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import OptionCard from './Molecules/OptionCard/OptionCard';
import {
  BULK_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT,
  MAX_MANUAL_IMPORT_RECIPIENTS
} from '../../helpers/constants';
import imgBulk from '../../images/bulk-img.svg';
import imgManually from '../../images/import-manually.svg';
import './_style.scss';

const ImportTypeSelector = ({ selected, onSelect, useCase, selectedRecipientsAmount }) => {
  const { t } = useTranslation();

  const isImportCredentials = useCase === IMPORT_CREDENTIALS_DATA;
  const noRecipients = !selectedRecipientsAmount;
  const tooManyRecipients = selectedRecipientsAmount > MAX_MANUAL_IMPORT_RECIPIENTS;
  const shouldDisableManual = isImportCredentials && (noRecipients || tooManyRecipients);

  const recipientsMessage = noRecipients ? 'noRecipients' : 'tooManyRecipients';
  const disabledHelp = shouldDisableManual
    ? t(`${useCase}.manualImportCard.${recipientsMessage}`)
    : '';

  return (
    <div className="OptionCardsContainer">
      <OptionCard
        option={BULK_IMPORT}
        isSelected={selected === BULK_IMPORT}
        onSelect={onSelect}
        img={imgBulk}
        useCase={useCase}
      />
      <OptionCard
        option={MANUAL_IMPORT}
        isSelected={selected === MANUAL_IMPORT}
        onSelect={onSelect}
        img={imgManually}
        useCase={useCase}
        disabled={shouldDisableManual}
        disabledHelp={disabledHelp}
      />
    </div>
  );
};

ImportTypeSelector.defaultProps = {
  selected: ''
};

ImportTypeSelector.propTypes = {
  selected: PropTypes.string,
  onSelect: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  selectedRecipientsAmount: PropTypes.number.isRequired
};

export default ImportTypeSelector;
