import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Row, Col, message, Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import Logger from '../../../../helpers/Logger';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { withApi } from '../../../providers/withApi';
import { useDebounce } from '../../../../hooks/useDebounce';
import { colors } from '../../../../helpers/colors';
import { GROUP_NAME_STATES } from '../../../../helpers/constants';
import GroupForm from './GroupForm';

import './_style.scss';

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

  useEffect(() => {
    if (nameState === GROUP_NAME_STATES.initial && formValues?.groupName) {
      setNameState(GROUP_NAME_STATES.loading);
      checkIfGroupExists(formValues.groupName);
    }
  }, [nameState]);

  const handleUpdateForm = value => {
    updateForm(value);
    if (value) {
      setNameState(GROUP_NAME_STATES.loading);
      checkIfGroupExists(value);
    } else {
      setNameState(GROUP_NAME_STATES.initial);
    }
  };

  const groupExists = async value => {
    try {
      const groups = await api.groupsManager.getGroups();
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

  const renderNameState = () => {
    switch (nameState) {
      case GROUP_NAME_STATES.loading:
        return <Icon type="loading" />;
      case GROUP_NAME_STATES.failed:
        return <Icon type="close-circle" theme="filled" style={{ color: colors.error }} />;
      case GROUP_NAME_STATES.possible:
        return <Icon type="check-circle" theme="filled" style={{ color: colors.success }} />;
      default:
        return null;
    }
  };

  const checkIfGroupExists = useDebounce(groupExists);

  return (
    <Row type="flex" gutter={12} className={`ai-center mb-3 ${className}`}>
      <Col sm={20} md={8} className="FormContainer">
        <GroupForm ref={formRef} updateForm={handleUpdateForm} formValues={formValues} />
      </Col>
      <Col sm={2} md={2}>
        {renderNameState()}
      </Col>
    </Row>
  );
};

GroupName.defaultProps = {
  className: '',
  nameState: GROUP_NAME_STATES.initial
};

GroupName.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  className: PropTypes.string,
  formRef: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  nameState: PropTypes.oneOf(GROUP_NAME_STATES),
  setNameState: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired
};

export default withApi(GroupName);
