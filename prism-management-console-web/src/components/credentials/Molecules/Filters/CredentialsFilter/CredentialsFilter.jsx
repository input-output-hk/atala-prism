import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import CustomDatePicker from '../../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import CustomInputGroup from '../../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import {
  NORMALIZED_CONNECTION_STATUSES,
  CREDENTIAL_STATUSES
} from '../../../../../helpers/constants';

const credentialStatuses = Object.keys(CREDENTIAL_STATUSES);

const CredentialsFilter = ({ credentialsTypes, filterProps, isIssued }) => {
  const { t } = useTranslation();

  const CREDENTIALS_TYPES_ENABLED = Object.values(credentialsTypes).filter(item => item.enabled);

  const datePickerProps = {
    placeholder: t('credentials.filters.dateSigned'),
    suffixIcon: <DownOutlined />,
    onChange: (_, dateString) => filterProps.setDate(dateString)
  };

  const datePickerReceivedProps = {
    placeholder: t('credentials.filters.dateReceived'),
    suffixIcon: <DownOutlined />,
    onChange: (_, dateString) => filterProps.setDate(dateString)
  };

  const renderBaseFilters = () => (
    <>
      <div>
        <Input
          id="nameFilter"
          placeholder={t('credentials.filters.search')}
          prefix={<SearchOutlined />}
          onChange={({ target: { value } }) => filterProps.setName(value)}
          allowClear
          value={filterProps.name}
        />
      </div>
      <div>
        <Select
          id="credentialTypeFilter"
          value={filterProps.credentialType}
          placeholder={t('credentials.filters.credentialType')}
          allowClear
          onChange={filterProps.setCredentialType}
        >
          {CREDENTIALS_TYPES_ENABLED.map(aCredentialType => (
            <Select.Option key={aCredentialType.id} value={aCredentialType.id}>
              {t(aCredentialType.name)}
            </Select.Option>
          ))}
        </Select>
      </div>
    </>
  );

  const renderIssuedFilters = () => (
    <>
      <div>
        <Select
          id="credentialStatusFilter"
          value={filterProps.credentialStatus}
          placeholder={t('credentials.filters.credentialStatus')}
          allowClear
          onChange={filterProps.setCredentialStatus}
        >
          {credentialStatuses.map(aCredentialStatus => (
            <Select.Option
              key={CREDENTIAL_STATUSES[aCredentialStatus]}
              value={CREDENTIAL_STATUSES[aCredentialStatus]}
            >
              {t(`credentials.filters.types.credentialStatus.${aCredentialStatus}`)}
            </Select.Option>
          ))}
        </Select>
      </div>
      <div>
        <Select
          id="contactStateFilter"
          value={filterProps.contactStatus}
          placeholder={t('credentials.filters.contactStatus')}
          allowClear
          onChange={filterProps.setContactStatus}
        >
          {NORMALIZED_CONNECTION_STATUSES.map(aContactState => (
            <Select.Option key={aContactState} value={aContactState}>
              {t(`credentials.filters.types.contactStatus.${aContactState}`)}
            </Select.Option>
          ))}
        </Select>
      </div>
      <div>
        <CustomInputGroup prefixIcon="calendar">
          <CustomDatePicker {...datePickerProps} />
        </CustomInputGroup>
      </div>
    </>
  );

  const renderReceivedFilters = () => (
    <div>
      <CustomInputGroup prefixIcon="calendar">
        <CustomDatePicker {...datePickerReceivedProps} />
      </CustomInputGroup>
    </div>
  );

  return (
    <div className="FilterControls">
      <div className="w-100">
        {renderBaseFilters()}
        {isIssued ? renderIssuedFilters() : renderReceivedFilters()}
      </div>
    </div>
  );
};

CredentialsFilter.defaultProps = {
  isIssued: false,
  credentialsTypes: []
};

CredentialsFilter.propTypes = {
  credentialsTypes: PropTypes.arrayOf(PropTypes.string),
  isIssued: PropTypes.bool,
  filterProps: PropTypes.shape({
    name: PropTypes.string,
    setName: PropTypes.func,
    credentialType: PropTypes.string,
    setCredentialType: PropTypes.func,
    credentialStatus: PropTypes.number,
    setCredentialStatus: PropTypes.func,
    contactStatus: PropTypes.string,
    setContactStatus: PropTypes.func,
    date: PropTypes.string,
    setDate: PropTypes.func
  }).isRequired
};

export default CredentialsFilter;
