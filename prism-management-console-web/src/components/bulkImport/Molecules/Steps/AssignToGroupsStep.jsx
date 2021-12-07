import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Select, message, Checkbox } from 'antd';
import { useApi } from '../../../../hooks/useApi';
import GenericStep from './GenericStep';
import { ASSIGN_TO_GROUPS } from '../../../../helpers/constants';
import Logger from '../../../../helpers/Logger';

const AssignToGroupsStep = ({
  currentStep,
  setCurrentStep,
  showGroupSelection,
  selectedGroupIds,
  setSelectedGroupIds,
  setSkipGroupsAssignment,
  disabled
}) => {
  const { t } = useTranslation();
  const { groupsManager } = useApi();

  const { Option } = Select;

  const [groups, setGroups] = useState([]);

  useEffect(() => {
    if (showGroupSelection) {
      groupsManager
        .getGroups({})
        .then(({ groupsList }) => setGroups(groupsList))
        .catch(error => {
          Logger.error('[GroupsContainer.updateGroups] Error: ', error);
          message.error(t('errors.errorGetting', { model: t('groups.title') }));
        });
    }
  }, [showGroupSelection, groupsManager, t]);

  const toggleSkipStep = e => setSkipGroupsAssignment(e.target.checked);

  const props = {
    step: ASSIGN_TO_GROUPS,
    currentStep,
    title: t('bulkImport.assignToGroups.title'),
    info: t('bulkImport.assignToGroups.info'),
    actions: (
      <div className="MultiSelectContainer">
        <Select
          mode="tags"
          style={{ width: '100%' }}
          value={selectedGroupIds}
          placeholder={t('bulkImport.assignToGroups.placeholder')}
          onChange={setSelectedGroupIds}
          disabled={currentStep !== ASSIGN_TO_GROUPS}
        >
          {groups.map(({ name }) => (
            <Option key={name}>{name}</Option>
          ))}
        </Select>
        <div>
          <Checkbox onChange={toggleSkipStep} disabled={currentStep !== ASSIGN_TO_GROUPS}>
            {t('bulkImport.assignToGroups.skipStepText')}
          </Checkbox>
        </div>
      </div>
    ),
    setCurrentStep: !disabled ? setCurrentStep : undefined,
    disabled
  };

  return <GenericStep {...props} />;
};

AssignToGroupsStep.defaultProps = {
  showGroupSelection: false,
  selectedGroupIds: [],
  disabled: false
};

AssignToGroupsStep.propTypes = {
  currentStep: PropTypes.number.isRequired,
  setCurrentStep: PropTypes.func.isRequired,
  showGroupSelection: PropTypes.bool,
  selectedGroupIds: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroupIds: PropTypes.func.isRequired,
  setSkipGroupsAssignment: PropTypes.func.isRequired,
  disabled: PropTypes.bool
};

export default AssignToGroupsStep;
