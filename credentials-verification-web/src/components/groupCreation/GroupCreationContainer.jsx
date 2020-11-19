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
  const [members, setMembers] = useState([]);
  const formRef = React.createRef();
  const { t } = useTranslation();
  const formValues = { groupName };

  const saveGroup = async () => {
    try {
      const newGroup = await api.groupsManager.createGroup(groupName);
      await api.groupsManager.updateGroup(newGroup.id, members);
      message.success(t('groupCreation.success'));
      redirectToGroups();
    } catch (e) {
      message.error(t('groupCreation.errors.grpc'));
      Logger.error('groupCreation.errors.grpc', e);
    }
  };

  return (
    <GroupCreation
      isIssuer={() => api.wallet.isIssuer()}
      createGroup={saveGroup}
      formRef={formRef}
      updateForm={setGroupName}
      updateMembers={setMembers}
      formValues={formValues}
    />
  );
};

GroupCreationContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      createGroup: PropTypes.func.isRequired,
      updateGroup: PropTypes.func.isRequired
    }).isRequired,
    contactsManager: PropTypes.shape({ getContacts: PropTypes.func.isRequired }).isRequired,
    wallet: PropTypes.shape({ isIssuer: PropTypes.func, signCredentials: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({ redirectToGroups: PropTypes.func.isRequired }).isRequired
};

export default withApi(withRedirector(GroupCreationContainer));
