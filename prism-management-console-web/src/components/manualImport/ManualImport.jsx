import React, { useState, useEffect, useContext } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import { isEmptyContact } from '../../helpers/contactValidations';
import { isEmptyCredential } from '../../helpers/credentialDataValidation';
import GenericFooter from '../common/Molecules/GenericFooter/GenericFooter';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import ContactCreationTable from './Organisms/Tables/ContactCreationTable';
import CredentialCreationTable from './Organisms/Tables/CredentialCreationTable';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { contactCreationShape, credentialTypeShape, groupShape } from '../../helpers/propShapes';
import { DynamicFormContext } from '../../providers/DynamicFormProvider';

import './_style.scss';

const ManualImport = ({
  tableProps,
  groupsProps,
  cancelImport,
  onSave,
  useCase,
  showGroupSelection,
  isEmbedded,
  loading,
  credentialType
}) => {
  const [disableNext, setDisableNext] = useState(true);

  const { t } = useTranslation();
  const { Option } = Select;
  const { dataSource = [] } = tableProps;
  const { groups, selectedGroups, setSelectedGroups } = groupsProps;
  const { saveFormProviderAvailable, addEntity } = useContext(DynamicFormContext);

  useEffect(() => {
    const shouldDisableNext = () => {
      const emptyEntries = dataSource.filter(
        useCase === IMPORT_CONTACTS
          ? isEmptyContact
          : dataRow => isEmptyCredential(dataRow, credentialType.fields)
      );
      const errors = dataSource.filter(c => c.errorFields);

      return emptyEntries.length || errors.length;
    };

    setDisableNext(shouldDisableNext());
  }, [dataSource, credentialType.fields, useCase]);

  // Leave const for backward compatibility, when all forms uses DynamicForm, feel free for remove this and it's use
  const isContactCreation = useCase === IMPORT_CONTACTS;
  const isSaveEnabled = isContactCreation ? saveFormProviderAvailable : !disableNext;

  return (
    <div className="ManualImportWrapper">
      <div className={`ContentHeader TitleAndSubtitle ${isEmbedded ? 'EmbeddedHeader' : ''}`}>
        <h3>{t(`${useCase}.manualImport.title`)}</h3>

        <div className="Options">
          {showGroupSelection ? (
            <div className="MultiSelectContainer">
              <Select
                mode="tags"
                style={{ width: '100%' }}
                value={selectedGroups}
                placeholder={t('manualImport.assignToGroups.placeholder')}
                onChange={setSelectedGroups}
              >
                {groups.map(({ name }) => (
                  <Option key={name}>{name}</Option>
                ))}
              </Select>
            </div>
          ) : (
            <p>{t(`${useCase}.manualImport.info`)}</p>
          )}
          {isContactCreation && (
            <CustomButton
              buttonProps={{ onClick: addEntity, className: 'theme-secondary' }}
              buttonText={t(`${useCase}.manualImport.newContact`)}
              icon={<PlusOutlined />}
            />
          )}
        </div>
      </div>
      <div className="ManualImportContent">
        {useCase === IMPORT_CONTACTS ? (
          <ContactCreationTable tableProps={tableProps} setDisableSave={setDisableNext} />
        ) : (
          <CredentialCreationTable
            tableProps={tableProps}
            setDisableSave={setDisableNext}
            credentialType={credentialType}
          />
        )}
      </div>
      <GenericFooter
        previous={cancelImport}
        next={onSave}
        // backward compatibility
        disableNext={!isSaveEnabled}
        labels={{ previous: t('actions.back'), next: t('actions.save') }}
        loading={loading}
      />
    </div>
  );
};

ManualImport.defaultProps = {
  credentialType: {}
};

ManualImport.propTypes = {
  tableProps: PropTypes.shape({
    dataSource: PropTypes.shape(contactCreationShape)
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
  loading: PropTypes.bool.isRequired,
  credentialType: PropTypes.shape(credentialTypeShape)
};

export default ManualImport;
