import React from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import PropTypes from 'prop-types';
import { DownOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import CustomDatePicker from '../../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import CustomInputGroup from '../../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import {
  NORMALIZED_CONNECTION_STATUSES,
  CREDENTIAL_STATUSES
} from '../../../../../helpers/constants';
import { useCredentialsIssuedPageStore } from '../../../../../hooks/useCredentialsIssuedPageStore';
import { useTemplateStore } from '../../../../../hooks/useTemplateStore';

const credentialStatuses = Object.keys(CREDENTIAL_STATUSES);

const CredentialsFilter = observer(({ isIssued }) => {
  const { t } = useTranslation();

  const {
    filterSortingProps: {
      credentialTypeFilter,
      credentialStatusFilter,
      connectionStatusFilter,
      setFilterValue
    }
  } = useCredentialsIssuedPageStore();
  const { credentialTemplates: credentialTypes } = useTemplateStore();

  const setFilterByKey = key => value => setFilterValue(key, value);

  const renderContactStatusFilter = () => (
    <div>
      <Select
        id="connectionStatusFilter"
        value={connectionStatusFilter}
        placeholder={t('credentials.filters.connectionStatusPlaceholder')}
        allowClear
        onChange={setFilterByKey('connectionStatusFilter')}
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
        value={credentialStatusFilter}
        placeholder={t('credentials.filters.credentialStatusPlaceholder')}
        allowClear
        onChange={setFilterByKey('credentialStatusFilter')}
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
        value={credentialTypeFilter}
        placeholder={t('credentials.filters.credentialTypePlaceholder')}
        allowClear
        onChange={setFilterByKey('credentialTypeFilter')}
      >
        {credentialTypes.map(aCredentialType => (
          <Select.Option key={aCredentialType.id} value={aCredentialType.id}>
            {t(aCredentialType.name)}
          </Select.Option>
        ))}
      </Select>
    </div>
  );

  const renderCredentialDateFilter = () => {
    const datePickerProps = {
      placeholder: t(
        `credentials.filters.${isIssued ? 'dateSignedPlaceholder' : 'dateReceivedPlaceholder'}`
      ),
      suffixIcon: <DownOutlined />,
      onChange: (_, dateString) => setFilterValue('dateFilter', dateString)
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
});

CredentialsFilter.defaultProps = {
  isIssued: false
};

CredentialsFilter.propTypes = {
  isIssued: PropTypes.bool
};

export default CredentialsFilter;
