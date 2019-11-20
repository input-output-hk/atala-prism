import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { noEmptyInput } from '../../../../helpers/formRules';
import { refShape } from '../../../../helpers/propShapes';

import './_style.scss';

const OrganizationInfo = ({ organizationRef }) => {
  const { t } = useTranslation();

  const orgName = [
    {
      fieldDecoratorData: {
        rules: [noEmptyInput(t('errors.form.emptyField'))]
      },
      label: t('registration.organizationInfo.name'),
      key: 'organizationInfo',
      className: '',
      input: <Input />
    }
  ];

  return (
    <div className="RegisterStep">
      <StepCard
        title="registration.organizationInfo.title"
        subtitle="registration.organizationInfo.subtitle"
        comment="registration.organizationInfo.comment"
      />
      <CustomForm items={orgName} ref={organizationRef} />
    </div>
  );
};

OrganizationInfo.propTypes = {
  organizationRef: refShape.isRequired
};

export default OrganizationInfo;
