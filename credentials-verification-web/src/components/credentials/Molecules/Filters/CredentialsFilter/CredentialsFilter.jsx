import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { DatePicker, Row, Col, Input, Icon, Select } from 'antd';
import moment from 'moment';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';

const CredentialsFilter = ({
  credentialId,
  setCredentialId,
  name,
  setName,
  credentialTypes,
  credentialType,
  setCredentialType,
  categories,
  category,
  setCategory,
  groups,
  group,
  setGroup,
  date,
  setDate,
  clearFilters
}) => {
  const { t } = useTranslation();

  const datePickerProps = {
    placeholder: t('credentials.filters.date'),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => setDate(dateString),
    value: date && moment(date)
  };

  return (
    <div className="FilterControls">
      <Row gutter={16}>
        <Col span={3}>
          <Input
            id="credentialIdFilter"
            placeholder={t('credentials.filters.ID')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setCredentialId(value)}
            allowClear
            value={credentialId}
          />
        </Col>
        <Col span={3}>
          <Select id="credentialTypeFilter" value={credentialType} onChange={setCredentialType}>
            <Select.Option value="">{t('credentials.filters.credentialType')}</Select.Option>
            {credentialTypes.map(aCredentialType => (
              <Select.Option key={aCredentialType} value={aCredentialType}>
                {t(`credentials.filters.types.credentialType.${aCredentialType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
        <Col span={3}>
          <Select id="categoryFilter" value={category} onChange={setCategory}>
            <Select.Option value="">{t('credentials.filters.category')}</Select.Option>
            {categories.map(categoryType => (
              <Select.Option key={categoryType} value={categoryType}>
                {t(`credentials.filters.types.category.${categoryType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
        <Col span={3}>
          <Select id="groupFilter" value={group} onChange={setGroup}>
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
  credentialId: '',
  name: '',
  credentialType: ''
};

CredentialsFilter.propTypes = {
  credentialId: PropTypes.string,
  setCredentialId: PropTypes.func.isRequired,
  name: PropTypes.string,
  setName: PropTypes.func.isRequired,
  credentialType: PropTypes.string,
  setCredentialType: PropTypes.func.isRequired,
  clearFilters: PropTypes.func.isRequired
};

export default CredentialsFilter;
