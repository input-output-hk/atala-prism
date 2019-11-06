import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { noEmptyInput } from '../../../../helpers/formRules';
import { refShape } from '../../../../helpers/propShapes';

const OrganisationInfo = ({ organisationRef }) => {
  const { t } = useTranslation();

  const orgName = [
    {
      fieldDecoratorData: {
        rules: [noEmptyInput(t('errors.form.emptyField'))]
      },
      label: t('registration.organisationInfo.name'),
      key: 'organisationInfo',
      className: '',
      input: <Input />
    }
  ];

  return (
    <Fragment>
      <StepCard
        title="registration.organisationInfo.title"
        subtitle="registration.organisationInfo.subtitle"
        comment="registration.organisationInfo.comment"
      />
      <CustomForm items={orgName} ref={organisationRef} />
    </Fragment>
  );
};

OrganisationInfo.propTypes = {
  organisationRef: refShape.isRequired
};

export default OrganisationInfo;
