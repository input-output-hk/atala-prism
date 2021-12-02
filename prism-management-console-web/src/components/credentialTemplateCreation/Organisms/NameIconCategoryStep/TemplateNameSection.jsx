import React from 'react';
import { useTranslation } from 'react-i18next';
import { Form, Input } from 'antd';
import { observer } from 'mobx-react-lite';
import { exactValueExists } from '../../../../helpers/filterHelpers';
import { useTemplatesPageStore } from '../../../../hooks/useTemplatesPageStore';

import './_style.scss';

const normalize = input => input.trim();

const i18nPrefix = 'credentialTemplateCreation';

const TemplateNameSection = observer(() => {
  const { t } = useTranslation();
  const { credentialTemplates } = useTemplatesPageStore();

  const templateExists = async (_rule, value) => {
    const normalizedValue = normalize(value);

    if (exactValueExists(credentialTemplates, normalizedValue, 'name')) {
      const errorMessage = t(`${i18nPrefix}.errors.preExisting`, {
        value: normalizedValue
      });
      throw new Error(errorMessage);
    }
  };

  return (
    <div className="TemplateName">
      <p className="TitleSmall">{t(`${i18nPrefix}.templateName.title`)}</p>
      <p className="SubtitleGray">{t(`${i18nPrefix}.templateName.info`)}</p>
      <Form.Item
        hasFeedback
        className="flex"
        name="name"
        label={t(`${i18nPrefix}.templateName.label`)}
        rules={[{ required: true }, { validator: templateExists }]}
      >
        <Input placeholder={t(`${i18nPrefix}.templateName.placeholder`)} />
      </Form.Item>
    </div>
  );
});

export default TemplateNameSection;
