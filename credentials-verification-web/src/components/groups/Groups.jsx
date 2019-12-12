import React, { useState, useEffect } from 'react';
import { Row, Icon, message } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import GroupsTable from './Organisms/Tables/GroupsTable';
import GroupFilters from './Molecules/Filters/GroupFilter';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import DeleteGroupModal from './Organisms/Modals/NewGroupModal/DeleteGroupModal';
import { groupShape } from '../../helpers/propShapes';
import noGroups from '../../images/noGroups.svg';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

const NewGroupButton = () => {
  const { t } = useTranslation();
  return (
    <CustomButton
      buttonProps={{
        onClick: () => message.info(t('groups.onCreateNewGroupClick')),
        className: 'theme-secondary'
      }}
      buttonText={t('groups.createNewGroup')}
      icon={<Icon type="plus" />}
    />
  );
};

const Groups = ({
  groups,
  setDate,
  setName,
  handleGroupDeletion,
  setGroup,
  group,
  updateGroups,
  hasMore
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [groupToDelete, setGroupToDelete] = useState({});

  const closeModal = () => {
    setGroupToDelete({});
  };

  useEffect(() => {
    const hasValues = Object.keys(groupToDelete).length !== 0;
    setOpen(hasValues);
  }, [groupToDelete]);

  const modalProps = {
    toDelete: { name: groupToDelete.groupName, id: groupToDelete.id },
    open,
    closeModal,
    handleDeletion: handleGroupDeletion,
    prefix: 'groups'
  };

  const tableProps = {
    setGroupToDelete,
    groups,
    onPageChange: updateGroups,
    setGroup,
    hasMore,
    selectedGroup: group
  };

  return (
    <div className="Wrapper">
      <DeleteGroupModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('groups.title')}</h1>
        {!setGroup && <NewGroupButton />}
      </div>
      <GroupFilters changeDate={setDate} changeFilter={setName} />
      <Row>
        {groups.length ? (
          <GroupsTable {...tableProps} />
        ) : (
          <EmptyComponent
            photoSrc={noGroups}
            photoAlias="groups.noGroups.photoAlt"
            title="groups.noGroups.title"
            subtitle="groups.noGroups.subtitle"
            button={<NewGroupButton />}
          />
        )}
      </Row>
    </div>
  );
};

Groups.defaultProps = {
  groups: [],
  setGroup: null,
  group: ''
};

Groups.propTypes = {
  groups: PropTypes.arrayOf(groupShape),
  setDate: PropTypes.func.isRequired,
  setName: PropTypes.func.isRequired,
  handleGroupDeletion: PropTypes.func.isRequired,
  setGroup: PropTypes.func,
  group: PropTypes.string,
  updateGroups: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired
};

export default Groups;
