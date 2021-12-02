import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { Form, Radio } from 'antd';
import { templateLayouts } from '../../../../helpers/templateLayouts/templates';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import './_style.scss';

const LayoutSelector = observer(() => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateCreationStore();

  return (
    <Form.Item
      className="layoutContainer"
      name="layout"
      label={t('credentialTemplateCreation.step2.style.layout')}
      rules={[
        {
          required: true
        }
      ]}
    >
      <Radio.Group>
        {templateLayouts.map((l, idx) => {
          const layoutKey = `LayoutTemplate_${idx}`;
          return (
            <Radio key={layoutKey} value={idx}>
              <div
                className={`LayoutOption shadow ${templateSketch.layout === idx ? 'selected' : ''}`}
              >
                <img className="layout-thumb" src={l.thumb} alt={layoutKey} />
              </div>
            </Radio>
          );
        })}
      </Radio.Group>
    </Form.Item>
  );
});

export default LayoutSelector;
