import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Checkbox, Col, Row, Button } from 'antd';
import { PulseLoader } from 'react-spinners';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { CONTACT_ID_KEY, GROUP_NAME_STATES } from '../../helpers/constants';
import GroupContacts from './GroupContacts';
import AddContactsModal from './AddContactsModal/AddContactsModal';
import ConfirmDeletionModal from './ConfirmDeletionModal/ConfirmDeletionModal';
import { contactShape, groupShape } from '../../helpers/propShapes';
import { getCheckedAndIndeterminateProps, handleSelectAll } from '../../helpers/selectionHelpers';

import './_style.scss';

const GroupEditing = ({
  group,
  filterProps,
  handleContactsRequest,
  contacts,
  onGroupRename,
  onRemoveContacts,
  onAddContacts,
  hasMore,
  isSaving,
  loading,
  loadingContacts,
  fetchAllContacts
}) => {
  const { t } = useTranslation();
  const formRef = React.createRef();
  const [groupName, setGroupName] = useState('');
  const [contactsToRemove, setContactsToRemove] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalDeletionVisible, setModalDeletionVisible] = useState(false);
  const [selectedGroupContacts, setSelectedGroupContacts] = useState([]);
  const [editing, setEditing] = useState(false);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);
  const [loadingSelection, setLoadingSelection] = useState(false);
  const formValues = { groupName };

  useEffect(() => {
    if (group.name) {
      setGroupName(group.name);
    }
  }, [group.name]);

  const handleSelectAllContacts = ev =>
    handleSelectAll({
      ev,
      setSelected: setSelectedGroupContacts,
      entities: contacts,
      hasMore,
      idKey: CONTACT_ID_KEY,
      fetchAll: fetchAllContacts,
      setLoading: setLoadingSelection
    });

  const selectAllProps = {
    ...getCheckedAndIndeterminateProps(contacts, selectedGroupContacts),
    disabled: loadingSelection,
    onChange: handleSelectAllContacts
  };

  const handleCancelClick = () => {
    setGroupName(group.name);
    setEditing(false);
  };

  const handleRemoveClick = async () =>
    onRemoveContacts(contactsToRemove).finally(() => {
      setModalDeletionVisible(false);
      setSelectedGroupContacts([]);
    });

  const handleAddContactConfirm = async aContactsList =>
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
    if (loading) return <SimpleLoading size="sm" />;
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
        groupName={groupName}
        onCancel={() => setModalVisible(false)}
        onConfirm={handleAddContactConfirm}
      />

      <ConfirmDeletionModal
        visible={modalDeletionVisible}
        length={contactsToRemove.length}
        onCancel={() => setModalDeletionVisible(false)}
        onConfirm={handleRemoveClick}
      />

      <div className="GroupEditingContent">
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
            <ConnectionsFilter {...filterProps} showFullFilter={false} />
          </div>
          <div>
            <Checkbox className="groupsCheckbox" {...selectAllProps}>
              {loadingSelection ? (
                <PulseLoader size={3} color="#FFAEB3" />
              ) : (
                <span>
                  {t('groupEditing.selectAll')}
                  {!!selectedGroupContacts.length && `  (${selectedGroupContacts.length})  `}{' '}
                </span>
              )}
            </Checkbox>
          </div>
        </div>
        <Row gutter={10} align="bottom" type="flex" className="ContactsContainer">
          <GroupContacts
            contacts={contacts}
            groupName={groupName}
            selectedContacts={selectedGroupContacts}
            setSelectedContacts={setSelectedGroupContacts}
            onDeleteContact={handleRemoveContactRequest}
            handleContactsRequest={handleContactsRequest}
            loading={loadingContacts || isSaving}
            hasMore={hasMore}
          />
        </Row>
      </div>
    </div>
  );
};

GroupEditing.defaultProps = {
  group: {
    name: ''
  },
  filterProps: {},
  loading: true,
  loadingContacts: false,
  isSaving: false,
  contacts: []
};

GroupEditing.propTypes = {
  handleContactsRequest: PropTypes.func.isRequired,
  onGroupRename: PropTypes.func.isRequired,
  onRemoveContacts: PropTypes.func.isRequired,
  onAddContacts: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  group: PropTypes.shape(groupShape),
  filterProps: PropTypes.shape(),
  loading: PropTypes.bool,
  loadingContacts: PropTypes.bool,
  isSaving: PropTypes.bool,
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  fetchAllContacts: PropTypes.func.isRequired
};

export default GroupEditing;
