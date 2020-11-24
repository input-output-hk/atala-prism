import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { DatePicker, Row, Col, Input, Icon, Select } from 'antd';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import { DEFAULT_DATE_FORMAT } from '../../../../../helpers/constants';

const CredentialsFilter = ({ fetchCredentials, credentialTypes, categories, groups }) => {
  const { t } = useTranslation();

  const [credentialId, setCredentialId] = useState('');
  const [name, setName] = useState('');
  const [credentialType, setCredentialType] = useState('');
  const [category, setCategory] = useState('');
  const [group, setGroup] = useState('');
  const [date, setDate] = useState('');

  useEffect(() => {
    fetchCredentials(false, [], credentialId, name, credentialType, category, group, date);
  }, [credentialId, name, credentialType, category, group, date]);

  const clearFilters = () => {
    setCredentialId('');
    setName('');
    setCredentialType('');
    setCategory('');
    setGroup('');
    setDate();
  };

  const datePickerProps = {
    disabled: true,
    placeholder: t('credentials.filters.date'),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => setDate(dateString),
    format: DEFAULT_DATE_FORMAT
  };

  return (
    <div className="FilterControls">
      <Row gutter={16}>
        <Col span={3}>
          <Select
            disabled
            id="credentialTypeFilter"
            value={credentialType}
            onChange={setCredentialType}
          >
            <Select.Option value="">{t('credentials.filters.credentialType')}</Select.Option>
            {credentialTypes.map(aCredentialType => (
              <Select.Option key={aCredentialType} value={aCredentialType}>
                {t(`credentials.filters.types.credentialType.${aCredentialType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
        <Col span={3}>
          <Select disabled id="categoryFilter" value={category} onChange={setCategory}>
            <Select.Option value="">{t('credentials.filters.category')}</Select.Option>
            {categories.map(categoryType => (
              <Select.Option key={categoryType} value={categoryType}>
                {t(`credentials.filters.types.category.${categoryType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
        <Col span={3}>
          <Select disabled id="groupFilter" value={group} onChange={setGroup}>
            <Select.Option value="">{t('credentials.filters.group')}</Select.Option>
            {groups.map(groupType => (
              <Select.Option key={groupType} value={groupType}>
                {t(`credentials.filters.types.group.${groupType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
        <Col span={3}>
          <DatePicker {...datePickerProps} />
        </Col>
        <Col span={3}>
          <Input
            disabled
            id="nameFilter"
            placeholder={t('credentials.filters.name')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setName(value)}
            allowClear
            value={name}
          />
        </Col>
        <Col span={3}>
          <CustomButton
            buttonProps={{
              disabled: true,
              onClick: clearFilters,
              className: 'theme-filter'
            }}
            buttonText={t('credentials.filters.clearFilters')}
          />
        </Col>
      </Row>
    </div>
  );
};

CredentialsFilter.defaultProps = {
  credentialTypes: [],
  categories: [],
  groups: []
};

CredentialsFilter.propTypes = {
  fetchCredentials: PropTypes.func.isRequired,
  credentialTypes: PropTypes.arrayOf(PropTypes.string),
  categories: PropTypes.arrayOf(PropTypes.string),
  groups: PropTypes.arrayOf(PropTypes.string)
};

export default CredentialsFilter;
