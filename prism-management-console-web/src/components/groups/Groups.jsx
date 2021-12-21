import React, { useState, useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import GroupsTable from './Organisms/Tables/GroupsTable';
import DeleteGroupModal from './Organisms/Modals/DeleteGroupModal/DeleteGroupModal';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import CopyGroupModal from './Organisms/Modals/CopyGroupModal/CopyGroupModal';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import { useGroupsPageStore } from '../../hooks/useGroupStore';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import GroupActionsHeader from './Molecules/Headers/GroupActionsHeader';

import './_style.scss';

const Groups = observer(({ handleGroupDeletion, copyGroup }) => {
  const { t } = useTranslation();
  const {
    filterSortingProps,
    groups,
    fetchMoreData,
    isFetchingMore,
    hasMore,
    hasFiltersApplied,
    isSearching,
    isLoadingFirstPage,
    isSaving
  } = useGroupsPageStore();

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
    onSave: copyName => copyGroup(selectedGroup, copyName).then(closeCopyModal),
    isInProgress: isSaving
  };

  const handleCopy = group => {
    setSelectedGroup(group);
    setIsCopyModalOpen(true);
  };

  return (
    <div className="GroupsContainer Wrapper">
      {accountStatus === UNCONFIRMED && <WaitBanner />}
      <DeleteGroupModal {...deleteModalProps} />
      <CopyGroupModal {...copyModalProps} />
      <div className="ContentHeader">
        <div className="title">
          <h1>{t('groups.title')}</h1>
        </div>
        <div className="GroupActionsHeaderWrapper">
          {accountStatus === CONFIRMED && (
            <GroupActionsHeader filterSortingProps={filterSortingProps} />
          )}
        </div>
      </div>
      <div className="GroupContentContainer InfiniteScrollTableContainer">
        {isLoadingFirstPage ? (
          <SimpleLoading size="md" />
        ) : (
          <GroupsTable
            groups={groups}
            fetchMoreData={fetchMoreData}
            isFetchingMore={isFetchingMore}
            hasMore={hasMore}
            hasFiltersApplied={hasFiltersApplied}
            isLoading={isSearching || isSaving}
            onCopy={handleCopy}
            setGroupToDelete={setGroupToDelete}
          />
        )}
      </div>
    </div>
  );
});

Groups.propTypes = {
  handleGroupDeletion: PropTypes.func.isRequired,
  copyGroup: PropTypes.func.isRequired
};

export default Groups;
