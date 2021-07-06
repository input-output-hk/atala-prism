import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import ContactCreationTable from './Organisms/Tables/ContactCreationTable';
import CredentialCreationTable from './Organisms/Tables/CredentialCreationTable';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { contactCreationShape, credentialTypeShape, groupShape } from '../../helpers/propShapes';

import './_style.scss';

const ManualImport = ({
  tableProps,
  groupsProps,
  useCase,
  isEmbedded,
  credentialType,
  addEntity
}) => {
  const { t } = useTranslation();
  const { Option } = Select;
  const { groups, selectedGroups, setSelectedGroups } = groupsProps;

  return (
    <div className="ManualImportWrapper">
      <div className={`ContentHeader TitleAndSubtitle ${isEmbedded ? 'EmbeddedHeader' : ''}`}>
        <div className="Options">
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
          {useCase === IMPORT_CONTACTS && (
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
          <ContactCreationTable tableProps={tableProps} />
        ) : (
          <CredentialCreationTable tableProps={tableProps} credentialType={credentialType} />
        )}
      </div>
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
  useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
  isEmbedded: PropTypes.bool.isRequired,
  credentialType: PropTypes.shape(credentialTypeShape),
  addEntity: PropTypes.func.isRequired
};

export default ManualImport;
