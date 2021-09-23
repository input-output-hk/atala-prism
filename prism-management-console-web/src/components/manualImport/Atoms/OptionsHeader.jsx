import React from 'react';
import PropTypes from 'prop-types';
import { Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { IMPORT_CONTACTS } from '../../../helpers/constants';
import { groupShape } from '../../../helpers/propShapes';

const OptionsHeader = ({ groupsProps, addEntity }) => {
  const { t } = useTranslation();
  const { groups, selectedGroups, setSelectedGroups } = groupsProps;
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
        buttonProps={{ onClick: addEntity, className: 'theme-secondary', icon: <PlusOutlined /> }}
        buttonText={t(`${useCase}.manualImport.newContact`)}
      />
    </div>
  );
};

OptionsHeader.defaultProps = {
  addEntity: undefined
};

OptionsHeader.propTypes = {
  groupsProps: PropTypes.shape({
    groups: PropTypes.arrayOf(groupShape).isRequired,
    selectedGroups: PropTypes.arrayOf(PropTypes.string),
    setSelectedGroups: PropTypes.func.isRequired
  }).isRequired,
  addEntity: PropTypes.func
};

export default OptionsHeader;
