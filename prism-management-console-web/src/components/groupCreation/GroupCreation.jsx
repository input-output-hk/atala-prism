import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { GROUP_NAME_STATES } from '../../helpers/constants';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import SimpleContactFilter from '../connections/Molecules/Filters/SimpleContactFilter';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import SelectAllButton from '../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { refPropShape } from '../../helpers/propShapes';
import { useCreateGroupStore } from '../../hooks/useGroupStore';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const GroupCreation = observer(
  ({ createGroup, formRef, updateForm, formValues, updateMembers }) => {
    const { groupName } = formValues;
    const { t } = useTranslation();

    const {
      init,
      contacts,
      isLoadingContacts,
      isSaving,
      filterSortingProps,
      filterValues,
      hasFiltersApplied,
      hasMoreContacts,
      isSearching,
      isFetchingMore,
      fetchMoreGroupContacts,
      // select all
      selectedContactIds,
      isLoadingSelection,
      selectAllCheckboxStateProps,
      handleCherryPickSelection,
      selectAllContacts,
      resetSelection: resetContactsSelection
    } = useCreateGroupStore();

    useEffect(() => {
      init();
    }, [init]);

    const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

    useEffect(() => {
      updateMembers(selectedContactIds);
    }, [selectedContactIds, updateMembers]);

    useEffect(() => {
      resetContactsSelection();
    }, [filterValues.textFilter, resetContactsSelection]);

    const selectAllCheckboxProps = {
      checked: selectAllCheckboxStateProps.checked,
      indeterminate: selectAllCheckboxStateProps.indeterminate,
      disabled: isLoadingContacts || !contacts.length,
      onChange: ev => selectAllContacts(ev, filterValues.textFilter)
    };

    const nextButtonIsDisabled = !groupName || nameState !== GROUP_NAME_STATES.possible;

    return (
      <div className="WrapperGroupCreation">
        <div className="Header">
          <div className="Title">
            <h1>{t('groupCreation.title')}</h1>
            <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
          </div>

          <div className="flex">
            <div className="SearchBar">
              <SimpleContactFilter filterSortingProps={filterSortingProps} />
            </div>

            <div className="groupsButtonContainer">
              <CustomButton
                buttonProps={{
                  className: 'theme-primary',
                  disabled: nextButtonIsDisabled,
                  onClick: () => createGroup(groupName)
                }}
                buttonText={t('groupCreation.form.buttonText')}
                loading={isSaving}
              />
            </div>
          </div>
        </div>
        <div className="GroupCreationContent InfiniteScrollTableContainer">
          <div className="box">
            <h3>{t('groupCreation.groupName')}</h3>
            <GroupName
              updateForm={updateForm}
              formValues={formValues}
              formRef={formRef}
              setNameState={setNameState}
              nameState={nameState}
            />
            <h3>{t('groupCreation.addContacts')}</h3>
            <div className="UtilsContainer">
              <SelectAllButton
                isLoadingSelection={isLoadingSelection}
                selectedEntities={selectedContactIds}
                checkboxProps={selectAllCheckboxProps}
              />
            </div>
            <div className="ConnectionsTable InfiniteScrollTableContainer">
              {isLoadingContacts ? (
                <SimpleLoading />
              ) : (
                <ConnectionsTable
                  contacts={contacts}
                  isLoading={isSearching}
                  isFetchingMore={isFetchingMore}
                  hasFiltersApplied={hasFiltersApplied}
                  hasMore={hasMoreContacts}
                  fetchMoreData={fetchMoreGroupContacts}
                  selectedContactIds={selectedContactIds}
                  onSelect={handleCherryPickSelection}
                  size="md"
                />
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }
);

GroupCreation.propTypes = {
  createGroup: PropTypes.func.isRequired,
  formRef: refPropShape.isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  updateMembers: PropTypes.func.isRequired
};

export default GroupCreation;
