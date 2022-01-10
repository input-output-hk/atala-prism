import React, { createRef, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Col, Row, Button } from 'antd';
import SimpleContactFilter from '../connections/Molecules/Filters/SimpleContactFilter';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { GROUP_NAME_STATES } from '../../helpers/constants';
import AddContactsModal from './AddContactsModal/AddContactsModal';
import ConfirmDeletionModal from './ConfirmDeletionModal/ConfirmDeletionModal';
import SelectAllButton from '../newCredential/Molecules/RecipientsTable/SelectAllButton';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import { getGroupContactColumns } from '../../helpers/tableDefinitions/contacts';
import { useCurrentGroupStore } from '../../hooks/useGroupStore';
import './_style.scss';

const GroupEditing = observer(({ onGroupRename, onRemoveContacts, onAddContacts }) => {
  const {
    filterSortingProps,
    filterValues,
    isLoadingGroup,
    isLoadingMembers,
    isFetchingMore,
    isSearching,
    isSaving,
    name,
    members,
    fetchMoreGroupMembers,
    hasMoreMembers,
    // select all
    selectedContactIds,
    isLoadingSelection,
    selectAllCheckboxStateProps,
    handleCherryPickSelection,
    selectAllContacts,
    resetSelection: resetContactsSelection
  } = useCurrentGroupStore();
  const { hasFiltersApplied } = filterSortingProps;

  const { t } = useTranslation();
  const formRef = createRef();
  const [groupName, setGroupName] = useState(name);
  const [contactsToRemove, setContactsToRemove] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalDeletionVisible, setModalDeletionVisible] = useState(false);
  const [editing, setEditing] = useState(false);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);
  const formValues = { groupName };

  const selectAllCheckboxProps = {
    checked: selectAllCheckboxStateProps.checked,
    indeterminate: selectAllCheckboxStateProps.indeterminate,
    disabled: isLoadingMembers || !members.length,
    onChange: ev => selectAllContacts(ev, filterValues.textFilter)
  };

  useEffect(() => {
    setGroupName(name);
  }, [name]);

  useEffect(() => {
    resetContactsSelection();
  }, [filterValues.textFilter, resetContactsSelection]);

  const handleCancelClick = () => {
    setGroupName(name);
    setEditing(false);
  };

  const handleRemoveClick = () =>
    onRemoveContacts(contactsToRemove).finally(() => {
      setModalDeletionVisible(false);
      resetContactsSelection();
    });

  const handleAddContactConfirm = aContactsList =>
    onAddContacts(aContactsList).then(() => {
      setModalVisible(false);
      resetContactsSelection();
    });

  const handleRemoveContactRequest = aContactsList => {
    setContactsToRemove(aContactsList);
    setModalDeletionVisible(true);
  };

  const handleSaveGroupName = () =>
    groupName && onGroupRename(groupName).finally(() => setEditing(false));

  const editButtonProps = {
    type: 'link',
    style: { color: '#FF2D3B' },
    onClick: () => setEditing(!editing)
  };

  const handleDelete = contact => handleRemoveContactRequest([contact]);

  const firstColProps = {
    lg: 20,
    md: 19,
    sm: 18
  };

  const secondColProps = {
    lg: 4,
    md: 5,
    sm: 6
  };

  const saveGroupRenameIsDisabled = !groupName || nameState !== GROUP_NAME_STATES.possible;

  const renderNameSection = () => {
    if (isLoadingGroup) return <SimpleLoading size="sm" />;
    if (editing)
      return (
        <GroupName
          formRef={formRef}
          updateForm={setGroupName}
          groupName={groupName}
          formValues={formValues}
          setNameState={setNameState}
          nameState={nameState}
          className="GroupNameForm"
        />
      );
    return <div>{groupName}</div>;
  };

  return (
    <div className="Wrapper">
      <Row className="Header">
        <Col {...firstColProps}>
          <h1>{t('groupEditing.title')}</h1>
          <h3 className="groupsSubtitle">{t('groupEditing.subtitle')}</h3>
        </Col>
        <Col {...secondColProps}>
          <CustomButton
            overrideClassName="theme-secondary custom"
            buttonProps={{
              onClick: () => setModalVisible(true),
              icon: <PlusOutlined />
            }}
            buttonText={t('groupEditing.buttons.addContacts')}
          />
        </Col>
      </Row>
      <AddContactsModal
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onConfirm={handleAddContactConfirm}
      />
      <ConfirmDeletionModal
        visible={modalDeletionVisible}
        length={contactsToRemove.length}
        onCancel={() => setModalDeletionVisible(false)}
        onConfirm={handleRemoveClick}
      />

      <div className="GroupEditingContent InfiniteScrollTableContainer">
        <Row className="GroupNameContainer">
          <Col {...firstColProps} className="GroupName">
            {renderNameSection()}
            {editing ? (
              <>
                <Button
                  {...editButtonProps}
                  disabled={saveGroupRenameIsDisabled}
                  onClick={handleSaveGroupName}
                >
                  {t('groupEditing.buttons.save')}
                </Button>
                <Button {...editButtonProps} onClick={handleCancelClick}>
                  {t('groupEditing.buttons.cancel')}
                </Button>
              </>
            ) : (
              <Button {...editButtonProps}>{t('groupEditing.buttons.edit')}</Button>
            )}
          </Col>
          <Col {...secondColProps}>
            <CustomButton
              overrideClassName="theme-outline custom"
              buttonProps={{
                disabled: !selectedContactIds.length,
                onClick: () => handleRemoveContactRequest(selectedContactIds)
              }}
              buttonText={t('groupEditing.buttons.remove')}
              loading={isSaving}
            />
          </Col>
        </Row>
        <h3>{t('groupEditing.contacts')}</h3>
        <div className="filterContainer">
          <div className="connectionFilter">
            <SimpleContactFilter filterSortingProps={filterSortingProps} />
          </div>
          <SelectAllButton
            isLoadingSelection={isLoadingSelection}
            selectedEntities={selectedContactIds}
            checkboxProps={selectAllCheckboxProps}
          />
        </div>
        <div className="ConnectionsTable">
          {isLoadingMembers ? (
            <SimpleLoading />
          ) : (
            <ConnectionsTable
              contacts={members}
              isLoading={isSearching}
              isFetchingMore={isFetchingMore}
              hasFiltersApplied={hasFiltersApplied}
              hasMore={hasMoreMembers}
              fetchMoreData={fetchMoreGroupMembers}
              selectedContactIds={selectedContactIds}
              onSelect={handleCherryPickSelection}
              columns={getGroupContactColumns(handleDelete)}
              size="md"
            />
          )}
        </div>
      </div>
    </div>
  );
});

GroupEditing.defaultProps = {};

GroupEditing.propTypes = {
  onGroupRename: PropTypes.func.isRequired,
  onRemoveContacts: PropTypes.func.isRequired,
  onAddContacts: PropTypes.func.isRequired
};

export default GroupEditing;
