import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Form, Input } from 'antd';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { withApi } from '../../../providers/withApi';
import { useCredentialTypes } from '../../../../hooks/useCredentialTypes';
import { credentialTypesManagerShape } from '../../../../helpers/propShapes';

import './_style.scss';

const normalize = input => input.trim();

const TemplateName = ({ api }) => {
  const { t } = useTranslation();
  const { credentialTypes } = useCredentialTypes(api.credentialTypesManager);

  const templateExists = async (_rule, value) => {
    const normalizedValue = normalize(value);

    if (exactValueExists(credentialTypes, normalizedValue, 'name')) {
      const errorMessage = t('credentialTemplateCreation.errors.preExisting', {
        value: normalizedValue
      });
      throw new Error(errorMessage);
    }
  };

  return (
    <div className="templateName">
      <Form.Item
        hasFeedback
        className="flex"
        name="name"
        label={t('credentialTemplateCreation.step1.templateName')}
        rules={[{ required: true }, { validator: templateExists }]}
      >
        <p className="greyText">Create a name for your credential’s template.</p>
        <Input placeholder={t('credentialTemplateCreation.step1.templateNamePlaceholder')} />
      </Form.Item>
    </div>
  );
};

TemplateName.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape
  }).isRequired
};

export default withApi(TemplateName);
