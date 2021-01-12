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

const ImportTypeSelector = ({ selected, onSelect, useCase }) => (
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
    />
  </div>
);

ImportTypeSelector.defaultProps = {
  selected: ''
};

ImportTypeSelector.propTypes = {
  selected: PropTypes.string,
  onSelect: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired
};

export default ImportTypeSelector;
