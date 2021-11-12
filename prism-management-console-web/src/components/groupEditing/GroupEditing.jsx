import React, { createRef, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Col, Row, Button } from 'antd';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { CONTACT_ID_KEY, GROUP_NAME_STATES } from '../../helpers/constants';
import AddContactsModal from './AddContactsModal/AddContactsModal';
import ConfirmDeletionModal from './ConfirmDeletionModal/ConfirmDeletionModal';
import { contactShape, groupShape } from '../../helpers/propShapes';
import SelectAllButton from '../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useSelectAll } from '../../hooks/useSelectAll';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import { getGroupContactColumns } from '../../helpers/tableDefinitions/contacts';
import { useCurrentGroupState } from '../../hooks/useCurrentGroupState';
import { useContactUiState } from '../../hooks/useContactStore';
import './_style.scss';

const GroupEditing = observer(({ onGroupRename, onRemoveContacts, onAddContacts }) => {
  const { applyFilters, hasFiltersApplied } = useContactUiState({ reset: true });
  const {
    isLoadingGroup,
    isLoadingMembers,
    isSaving,
    name,
    members,
    getAllGroupMembers
  } = useCurrentGroupState();

  const { t } = useTranslation();
  const formRef = createRef();
  const [groupName, setGroupName] = useState(name);
  const [contactsToRemove, setContactsToRemove] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalDeletionVisible, setModalDeletionVisible] = useState(false);
  const [selectedGroupContacts, setSelectedGroupContacts] = useState([]);
  const [editing, setEditing] = useState(false);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);
  const formValues = { groupName };

  const filteredContacts = applyFilters(members);

  const { loadingSelection, checkboxProps } = useSelectAll({
    displayedEntities: filteredContacts,
    entitiesFetcher: getAllGroupMembers,
    entityKey: CONTACT_ID_KEY,
    selectedEntities: selectedGroupContacts,
    setSelectedEntities: setSelectedGroupContacts,
    isFetching: isLoadingMembers
  });

  useEffect(() => {
    setGroupName(name);
  }, [name]);

  const handleCancelClick = () => {
    setGroupName(name);
    setEditing(false);
  };

  const handleRemoveClick = () =>
    onRemoveContacts(contactsToRemove).finally(() => {
      setModalDeletionVisible(false);
      setSelectedGroupContacts([]);
    });

  const handleAddContactConfirm = aContactsList =>
    onAddContacts(aContactsList).finally(() => setModalVisible(false));

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

  const renderNameSection = () => {
    if (isLoadingGroup) return <SimpleLoading size="sm" />;
    if (editing)
      return (
        <GroupName
          ref={formRef}
          updateForm={setGroupName}
          groupName={groupName}
          formValues={formValues}
          setNameState={setNameState}
          nameState={nameState}
          className="GroupNameForm"
        />
      );
    return <p>{groupName}</p>;
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
                <Button {...editButtonProps} disabled={!groupName} onClick={handleSaveGroupName}>
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
                disabled: !selectedGroupContacts.length,
                onClick: () => handleRemoveContactRequest(selectedGroupContacts)
              }}
              buttonText={t('groupEditing.buttons.remove')}
              loading={isSaving}
            />
          </Col>
        </Row>
        <h3>{t('groupEditing.contacts')}</h3>
        <div className="filterContainer">
          <div className="connectionFilter">
            <ConnectionsFilter showFullFilter={false} />
          </div>
          <SelectAllButton
            loadingSelection={loadingSelection}
            selectedEntities={selectedGroupContacts}
            checkboxProps={checkboxProps}
          />
        </div>
        <div className="ConnectionsTable">
          <ConnectionsTable
            // TODO: add pagination for getting group members (ATA-5701)
            contacts={filteredContacts}
            hasFiltersApplied={hasFiltersApplied}
            isFetchingMore={isLoadingMembers}
            columns={getGroupContactColumns(handleDelete)}
            selectedContacts={selectedGroupContacts}
            setSelectedContacts={setSelectedGroupContacts}
            size="md"
            searchDueGeneralScroll
          />
        </div>
      </div>
    </div>
  );
});

GroupEditing.defaultProps = {
  group: {
    name: ''
  },
  members: []
};

GroupEditing.propTypes = {
  group: PropTypes.arrayOf(groupShape),
  members: PropTypes.arrayOf(contactShape),
  onGroupRename: PropTypes.func.isRequired,
  onRemoveContacts: PropTypes.func.isRequired,
  onAddContacts: PropTypes.func.isRequired
};

export default GroupEditing;
