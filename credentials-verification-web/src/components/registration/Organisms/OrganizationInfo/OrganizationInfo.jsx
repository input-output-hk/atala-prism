import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input, Select } from 'antd';
import PropTypes from 'prop-types';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { isValidRole, noEmptyInput, minOneElement } from '../../../../helpers/formRules';
import { refShape } from '../../../../helpers/propShapes';
import { ISSUER, VERIFIER } from '../../../../helpers/constants';
import FileUploader from '../../../common/Molecules/FileUploader/FileUploader';

import './_style.scss';

const OrganizationInfo = ({
  organizationRef,
  organizationInfo: { organizationName = '', organizationRole = '', logo },
  savePicture
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
      className: 'organizationRole',
      input: <Input />
    },
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_rule, value, cb) => isValidRole(value, cb),
            message: t('registration.organizationInfo.invalidRole')
          }
        ],
        initialValue: organizationRole
      },
      label: t('registration.organizationInfo.role'),
      key: 'organizationRole',
      className: 'organizationRole',
      input: (
        <Select>
          <Select.Option value="">{t('registration.organizationInfo.defaultRole')}</Select.Option>
          <Select.Option value={ISSUER}>
            {t(`registration.organizationInfo.${ISSUER}`)}
          </Select.Option>
          <Select.Option value={VERIFIER}>
            {t(`registration.organizationInfo.${VERIFIER}`)}
          </Select.Option>
        </Select>
      )
    },
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_, value, cb) => minOneElement(value, cb),
            message: t('errors.form.emptyField')
          }
        ],
        initialValue: logo ? [logo] : []
      },
      label: t('newCredential.form.logo'),
      key: 'logo',
      className: 'organizationRole',
      input: (
        <FileUploader
          initialValue={logo}
          hint="newCredential.form.logoHint"
          field="logo"
          savePicture={savePicture}
          uploadText="newCredential.form.uploadButton"
          formRef={organizationRef}
        />
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
      <div className="OrganizationInfo">
        <CustomForm items={items} ref={organizationRef} />
      </div>
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
    organizationRole: PropTypes.string,
    logo: PropTypes.any
  }),
  savePicture: PropTypes.func.isRequired
};

export default OrganizationInfo;
