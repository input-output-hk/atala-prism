import React from 'react';
import PropTypes from 'prop-types';
import TypeCard from '../../Molecules/TypeCard/TypeCard';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const ENABLED_STATE = 1;

const TypeSelection = ({ credentialTypes, selectedType, onTypeSelection }) => (
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
              />
            ))
        )}
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
