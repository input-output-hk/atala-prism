import React, { useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import GroupCreation from './GroupCreation';
import Logger from '../../helpers/Logger';
import { withRedirector } from '../providers/withRedirector';

const GroupCreationContainer = ({ api, redirector: { redirectToGroups } }) => {
  const [groupName, setGroupName] = useState('');
  const formRef = React.createRef();

  const { t } = useTranslation();

  const saveGroup = async () => {
    try {
      await api.groupsManager.createGroup(groupName);
      message.success(t('groupCreation.success'));
      redirectToGroups();
    } catch (e) {
      message.error('Error while creating the group');
      Logger.error('groupCreation.errors.grpc', e);
    }
  };

  const formValues = { groupName };

  return (
    <GroupCreation
      createGroup={saveGroup}
      formRef={formRef}
      updateForm={setGroupName}
      formValues={formValues}
    />
  );
};

GroupCreationContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({ createGroup: PropTypes.func.isRequired }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToGroups: PropTypes.func.isRequired }).isRequired
};

export default withApi(withRedirector(GroupCreationContainer));
