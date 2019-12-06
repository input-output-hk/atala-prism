import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input, Select } from 'antd';
import PropTypes from 'prop-types';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { isValidRole, noEmptyInput } from '../../../../helpers/formRules';
import { refShape } from '../../../../helpers/propShapes';

import './_style.scss';
import { ISSUER, VERIFIER } from '../../../../helpers/constants';

const OrganizationInfo = ({
  organizationRef,
  organizationInfo: { organizationName = '', organizationRole = '' }
}) => {
  const { t } = useTranslation();

  const items = [
    {
      fieldDecoratorData: {
        rules: [noEmptyInput(t('errors.form.emptyField'))],
        initialValue: organizationName
      },
      label: t('registration.organizationInfo.name'),
      key: 'organizationName',
      className: '',
      input: <Input />
    },
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_rule, value, cb) => isValidRole(value, cb),
            message: 'registration.organizationInfo.invalidRole'
          }
        ],
        initialValue: organizationRole
      },
      label: t('registration.organizationInfo.role'),
      key: 'organizationRole',
      className: '',
      input: (
        <Select value={organizationRole}>
          <Select.Option value="">{t('registration.organizationInfo.defaultRole')}</Select.Option>
          <Select.Option value={ISSUER}>
            {t(`registration.organizationInfo.${ISSUER}`)}
          </Select.Option>
          <Select.Option value={VERIFIER}>
            {t(`registration.organizationInfo.${VERIFIER}`)}
          </Select.Option>
        </Select>
      )
    }
  ];

  return (
    <div className="RegisterStep">
      <StepCard
        title="registration.organizationInfo.title"
        subtitle="registration.organizationInfo.subtitle"
        comment="registration.organizationInfo.comment"
      />
      <CustomForm items={items} ref={organizationRef} />
    </div>
  );
};

OrganizationInfo.defaultProps = {
  organizationInfo: {
    organizationName: '',
    organizationRole: ''
  }
};

OrganizationInfo.propTypes = {
  organizationRef: refShape.isRequired,
  organizationInfo: PropTypes.shape({
    organizationName: PropTypes.string,
    organizationRole: PropTypes.string
  })
};

export default OrganizationInfo;
