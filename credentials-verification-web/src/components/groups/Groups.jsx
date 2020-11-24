import React, { useState, useEffect } from 'react';
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
import { withRedirector } from '../providers/withRedirector';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { backendDateFormat } from '../../helpers/formatters';
import { filterByInclusion } from '../../helpers/filterHelpers';

const NewGroupButton = ({ onClick }) => {
  const { t } = useTranslation();
  return (
    <CustomButton
      buttonProps={{
        onClick,
        className: 'theme-secondary'
      }}
      buttonText={t('groups.createNewGroup')}
      icon={<Icon type="plus" />}
    />
  );
};

const Groups = ({
  groups,
  handleGroupDeletion,
  updateGroups,
  hasMore,
  loading,
  redirector: { redirectToGroupCreation }
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [date, setDate] = useState('');

  const [groupToDelete, setGroupToDelete] = useState({});
  const [filteredGroups, setFilteredGroups] = useState([]);

  const closeModal = () => {
    setGroupToDelete({});
  };

  useEffect(() => {
    setFilteredGroups(filterGroups());
  }, [groups, name, date]);

  useEffect(() => {
    const hasValues = Object.keys(groupToDelete).length !== 0;
    setOpen(hasValues);
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

  const modalProps = {
    toDelete: { name: groupToDelete.groupName, id: groupToDelete.id },
    open,
    closeModal,
    handleGroupDeletion,
    prefix: 'groups'
  };

  const tableProps = {
    onPageChange: updateGroups,
    scroll: '60vh',
    setGroupToDelete,
    hasMore
  };

  const newGroupButton = <NewGroupButton onClick={redirectToGroupCreation} />;

  const renderContent = () => {
    if (loading) return <SimpleLoading size="md" />;
    if (groups.length) return <GroupsTable {...tableProps} groups={filteredGroups} />;
    return <EmptyComponent photoSrc={noGroups} model={t('groups.title')} button={newGroupButton} />;
  };

  return (
    <div className="Wrapper">
      <DeleteGroupModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('groups.title')}</h1>
        {newGroupButton}
      </div>
      <GroupFilters updateGroups={handleUpdateGroups} />
      <Row>{renderContent()}</Row>
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
  updateGroups: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  loading: PropTypes.bool,
  redirector: PropTypes.shape({ redirectToGroupCreation: PropTypes.func.isRequired }).isRequired
};

NewGroupButton.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default withRedirector(Groups);
