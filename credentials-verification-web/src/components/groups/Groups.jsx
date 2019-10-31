import React, { useState } from 'react';
import { Row, Icon } from 'antd';
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
      onClick={() => console.log('create a new group')}
      buttonText={t('groups.createNewGroup')}
      theme="theme-secondary"
      icon={<Icon type="plus" />}
    />
  );
};

const Groups = ({ groups, count, offset, setOffset, setDate, setName, handleGroupDeletion }) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [groupToDelete, setGroupToDelete] = useState();

  const closeModal = () => {
    setOpen(false);
    setGroupToDelete({});
  };

  const modalProps = {
    groupToDelete,
    open,
    closeModal,
    handleGroupDeletion
  };

  return (
    <React.Fragment>
      <DeleteGroupModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('groups.title')}</h1>
        <NewGroupButton />
      </div>
      <GroupFilters changeDate={setDate} changeFilter={setName} />
      <Row>
        {groups.length ? (
          <GroupsTable
            setOpen={setOpen}
            setGroupToDelete={setGroupToDelete}
            groups={groups}
            current={offset + 1}
            total={count}
            offset={offset}
            onPageChange={value => setOffset(value - 1)}
          />
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
    </React.Fragment>
  );
};

Groups.defaultProps = {
  groups: [],
  count: 0,
  offset: 0
};

Groups.propTypes = {
  groups: PropTypes.arrayOf(groupShape),
  count: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  setDate: PropTypes.func.isRequired,
  setName: PropTypes.func.isRequired,
  handleGroupDeletion: PropTypes.func.isRequired
};

export default Groups;
