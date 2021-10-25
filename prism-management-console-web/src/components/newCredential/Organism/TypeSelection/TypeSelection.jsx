import React from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import TypeCard from '../../Molecules/TypeCard/TypeCard';
import { credentialTypesShape, templateCategoryShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import {
  CREDENTIAL_TYPE_STATUSES,
  VALID_CREDENTIAL_TYPE_STATUSES
} from '../../../../helpers/constants';

import './_style.scss';

// TODO: refactor this page based on new mockups (ATA-5547)
// eslint-disable-next-line no-unused-vars
const TypeSelection = ({ credentialTypes, templateCategories, selectedType, onTypeSelection }) => {
  const { t } = useTranslation();

  const isValidState = state => VALID_CREDENTIAL_TYPE_STATUSES.includes(state);
  const isMockedState = state => state === CREDENTIAL_TYPE_STATUSES.MOCKED;

  const showWarning = () => message.warn(t('templates.messages.customTypeWarning'));

  return (
    <div className="TypeSelectionWrapper">
      <div className="TypeSelectionContainer">
        <div className="TypeSelection">
          {!credentialTypes.length ? (
            <SimpleLoading size="md" />
          ) : (
            credentialTypes
              .filter(({ state }) => isValidState(state))
              .map(ct => (
                <TypeCard
                  credentialType={ct}
                  typeKey={ct.id}
                  key={ct.id}
                  isSelected={selectedType === ct.id}
                  onClick={isMockedState(ct.state) ? showWarning : onTypeSelection}
                  logo={ct.icon}
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
