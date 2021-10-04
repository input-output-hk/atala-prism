import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { DownOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import CustomDatePicker from '../../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import CustomInputGroup from '../../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import {
  NORMALIZED_CONNECTION_STATUSES,
  CREDENTIAL_STATUSES
} from '../../../../../helpers/constants';
import { credentialTypeShape } from '../../../../../helpers/propShapes';

const credentialStatuses = Object.keys(CREDENTIAL_STATUSES);

const CredentialsFilter = ({ filterProps, isIssued }) => {
  const { t } = useTranslation();

  const CREDENTIALS_TYPES_ENABLED = Object.values(filterProps?.credentialTypes);
  const renderContactStatusFilter = () => (
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
  );

  const renderCredentialStatusFilter = () => (
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
  );

  const renderCredentialTypeFilter = () => (
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
  );

  const renderCredentialDateFilter = () => {
    const datePickerProps = {
      placeholder: t(`credentials.filters.${isIssued ? 'dateSigned' : 'dateReceived'}`),
      suffixIcon: <DownOutlined />,
      onChange: (_, dateString) => filterProps.setDate(dateString)
    };

    return (
      <div>
        <CustomInputGroup prefixIcon="calendar">
          <CustomDatePicker {...datePickerProps} />
        </CustomInputGroup>
      </div>
    );
  };

  const renderIssuedFilters = () => (
    <>
      {renderContactStatusFilter()}
      {renderCredentialStatusFilter()}
      {renderCredentialTypeFilter()}
      {renderCredentialDateFilter()}
    </>
  );

  const renderReceivedFilters = () => (
    <>
      {renderCredentialTypeFilter()}
      {renderCredentialDateFilter()}
    </>
  );

  return (
    <div className="FiltersMenu">{isIssued ? renderIssuedFilters() : renderReceivedFilters()}</div>
  );
};

CredentialsFilter.defaultProps = {
  isIssued: false
};

CredentialsFilter.propTypes = {
  isIssued: PropTypes.bool,
  filterProps: PropTypes.shape({
    name: PropTypes.string,
    setName: PropTypes.func,
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
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
