import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { CheckCircleFilled, CloseCircleFilled, LoadingOutlined } from '@ant-design/icons';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import Logger from '../../../../helpers/Logger';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { withApi } from '../../../providers/withApi';
import { useDebounce } from '../../../../hooks/useDebounce';
import { colors } from '../../../../helpers/colors';
import { GROUP_NAME_STATES } from '../../../../helpers/constants';
import GroupForm from './GroupForm';

import './_style.scss';
import { refPropShape } from '../../../../helpers/propShapes';

const GroupName = ({
  api,
  updateForm,
  formValues,
  formRef,
  nameState,
  setNameState,
  className
}) => {
  const { t } = useTranslation();

  const groupExists = async value => {
    try {
      const groups = await api.groupsManager.getAllGroups();
      if (exactValueExists(groups, value, 'name')) {
        setNameState(GROUP_NAME_STATES.failed);
      } else {
        setNameState(GROUP_NAME_STATES.possible);
      }
    } catch (error) {
      setNameState(GROUP_NAME_STATES.failed);
      message.error(t('groupCreation.errors.gettingGroups'));
      Logger.error('groupCreation.errors.gettingGroups', error);
    }
  };

  const checkIfGroupExists = useDebounce(groupExists);

  useEffect(() => {
    if (nameState === GROUP_NAME_STATES.initial && formValues?.groupName) {
      setNameState(GROUP_NAME_STATES.loading);
      checkIfGroupExists(formValues.groupName);
    }
  }, [nameState, checkIfGroupExists, formValues, setNameState]);

  const handleUpdateForm = value => {
    updateForm(value);
    if (value) {
      setNameState(GROUP_NAME_STATES.loading);
      checkIfGroupExists(value);
    } else {
      setNameState(GROUP_NAME_STATES.initial);
    }
  };

  const renderNameState = () => {
    switch (nameState) {
      case GROUP_NAME_STATES.loading:
        return <LoadingOutlined />;
      case GROUP_NAME_STATES.failed:
        return <CloseCircleFilled style={{ color: colors.error }} />;
      case GROUP_NAME_STATES.possible:
        return <CheckCircleFilled style={{ color: colors.success }} />;
      default:
        return null;
    }
  };

  return (
    <div className={`ai-center mb-3 ${className}`}>
      <div className="FormContainer">
        <GroupForm ref={formRef} updateForm={handleUpdateForm} formValues={formValues} />
      </div>
      <div>{renderNameState()}</div>
    </div>
  );
};

GroupName.defaultProps = {
  className: '',
  nameState: GROUP_NAME_STATES.initial
};

GroupName.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getAllGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  className: PropTypes.string,
  formRef: refPropShape.isRequired,
  updateForm: PropTypes.func.isRequired,
  nameState: PropTypes.oneOf(Object.values(GROUP_NAME_STATES)),
  setNameState: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired
};

export default withApi(GroupName);
