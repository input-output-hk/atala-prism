import React from 'react';
import PropTypes from 'prop-types';
import OptionCard from './Molecules/OptionCard/OptionCard';
import {
  BULK_IMPORT,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  MANUAL_IMPORT
} from '../../helpers/constants';
import imgBulk from '../../images/bulk-img.svg';
import imgManually from '../../images/import-manually.svg';
import './_style.scss';

const ImportTypeSelector = ({ selected, onSelect, useCase, disableBulk, disableManual }) => (
  <div className="OptionCardsContainer">
    <OptionCard
      option={BULK_IMPORT}
      isSelected={selected === BULK_IMPORT}
      onSelect={onSelect}
      img={imgBulk}
      useCase={useCase}
      disabled={disableBulk}
    />
    <OptionCard
      option={MANUAL_IMPORT}
      isSelected={selected === MANUAL_IMPORT}
      onSelect={onSelect}
      img={imgManually}
      useCase={useCase}
      disabled={disableManual}
    />
  </div>
);

ImportTypeSelector.defaultProps = {
  selected: '',
  disableBulk: false,
  disableManual: false
};

ImportTypeSelector.propTypes = {
  selected: PropTypes.string,
  onSelect: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  disableBulk: PropTypes.bool,
  disableManual: PropTypes.bool
};

export default ImportTypeSelector;
