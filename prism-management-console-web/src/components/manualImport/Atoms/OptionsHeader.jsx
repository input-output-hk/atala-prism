import React from 'react';
import PropTypes from 'prop-types';
import { Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { IMPORT_CONTACTS } from '../../../helpers/constants';

const OptionsHeader = ({ groups, selectedGroups, setSelectedGroups, addEntity }) => {
  const { t } = useTranslation();
  const { Option } = Select;
  const useCase = IMPORT_CONTACTS;

  return (
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
      <CustomButton
        buttonProps={{ onClick: addEntity, className: 'theme-secondary' }}
        buttonText={t(`${useCase}.manualImport.newContact`)}
        icon={<PlusOutlined />}
      />
    </div>
  );
};
OptionsHeader.propTypes = {
  // FIXME: add proptypes
};

export default OptionsHeader;
