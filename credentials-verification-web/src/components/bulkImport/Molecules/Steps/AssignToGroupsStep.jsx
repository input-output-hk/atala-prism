import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Select, message, Checkbox } from 'antd';
import GenericStep from './GenericStep';
import { ASSIGN_TO_GROUPS } from '../../../../helpers/constants';
import { withApi } from '../../../providers/withApi';
import Logger from '../../../../helpers/Logger';

const AssignToGroupsStep = ({
  api,
  currentStep,
  setCurrentStep,
  showGroupSelection,
  selectedGroups,
  setSelectedGroups,
  setSkipGroupsAssignment,
  disabled
}) => {
  const { t } = useTranslation();

  const { Option } = Select;

  const [groups, setGroups] = useState([]);

  useEffect(() => {
    if (showGroupSelection) {
      api.groupsManager
        .getGroups()
        .then(setGroups)
        .catch(error => {
          Logger.error('[GroupsContainer.updateGroups] Error: ', error);
          message.error(t('errors.errorGetting', { model: t('groups.title') }));
        });
    }
  }, []);

  const toggleSkipStep = e => setSkipGroupsAssignment(e.target.checked);

  const props = {
    step: ASSIGN_TO_GROUPS,
    currentStep,
    title: t('bulkImport.assignToGroups.title'),
    info: t('bulkImport.assignToGroups.info'),
    actions: (
      <div className="InputContainer">
        <Select
          mode="tags"
          style={{ width: '100%' }}
          value={selectedGroups}
          placeholder={t('bulkImport.assignToGroups.placeholder')}
          onChange={setSelectedGroups}
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
  selectedGroups: [],
  setSelectedGroups: () => {},
  setSkipGroupsAssignment: () => {},
  disabled: false
};

AssignToGroupsStep.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  currentStep: PropTypes.number.isRequired,
  setCurrentStep: PropTypes.func.isRequired,
  showGroupSelection: PropTypes.bool,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func,
  setSkipGroupsAssignment: PropTypes.func,
  disabled: PropTypes.bool
};

export default withApi(AssignToGroupsStep);
