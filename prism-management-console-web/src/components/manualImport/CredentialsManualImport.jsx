import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import { filterEmptyContact } from '../../helpers/contactValidations';
import GenericFooter from '../common/Molecules/GenericFooter/GenericFooter';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import ContactCreationTable from './Organisms/Tables/ContactCreationTable';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { contactCreationShape, groupShape } from '../../helpers/propShapes';

import './_style.scss';

const ManualImport = ({ tableProps, cancelImport, onSave, useCase, isEmbedded, loading }) => {
  const [disableNext, setDisableNext] = useState(true);

  const { t } = useTranslation();
  const { contacts, addNewContact } = tableProps;

  const shouldDisableNext = () => {
    const noEmptyContacts = contacts.filter(filterEmptyContact);
    const errors = contacts.filter(c => c.errors);

    return !noEmptyContacts.length || errors.length;
  };

  useEffect(() => {
    setDisableNext(shouldDisableNext());
  }, [contacts]);

  return (
    <div className="ManualImportWrapper">
      <div className={`ContentHeader TitleAndSubtitle ${isEmbedded ? 'EmbeddedHeader' : ''}`}>
        <h3>{t(`${useCase}.manualImport.title`)}</h3>
        <div className="Options">
          <CustomButton
            buttonProps={{ onClick: addNewContact, className: 'theme-secondary' }}
            buttonText={t(`${useCase}.manualImport.newContact`)}
            icon={<Icon type="plus" />}
          />
        </div>
      </div>
      <div className="ManualImportContent">
        <ContactCreationTable {...tableProps} setDisableSave={setDisableNext} />
      </div>
      <GenericFooter
        previous={cancelImport}
        next={onSave}
        disableNext={disableNext}
        labels={{ previous: t('actions.back'), next: t('actions.save') }}
        loading={loading}
      />
    </div>
  );
};

ManualImport.propTypes = {
  tableProps: PropTypes.shape({
    contacts: PropTypes.shape(contactCreationShape).isRequired,
    updateDataSource: PropTypes.func.isRequired,
    deleteContact: PropTypes.func.isRequired,
    addNewContact: PropTypes.func.isRequired
  }).isRequired,
  groupsProps: PropTypes.shape({
    groups: PropTypes.shape(groupShape).isRequired,
    selectedGroups: PropTypes.func.isRequired,
    setSelectedGroups: PropTypes.func.isRequired
  }).isRequired,
  cancelImport: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  showGroupSelection: PropTypes.bool.isRequired,
  isEmbedded: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired
};

export default ManualImport;
