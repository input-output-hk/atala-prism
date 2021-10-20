import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import { Checkbox } from 'antd';
import { withApi } from '../providers/withApi';
import { useContacts } from '../../hooks/useContacts';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { CONTACT_ID_KEY, GROUP_NAME_STATES } from '../../helpers/constants';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { getCheckedAndIndeterminateProps, handleSelectAll } from '../../helpers/selectionHelpers';

import './_style.scss';
import { refPropShape } from '../../helpers/propShapes';

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
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

  const [loadingSelection, setLoadingSelection] = useState(false);

  // FIXME: remove
  const {
    contacts,
    filterProps,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching
  } = useContacts(api.contactsManager);

  const { groupName } = formValues;
  const { t } = useTranslation();

  useEffect(() => {
    updateMembers(selectedContacts);
  }, [selectedContacts, updateMembers]);

  const handleSelectAllContacts = ev =>
    handleSelectAll({
      ev,
      setSelected: setSelectedContacts,
      entities: contacts,
      hasMore,
      idKey: CONTACT_ID_KEY,
      fetchAll: () => api.contactsManager.getAllContacts(),
      setLoading: setLoadingSelection
    });

  const selectAllProps = {
    ...getCheckedAndIndeterminateProps(contacts, selectedContacts),
    disabled: loadingSelection,
    onChange: handleSelectAllContacts
  };

  const selectedLabel = selectedContacts.length ? `  (${selectedContacts.length})  ` : null;

  return (
    <div className="WrapperGroupCreation">
      <div className="Header">
        <div className="Title">
          <h1>{t('groupCreation.title')}</h1>
          <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
        </div>

        <div className="flex">
          <div className="SearchBar">
            <ConnectionsFilter {...filterProps} showFullFilter={false} />
          </div>

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
        </div>
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
          <div className="UtilsContainer mb-0">
            <div className="SelectAll">
              <Checkbox className="groupsCheckbox" {...selectAllProps}>
                {loadingSelection ? (
                  <PulseLoader size={3} color="#FFAEB3" />
                ) : (
                  <span>
                    {t('groupCreation.selectAll')}
                    {selectedLabel}
                  </span>
                )}
              </Checkbox>
            </div>
          </div>
          <div className="addContactSection">
            <div className="addContactsContainer">
              {(isSearching && !contacts.length) || isLoading ? (
                <SimpleLoading />
              ) : (
                <ConnectionsTable
                  contacts={contacts}
                  selectedContacts={selectedContacts}
                  setSelectedContacts={setSelectedContacts}
                  handleContactsRequest={handleContactsRequest}
                  searching={isSearching}
                  hasMore={hasMore}
                  size="md"
                />
              )}
            </div>
          </div>
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
      getContacts: PropTypes.func.isRequired,
      getAllContacts: PropTypes.func.isRequired
    }).isRequired,
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  createGroup: PropTypes.func.isRequired,
  formRef: refPropShape.isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  isSaving: PropTypes.bool,
  updateMembers: PropTypes.func.isRequired
};

export default withApi(withRedirector(GroupCreation));
