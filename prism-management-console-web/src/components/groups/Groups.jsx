import React, { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { PlusOutlined } from '@ant-design/icons';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import GroupsTable from './Organisms/Tables/GroupsTable';
import GroupFilters from './Molecules/Filters/GroupFilter';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import DeleteGroupModal from './Organisms/Modals/DeleteGroupModal/DeleteGroupModal';
import { groupShape } from '../../helpers/propShapes';
import noGroups from '../../images/noGroups.svg';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../providers/withRedirector';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import CopyGroupModal from './Organisms/Modals/CopyGroupModal/CopyGroupModal';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';

import './_style.scss';

const NewGroupButton = ({ onClick }) => {
  const { t } = useTranslation();
  return (
    <CustomButton
      buttonProps={{
        onClick,
        className: 'theme-secondary',
        icon: <PlusOutlined />
      }}
      buttonText={t('groups.createNewGroup')}
    />
  );
};

const Groups = observer(
  ({
    groups,
    handleGroupDeletion,
    copyGroup,
    loading,
    searching,
    hasMore,
    isFilter,
    setName,
    setDateRange,
    setSortingKey,
    setSortingDirection,
    sortingDirection,
    getMoreGroups,
    redirector: { redirectToGroupCreation }
  }) => {
    const { t } = useTranslation();

    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
    const [selectedGroup, setSelectedGroup] = useState(null);
    const [groupToDelete, setGroupToDelete] = useState({});

    const { accountStatus } = useSession();

    const closeDeleteModal = () => {
      setIsDeleteModalOpen(false);
      setGroupToDelete({});
    };
    const closeCopyModal = () => setIsCopyModalOpen(false);

    useEffect(() => {
      const hasValues = Object.keys(groupToDelete).length !== 0;
      setIsDeleteModalOpen(hasValues);
    }, [groupToDelete]);

    const handleConfirmedGroupDeletion = () => {
      handleGroupDeletion(groupToDelete);
      setIsDeleteModalOpen(false);
    };

    const deleteModalProps = {
      group: groupToDelete,
      open: isDeleteModalOpen,
      closeModal: closeDeleteModal,
      handleGroupDeletion: handleConfirmedGroupDeletion,
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
      groups,
      searching,
      hasMore,
      getMoreGroups
    };

    const newGroupButton = <NewGroupButton onClick={redirectToGroupCreation} />;

    const emptyProps = {
      photoSrc: noGroups,
      model: t('groups.title')
    };

    const renderContent = () => {
      if (loading) return <SimpleLoading size="md" />;
      if (groups.length) return <GroupsTable {...tableProps} groups={groups} />;
      if (isFilter) return <EmptyComponent {...emptyProps} isFilter />;
      return (
        <EmptyComponent {...emptyProps} button={accountStatus === CONFIRMED && newGroupButton} />
      );
    };

    return (
      <div className="Wrapper Groups">
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        <DeleteGroupModal {...deleteModalProps} />
        <CopyGroupModal {...copyModalProps} />
        <div className="ContentHeader">
          <div className="title">
            <h1>{t('groups.title')}</h1>
          </div>
          <div className="filterSection">
            <div className="filterContainer">
              <GroupFilters
                setName={setName}
                setDateRange={setDateRange}
                setSortingKey={setSortingKey}
                setSortingDirection={setSortingDirection}
                sortingDirection={sortingDirection}
              />
            </div>
            {accountStatus === CONFIRMED && newGroupButton}
          </div>
        </div>
        <div className="GroupContentContainer">{renderContent()}</div>
      </div>
    );
  }
);

Groups.defaultProps = {
  groups: [],
  loading: false,
  searching: false,
  hasMore: true
};

Groups.propTypes = {
  groups: PropTypes.arrayOf(PropTypes.shape(groupShape)),
  handleGroupDeletion: PropTypes.func.isRequired,
  copyGroup: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  searching: PropTypes.bool,
  hasMore: PropTypes.bool,
  isFilter: PropTypes.bool.isRequired,
  setName: PropTypes.func.isRequired,
  setDateRange: PropTypes.func.isRequired,
  setSortingKey: PropTypes.func.isRequired,
  setSortingDirection: PropTypes.func.isRequired,
  sortingDirection: PropTypes.string.isRequired,
  getMoreGroups: PropTypes.func.isRequired,
  redirector: PropTypes.shape({ redirectToGroupCreation: PropTypes.func.isRequired }).isRequired
};

NewGroupButton.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default withRedirector(Groups);
