import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Input } from 'antd';
import { observer } from 'mobx-react-lite';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { useTemplateStore } from '../../../../hooks/useTemplateStore';

import './_style.scss';

const normalize = input => input.trim();

const TemplateName = observer(() => {
  const { t } = useTranslation();
  const { credentialTemplates } = useTemplateStore();

  const templateExists = async (_rule, value) => {
    const normalizedValue = normalize(value);

    if (exactValueExists(credentialTemplates, normalizedValue, 'name')) {
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
        <Input placeholder={t('credentialTemplateCreation.step1.templateNamePlaceholder')} />
      </Form.Item>
    </div>
  );
});

export default TemplateName;
