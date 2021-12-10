import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { message, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import TypeCard from '../../Molecules/TypeCard/TypeCard';
import {
  CREDENTIAL_TYPE_STATUSES,
  VALID_CREDENTIAL_TYPE_STATUSES
} from '../../../../helpers/constants';
import { useTemplatesByCategoryStore } from '../../../../hooks/useTemplatesPageStore';

import './_style.scss';

const TemplatesList = observer(({ selectedType, onTypeSelection }) => {
  const { t } = useTranslation();
  const { templateCategories, filteredTemplatesByCategory } = useTemplatesByCategoryStore();

  const isValidState = state => VALID_CREDENTIAL_TYPE_STATUSES.includes(state);
  const isMockedState = state => state === CREDENTIAL_TYPE_STATUSES.MOCKED;

  const showWarning = () => message.warn(t('templates.messages.customTypeWarning'));

  const validCategories = templateCategories.filter(({ state }) => isValidState(state));

  return validCategories.map(category => {
    const templatesForThisCategory = filteredTemplatesByCategory(category);
    return templatesForThisCategory.length ? (
      <div className="CredentialTemplatesContainer">
        <p className="CategoryTitle">{category.name}</p>
        <Row>
          {templatesForThisCategory.map(ct => (
            <TypeCard
              credentialType={ct}
              typeKey={ct.id}
              key={ct.id}
              isSelected={selectedType === ct.id}
              onClick={isMockedState(ct.state) ? showWarning : onTypeSelection}
              logo={ct.icon}
              sampleImage={ct.sampleImage}
            />
          ))}
        </Row>
      </div>
    ) : null;
  });
});

TemplatesList.defaultProps = {
  selectedType: ''
};

TemplatesList.propTypes = {
  selectedType: PropTypes.string,
  onTypeSelection: PropTypes.func.isRequired
};
export default TemplatesList;
