import React from 'react';
import PropTypes from 'prop-types';
import TypeCard from '../../Molecules/TypeCard/TypeCard';
import { credentialTypeShape } from '../../../../helpers/propShapes';

import './_style.scss';

const TypeSelection = ({ credentialTypes, selectedType, onTypeSelection }) => (
  <div className="TypeSelectionWrapper">
    <div className="TypeSelectionContainer">
      <div className="TypeSelection">
        {Object.keys(credentialTypes)
          .filter(key => credentialTypes[key].enabled)
          .map(key => (
            <TypeCard
              credentialType={credentialTypes[key]}
              typeKey={key}
              key={key}
              isSelected={selectedType === key}
              onClick={onTypeSelection}
            />
          ))}
      </div>
    </div>
  </div>
);

TypeSelection.defaultProps = {
  selectedType: ''
};

TypeSelection.propTypes = {
  credentialTypes: PropTypes.shape(credentialTypeShape).isRequired,
  selectedType: PropTypes.string,
  onTypeSelection: PropTypes.func.isRequired
};

export default TypeSelection;
