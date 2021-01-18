import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox, Row, Col } from 'antd';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { MAX_CONTACTS, GROUP_NAME_STATES } from '../../helpers/constants';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import './_style.scss';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';

const GroupCreation = ({
  api,
  createGroup,
  formRef,
  updateForm,
  formValues,
  updateMembers,
  isSaving
}) => {
  const [selectedContacts, setSelectedContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [searching, setSearching] = useState(true);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

  const {
    contacts,
    filteredContacts,
    filterProps,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredList(api.contactsManager, setLoadingContacts, setSearching);

  const { groupName } = formValues;
  const { t } = useTranslation();

  useEffect(() => {
    setLoadingContacts(true);
    if (!contacts.length) handleContactsRequest();
  }, []);

  useEffect(() => {
    updateMembers(selectedContacts);
  }, [selectedContacts]);

  const handleSelectAll = async e => {
    const { checked } = e.target;
    if (checked) {
      const allContacts = await api.contactsManager.getContacts(null, MAX_CONTACTS);
      setSelectedContacts(allContacts.map(contact => contact.contactid));
    } else {
      setSelectedContacts([]);
    }
  };

  return (
    <div className="Wrapper">
      <div className="Header">
        <h1>{t('groupCreation.title')}</h1>
        <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
      </div>

      <div className="GroupCreationContent">
        <div className="box">
          <h3>{t('groupCreation.groupName')}</h3>
          <GroupName
            updateForm={updateForm}
            formValues={formValues}
            ref={formRef}
            setNameState={setNameState}
            nameState={nameState}
          />
          <h3>{t('groupCreation.addContacts')}</h3>
          <Row className="UtilsContainer mb-0">
            <Col span={3}>
              <Checkbox onChange={handleSelectAll}>{t('groupCreation.selectAll')}</Checkbox>
              {selectedContacts.length ? `(${selectedContacts.length})` : null}
            </Col>
            <Col span={19}>
              <ConnectionsFilter {...filterProps} withStatus={false} />
            </Col>
          </Row>
          <Row gutter={10} align="bottom" type="flex">
            <Col sm={24} md={20}>
              <div className="addContactsContainer">
                {(searching && !filteredContacts.length) || loadingContacts ? (
                  <SimpleLoading />
                ) : (
                  <ConnectionsTable
                    contacts={filteredContacts}
                    selectedContacts={selectedContacts}
                    setSelectedContacts={setSelectedContacts}
                    handleContactsRequest={handleContactsRequest}
                    searching={searching}
                    hasMore={hasMore}
                    size="md"
                  />
                )}
              </div>
            </Col>
            <Col sm={24} md={4}>
              <div className="groupsButtonContainer">
                <CustomButton
                  buttonProps={{
                    className: 'theme-primary',
                    disabled: nameState === GROUP_NAME_STATES.failed,
                    onClick: () => createGroup(groupName)
                  }}
                  buttonText={t('groupCreation.form.buttonText')}
                  loading={isSaving}
                />
              </div>
            </Col>
          </Row>
        </div>
      </div>
    </div>
  );
};

GroupCreation.defaultProps = {
  isSaving: false
};

GroupCreation.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired,
      getContacts: PropTypes.func.isRequired
    }).isRequired,
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  createGroup: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  isSaving: PropTypes.bool,
  updateMembers: PropTypes.func.isRequired
};

export default withApi(withRedirector(GroupCreation));
