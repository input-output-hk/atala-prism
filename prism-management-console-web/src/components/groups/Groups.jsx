import React, { useState, useEffect } from 'react';
import { PlusOutlined } from '@ant-design/icons';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import GroupsTable from './Organisms/Tables/GroupsTable';
import GroupFilters from './Molecules/Filters/GroupFilter';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import DeleteGroupModal from './Organisms/Modals/NewGroupModal/DeleteGroupModal';
import { groupShape } from '../../helpers/propShapes';
import noGroups from '../../images/noGroups.svg';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../providers/withRedirector';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { backendDateFormat } from '../../helpers/formatters';
import { filterByInclusion } from '../../helpers/filterHelpers';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../providers/SessionContext';
import CopyGroupModal from './Organisms/Modals/CopyGroupModal/CopyGroupModal';

import './_style.scss';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';

const NewGroupButton = ({ onClick }) => {
  const { t } = useTranslation();
  return (
    <CustomButton
      buttonProps={{
        onClick,
        className: 'theme-secondary'
      }}
      buttonText={t('groups.createNewGroup')}
      icon={<PlusOutlined />}
    />
  );
};

const Groups = ({
  groups,
  handleGroupDeletion,
  copyGroup,
  loading,
  redirector: { redirectToGroupCreation }
}) => {
  const { t } = useTranslation();

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  const [name, setName] = useState('');
  const [date, setDate] = useState('');

  const [groupToDelete, setGroupToDelete] = useState({});
  const [filteredGroups, setFilteredGroups] = useState([]);

  const { accountStatus } = useSession();

  const closeDeleteModal = () => setGroupToDelete({});
  const closeCopyModal = () => setIsCopyModalOpen(false);

  useEffect(() => {
    setFilteredGroups(filterGroups());
  }, [groups, name, date]);

  useEffect(() => {
    const hasValues = Object.keys(groupToDelete).length !== 0;
    setIsDeleteModalOpen(hasValues);
  }, [groupToDelete]);

  const filterGroups = () =>
    groups.filter(
      group =>
        filterByInclusion(name, group.name) &&
        (!date || backendDateFormat(group.createdat) === date)
    );

  const handleUpdateGroups = (oldGroups, newDate, newName) => {
    setName(newName);
    setDate(newDate);
  };

  const deleteModalProps = {
    toDelete: { name: groupToDelete.groupName, id: groupToDelete.id },
    open: isDeleteModalOpen,
    closeModal: closeDeleteModal,
    handleGroupDeletion,
    prefix: 'groups'
  };

  const copyModalProps = {
    open: isCopyModalOpen,
    closeModal: closeCopyModal,
    prefix: 'groups',
    group: selectedGroup,
    onSave: copyName => copyGroup(selectedGroup, copyName).then(closeCopyModal)
  };

  const onCopy = group => {
    setSelectedGroup(group);
    setIsCopyModalOpen(true);
  };

  const tableProps = {
    onCopy,
    setGroupToDelete,
    groups: filteredGroups
  };

  const newGroupButton = <NewGroupButton onClick={redirectToGroupCreation} />;

  const emptyProps = {
    photoSrc: noGroups,
    model: t('groups.title')
  };

  const renderContent = () => {
    if (loading) return <SimpleLoading size="md" />;
    if (groups.length && !filteredGroups.length) return <EmptyComponent {...emptyProps} isFilter />;
    if (groups.length) return <GroupsTable {...tableProps} groups={filteredGroups} />;
    return (
      <EmptyComponent {...emptyProps} button={accountStatus === CONFIRMED && newGroupButton} />
    );
  };

  return (
    <div className="Wrapper">
      {accountStatus === UNCONFIRMED && <WaitBanner />}
      <DeleteGroupModal {...deleteModalProps} />
      <CopyGroupModal {...copyModalProps} />
      <div className="ContentHeader">
        <h1>{t('groups.title')}</h1>
        {accountStatus === CONFIRMED && newGroupButton}
      </div>
      <GroupFilters updateGroups={handleUpdateGroups} />
      {renderContent()}
    </div>
  );
};

Groups.defaultProps = {
  groups: [],
  loading: false
};

Groups.propTypes = {
  groups: PropTypes.arrayOf(PropTypes.shape(groupShape)),
  handleGroupDeletion: PropTypes.func.isRequired,
  copyGroup: PropTypes.func.isRequired,
  updateGroups: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  loading: PropTypes.bool,
  redirector: PropTypes.shape({ redirectToGroupCreation: PropTypes.func.isRequired }).isRequired
};

NewGroupButton.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default withRedirector(Groups);
