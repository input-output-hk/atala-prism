import React from 'react';
import PropTypes from 'prop-types';
import TypeCard from '../../Molecules/TypeCard/TypeCard';
import { credentialTypesShape, templateCategoryShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const ENABLED_STATE = 1;

const TypeSelection = ({ credentialTypes, templateCategories, selectedType, onTypeSelection }) => {
  const getCategoryIcon = id => templateCategories.find(c => c.id === id)?.logo;

  return (
    <div className="TypeSelectionWrapper">
      <div className="TypeSelectionContainer">
        <div className="TypeSelection">
          {!credentialTypes.length ? (
            <SimpleLoading size="md" />
          ) : (
            credentialTypes
              .filter(({ state }) => state === ENABLED_STATE)
              .map(ct => (
                <TypeCard
                  credentialType={ct}
                  typeKey={ct.id}
                  key={ct.id}
                  isSelected={selectedType === ct.id}
                  onClick={onTypeSelection}
                  logo={getCategoryIcon(ct.category)}
                  sampleImage={ct.sampleImage}
                />
              ))
          )}
        </div>
      </div>
    </div>
  );
};

TypeSelection.defaultProps = {
  selectedType: ''
};

TypeSelection.propTypes = {
  credentialTypes: credentialTypesShape.isRequired,
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  selectedType: PropTypes.string,
  onTypeSelection: PropTypes.func.isRequired
};

export default TypeSelection;
