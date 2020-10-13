import React from 'react';
import PropTypes from 'prop-types';
import OptionCard from './Molecules/OptionCard/OptionCard';
import { BULK_IMPORT, MANUAL_IMPORT } from '../../helpers/constants';
import imgBulk from '../../images/bulk-img.svg';
import imgManually from '../../images/import-manually.svg';
import './_style.scss';

const ImportTypeSelector = ({ selected, onSelect }) => (
  <div className="OptionCardsContainer">
    <OptionCard
      option={BULK_IMPORT}
      isSelected={selected === BULK_IMPORT}
      onSelect={onSelect}
      img={imgBulk}
    />
    <OptionCard
      option={MANUAL_IMPORT}
      isSelected={selected === MANUAL_IMPORT}
      onSelect={onSelect}
      img={imgManually}
    />
  </div>
);

ImportTypeSelector.propTypes = {
  selected: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired
};

export default ImportTypeSelector;
